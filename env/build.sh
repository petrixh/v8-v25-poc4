#!/bin/bash
# Build the app and the runtime image, shared by all copies:
#   1. mvn clean install -DskipTests on a clone of this checkout's committed HEAD (uncommitted changes are not built),
#      Maven 3.9 on JDK 21 in a container (default profiles, no Android app)
#   2. collect the ear, the wars, the server libs and the setup files into $APP_ENV_STATE_DIR/build/deploy
#   3. build the Payara image $APP_ENV_IMAGE (eclipse-temurin:11-jdk + the Payara zip from Maven Central, arm64 and amd64)
# Until ticket 1a-22 the image is still Payara 5 on JDK 11, which cannot load the Java 21 classes built in step 1: the
# build succeeds, the app does not come up (env/README.md, "Java 21 build, Payara 5 runtime").
# Usually run by up.sh. Holds a lock so two copies never build at once.
set -euo pipefail
source "$(dirname "$0")/lib.sh"

mkdir -p "$BUILD_DIR" "$APP_ENV_M2_REPO"
exec 9>"$BUILD_DIR/.lock"
flock 9

mkdir -p "$BUILD_DIR/home" "$BUILD_DIR/dl" "$BUILD_DIR/ctx"
LOG=$BUILD_DIR/build.log
: > "$LOG"

# Build a clean clone of HEAD in the state dir: a known commit, no build output in the checkout, and it works from a
# git worktree too (the git-commit-id plugin, which writes git.properties for the About view, cannot read worktrees).
SRC=$BUILD_DIR/src
git -C "$APP_ROOT" diff --quiet HEAD 2>/dev/null || log "WARNING: uncommitted changes in $APP_ROOT are not built (HEAD is)"
[ -d "$SRC/.git" ] || git clone -q --no-checkout "$APP_ROOT" "$SRC"
git -C "$SRC" fetch -q "$APP_ROOT" HEAD && git -C "$SRC" checkout -q --detach FETCH_HEAD && git -C "$SRC" clean -qfdx
mounts=(-v "$SRC:$SRC")

mvn_run() { # mvn_run <name> <mvn args...>
  local name=$APP_ENV_PROJECT-mvn-$1; shift
  docker run --rm --name "$name" --label sormas-env.project="$APP_ENV_PROJECT" --user "$(id -u):$(id -g)" \
    "${mounts[@]}" -v "$APP_ENV_M2_REPO:/m2" -v "$BUILD_DIR/home:/home/build" \
    -e HOME=/home/build -e MAVEN_CONFIG=/home/build/.m2 -e MAVEN_OPTS="-Xmx3g -Duser.home=/home/build" \
    -w "$SRC/sormas-base" "$MVN_IMAGE" mvn -B -Dmaven.repo.local=/m2 "$@"
}

log "build: mvn clean install -DskipTests (JDK 21 container; log: $LOG)"
t0=$SECONDS
mvn_run build clean install -DskipTests >> "$LOG" 2>&1 || die "maven build failed (log: $LOG)"
for f in sormas-ear/target/sormas-ear.ear sormas-ui/target/sormas-ui.war sormas-rest/target/sormas-rest.war; do
  [ -s "$SRC/$f" ] || die "missing $f after the build"
done

log "build: collect artefacts into $APP_ENV_DEPLOY_DIR"
D=$APP_ENV_DEPLOY_DIR
rm -rf "$D"; mkdir -p "$D/apps" "$D/setup"
cp "$SRC"/sormas-ear/target/sormas-ear.ear "$SRC"/sormas-ui/target/sormas-ui.war "$SRC"/sormas-rest/target/sormas-rest.war "$D/apps/"
cp "$SRC"/sormas-base/setup/{sormas.properties,logback.xml,loginsidebar.html,loginsidebar-header.html,logindetails.html,loginmain.html} "$D/setup/"
# Stock properties plus the env overlay (default entities, no default-password dialog, mail sender)
cat "$SRC/sormas-base/setup/sormas.properties" "$ENV_DIR/sormas.properties.env" > "$D/sormas.properties"
# Server libs: the compile-scope dependencies of sormas-serverlibs, as sormas-base/dev/collect-serverlibs.sh does
mvn_run serverlibs -f ../sormas-serverlibs/pom.xml -DincludeScope=compile \
  -DoutputDirectory="$SRC/sormas-serverlibs/target/collected-serverlibs" dependency:copy-dependencies >> "$LOG" 2>&1 ||
  die "server libs failed (log: $LOG)"
rm -rf "$D/serverlibs"; mv "$SRC/sormas-serverlibs/target/collected-serverlibs" "$D/serverlibs"
git -C "$SRC" rev-parse HEAD > "$D/commit" 2>/dev/null || echo unknown > "$D/commit"

log "build: Payara $PAYARA_VERSION image $APP_ENV_IMAGE"
ZIP=$BUILD_DIR/dl/payara-$PAYARA_VERSION.zip
if [ ! -s "$ZIP" ]; then curl -sSfL -o "$ZIP.part" "$PAYARA_ZIP_URL" && mv "$ZIP.part" "$ZIP"; fi
[ "$(sha1sum < "$ZIP" | cut -c1-40)" = "$(curl -sSfL "$PAYARA_ZIP_URL.sha1" | cut -c1-40)" ] || die "Payara zip sha1 mismatch: $ZIP"
cp -f "$ENV_DIR"/payara/{Dockerfile,payara-setup.sh,entrypoint.sh} "$BUILD_DIR/ctx/"
ln -f "$ZIP" "$BUILD_DIR/ctx/payara.zip" 2>/dev/null || cp -f "$ZIP" "$BUILD_DIR/ctx/payara.zip"
docker build -q -t "$APP_ENV_IMAGE" "$BUILD_DIR/ctx" >> "$LOG" 2>&1 || die "image build failed (log: $LOG)"
log "build: done in $((SECONDS - t0)) s"
