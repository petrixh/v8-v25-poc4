# Payara 5.2022.5 on JDK 11 for SORMAS (arm64 and amd64: the payara/server-full images are amd64 only).
# Build context (made by env/build.sh): payara.zip from Maven Central (sha1 checked) and the two scripts. No secrets in any layer.
FROM eclipse-temurin:11-jdk
COPY payara.zip /tmp/payara.zip
RUN cd /opt && jar xf /tmp/payara.zip && rm /tmp/payara.zip && chmod +x /opt/payara5/bin/* /opt/payara5/glassfish/bin/*
ENV PAYARA_HOME=/opt/payara5 PATH=/opt/payara5/bin:$PATH
COPY payara-setup.sh entrypoint.sh /opt/sormas-env/
RUN chmod +x /opt/sormas-env/*.sh && /opt/sormas-env/payara-setup.sh
EXPOSE 4880
ENTRYPOINT ["/opt/sormas-env/entrypoint.sh"]
