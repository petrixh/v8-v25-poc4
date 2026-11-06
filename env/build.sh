#!/bin/bash
# Build the runtime image and the app artefacts, shared by all copies:
#   1. build the Payara image $APP_ENV_IMAGE (eclipse-temurin:21-jdk + the Payara 7 zip from Maven Central, sha1 checked;
#      arm64 and amd64). It does not depend on the app, so it is built first and a red app build cannot fail it.
#   2. on a clone of this checkout's committed HEAD (uncommitted changes are not built): collect the setup files and the
#      server libs (sormas-serverlibs) into $APP_ENV_STATE_DIR/build/deploy
#   3. unless APP_ENV_DEPLOY=0: mvn clean install -DskipTests, Maven 3.9 on JDK 21 in a container (default profiles, no
#      Android app), and collect the ear and the wars. With APP_ENV_DEPLOY=0 the app is not built and not collected.
# Usually run by up.sh. Holds a lock so two copies never build at once.
set -euo pipefail
source "$(dirname "$0")/lib.sh"

mkdir -p "$BUILD_DIR" "$APP_ENV_M2_REPO"
exec 9>"$BUILD_DIR/.lock"
flock 9

mkdir -p "$BUILD_DIR/home" "$BUILD_DIR/dl" "$BUILD_DIR/ctx"
LOG=$BUILD_DIR/build.log
: > "$LOG"

t0=$SECONDS
log "build: Payara $PAYARA_VERSION image $APP_ENV_IMAGE"
ZIP=$BUILD_DIR/dl/payara-$PAYARA_VERSION.zip
if [ ! -s "$ZIP" ]; then curl -sSfL -o "$ZIP.part" "$PAYARA_ZIP_URL" && mv "$ZIP.part" "$ZIP"; fi
[ "$(sha1sum < "$ZIP" | cut -c1-40)" = "$(curl -sSfL "$PAYARA_ZIP_URL.sha1" | cut -c1-40)" ] || die "Payara zip sha1 mismatch: $ZIP"
cp -f "$ENV_DIR"/payara/{Dockerfile,payara-setup.sh,entrypoint.sh} "$BUILD_DIR/ctx/"
ln -f "$ZIP" "$BUILD_DIR/ctx/payara.zip" 2>/dev/null || cp -f "$ZIP" "$BUILD_DIR/ctx/payara.zip"
docker build -q -t "$APP_ENV_IMAGE" "$BUILD_DIR/ctx" >> "$LOG" 2>&1 || die "image build failed (log: $LOG)"

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

log "build: collect setup files and server libs into $APP_ENV_DEPLOY_DIR"
D=$APP_ENV_DEPLOY_DIR
rm -rf "$D"; mkdir -p "$D/setup"
cp "$SRC"/sormas-base/setup/{sormas.properties,logback.xml,loginsidebar.html,loginsidebar-header.html,logindetails.html,loginmain.html} "$D/setup/"
# Stock properties plus the env overlay (default entities, no default-password dialog, mail sender)
cat "$SRC/sormas-base/setup/sormas.properties" "$ENV_DIR/sormas.properties.env" > "$D/sormas.properties"
# Server libs (the domain's lib directory, entrypoint.sh): the compile-scope dependencies of sormas-serverlibs, as
# sormas-base/dev/collect-serverlibs.sh does. The list is whatever sormas-serverlibs/pom.xml at HEAD says: migration
# ticket 1a-02 replaces it with the Jakarta EE 11 list, 1a-91 checks it against the deployed app.
mvn_run serverlibs -f ../sormas-serverlibs/pom.xml -DincludeScope=compile \
  -DoutputDirectory="$SRC/sormas-serverlibs/target/collected-serverlibs" dependency:copy-dependencies >> "$LOG" 2>&1 ||
  die "server libs failed (log: $LOG)"
mv "$SRC/sormas-serverlibs/target/collected-serverlibs" "$D/serverlibs"
git -C "$SRC" rev-parse HEAD > "$D/commit" 2>/dev/null || echo unknown > "$D/commit"

if [ "$APP_ENV_DEPLOY" = 0 ]; then
  log "build: APP_ENV_DEPLOY=0, the app is not built"
else
  log "build: mvn clean install -DskipTests (JDK 21 container; log: $LOG)"
  mvn_run build clean install -DskipTests >> "$LOG" 2>&1 || die "maven build failed (log: $LOG)"
  for f in sormas-ear/target/sormas-ear.ear sormas-ui/target/sormas-ui.war sormas-rest/target/sormas-rest.war; do
    [ -s "$SRC/$f" ] || die "missing $f after the build"
  done
  mkdir -p "$D/apps"
  cp "$SRC"/sormas-ear/target/sormas-ear.ear "$SRC"/sormas-ui/target/sormas-ui.war "$SRC"/sormas-rest/target/sormas-rest.war "$D/apps/"
fi
log "build: done in $((SECONDS - t0)) s"
