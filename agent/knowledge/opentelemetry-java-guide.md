# OpenTelemetry Java Agent Guide

## JVM Agent
Add the agent at runtime:

```
-javaagent:/app/opentelemetry-javaagent.jar
```

## Key Environment Variables
- OTEL_SERVICE_NAME=auth-service
- OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
- OTEL_METRICS_EXPORTER=otlp
- OTEL_TRACES_EXPORTER=otlp
- OTEL_LOGS_EXPORTER=otlp

## MDC Correlation
- Ensure logging pattern includes traceId and spanId.
- Keep requestId in MDC at request entry and clear on exit.

## SDK Usage
- Use the Java agent; do not configure OpenTelemetry SDK in application code.

## Sensitive Data
- Do not export secrets, tokens, or passwords.
- Use attribute filters if needed to drop sensitive headers.

## Docker Example
```
ENV OTEL_SERVICE_NAME=auth-service \
    OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
```
