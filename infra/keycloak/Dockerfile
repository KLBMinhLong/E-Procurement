FROM maven:3.9-eclipse-temurin-17 AS provider-build

WORKDIR /workspace
COPY pom.xml .
COPY infra/keycloak/eprocure-keycloak-provider/pom.xml infra/keycloak/eprocure-keycloak-provider/pom.xml
COPY infra/keycloak/eprocure-keycloak-provider/src infra/keycloak/eprocure-keycloak-provider/src
RUN mvn -q -f infra/keycloak/eprocure-keycloak-provider/pom.xml -DskipTests package

FROM quay.io/keycloak/keycloak:24.0

COPY --from=provider-build /workspace/infra/keycloak/eprocure-keycloak-provider/target/eprocure-keycloak-provider-*.jar /opt/keycloak/providers/eprocure-keycloak-provider.jar
RUN /opt/keycloak/bin/kc.sh build
