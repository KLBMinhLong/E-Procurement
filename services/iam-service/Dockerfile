FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace
COPY pom.xml .
COPY services/iam-service/pom.xml services/iam-service/pom.xml
RUN mvn -pl services/iam-service -am dependency:go-offline

COPY services/iam-service services/iam-service
RUN mvn -pl services/iam-service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

ENV TZ=Asia/Ho_Chi_Minh
ENV JAVA_OPTS="-Xms128m -Xmx384m"

COPY --from=builder /workspace/services/iam-service/target/iam-service-*.jar /app/app.jar

EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=75s --retries=5 \
  CMD wget -q --spider http://127.0.0.1:8081/actuator/health || exit 1

USER app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
