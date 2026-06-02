FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace
COPY pom.xml .
COPY services/iam-service/pom.xml services/iam-service/pom.xml
COPY services/purchase-request-service/pom.xml services/purchase-request-service/pom.xml
COPY services/approval-service/pom.xml services/approval-service/pom.xml
COPY services/finance-service/pom.xml services/finance-service/pom.xml
COPY services/vendor-service/pom.xml services/vendor-service/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
COPY infra/keycloak/eprocure-keycloak-provider/pom.xml infra/keycloak/eprocure-keycloak-provider/pom.xml
RUN mvn -pl services/notification-service -am dependency:go-offline

COPY services/notification-service services/notification-service
RUN mvn -pl services/notification-service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S app && adduser -S app -G app && mkdir -p /app/logs /app/agents && chown -R app:app /app
WORKDIR /app

ENV TZ=Asia/Ho_Chi_Minh
ENV JAVA_OPTS="-Xms128m -Xmx192m -javaagent:/app/agents/opentelemetry-javaagent.jar"

COPY infra/otel/opentelemetry-javaagent.jar /app/agents/opentelemetry-javaagent.jar
COPY --from=builder /workspace/services/notification-service/target/notification-service-*.jar /app/app.jar
RUN chown -R app:app /app

EXPOSE 8088
HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=5 \
  CMD wget -q --spider http://127.0.0.1:8088/actuator/health || exit 1

USER app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
