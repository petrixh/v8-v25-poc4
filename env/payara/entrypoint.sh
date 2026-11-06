#!/bin/bash
# Container start: install server libs, properties and login pages from /deploy (read-only mount, env/build.sh output),
# start the domain, deploy the ear then the wars, then follow the server log (the container's log).
set -euo pipefail
: "${SORMAS_DB_PW:?}" "${SORMAS_DB_HOST:?}"
D=/opt/payara5/glassfish/domains/sormas
cp /deploy/serverlibs/*.jar $D/lib/
cp /deploy/sormas.properties $D/sormas.properties
cp /deploy/setup/logback.xml $D/config/logback.xml
cp /deploy/setup/loginsidebar.html /deploy/setup/loginsidebar-header.html /deploy/setup/logindetails.html /deploy/setup/loginmain.html /opt/sormas/custom/
asadmin start-domain sormas
for a in sormas-ear.ear sormas-rest.war sormas-ui.war; do
  echo "=== deploy $a"; asadmin --port 4848 deploy --force=true /deploy/apps/$a || echo "=== DEPLOY FAILED $a"
done
echo "=== DEPLOY DONE"
exec tail -n +1 -F $D/logs/server.log
