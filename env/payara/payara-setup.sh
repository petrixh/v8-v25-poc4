#!/bin/bash
# Image build time: domain "sormas" set up as sormas-base/setup/server-setup.sh does (port base 4800, so HTTP is 4880),
# on Payara 7. No secrets: the DB host and password are read from the env (SORMAS_DB_HOST, SORMAS_DB_PW) at run time.
# Differences to the Payara 5 setup are listed in env/README.md ("Payara 7 domain").
set -euo pipefail
A="asadmin --port 4848"
asadmin create-domain --portbase 4800 --nopassword sormas
asadmin start-domain sormas
$A delete-jvm-options '-client' || true
$A delete-jvm-options -Xmx512m || true
$A create-jvm-options '-Xmx4096m'
$A create-jvm-options '-XX\:+IgnoreUnrecognizedVMOptions'
$A create-system-properties --target server-config fish.payara.classloading.delegate=false
# As server-setup.sh: the "get" first. On Payara 7 the "set" alone fails with "No configuration found for
# configs.config.server-config.ejb-container". The Payara 5 env of the old app had no "get" and its pool stayed at 32.
$A get configs.config.server-config.ejb-container.max-pool-size
$A set configs.config.server-config.ejb-container.max-pool-size=128
$A set configs.config.server-config.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size=50
$A create-jdbc-connection-pool --restype javax.sql.ConnectionPoolDataSource --datasourceclassname org.postgresql.ds.PGConnectionPoolDataSource --isconnectvalidatereq true --validationmethod custom-validation --validationclassname org.glassfish.api.jdbc.validation.PostgresConnectionValidation --maxpoolsize 128 --property 'portNumber=5432:databaseName=sormas_db:serverName=${ENV=SORMAS_DB_HOST}:user=sormas_user:password=${ENV=SORMAS_DB_PW}' sormasDataPool
$A create-jdbc-resource --connectionpoolid sormasDataPool jdbc/sormasDataPool
# Mail: the copy's SMTP sink (compose service "mail", port 1025), never an outside mail host
$A create-javamail-resource --mailhost mail --mailuser sormas --fromaddress noreply@sormas.local --property 'mail.smtp.port=1025' mail/MailSession
$A create-custom-resource --restype java.util.Properties --factoryclass org.glassfish.resources.custom.factory.PropertiesFactory --property 'org.glassfish.resources.custom.factory.PropertiesFactory.fileName=${com.sun.aas.instanceRoot}/sormas.properties' sormas/Properties
$A create-jvm-options '-Dlogback.configurationFile=${com.sun.aas.instanceRoot}/config/logback.xml'
$A create-jvm-options '-DlogbackDisableServletContainerInitializer=true'
asadmin stop-domain sormas
mkdir -p /opt/sormas/custom /opt/sormas/documents /opt/sormas/temp /opt/sormas/generated
