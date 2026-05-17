# Observability Stack

## Logging
- Use Log4j2, exclude Logback
- Console pattern for dev, JSON layout for prod
- Separate AUDIT logger to audit.log (append-only)
- MDC keys: requestId, traceId, spanId, userId, layer

## Tracing
- OpenTelemetry Java agent
- Set -javaagent at runtime
- Export traces to Tempo

## Metrics
- Micrometer -> Prometheus -> Grafana

## Log aggregation
- Loki -> Grafana

## Env vars
- OTEL_SERVICE_NAME
- OTEL_EXPORTER_OTLP_ENDPOINT
- OTEL_TRACES_EXPORTER
- OTEL_METRICS_EXPORTER
- OTEL_LOGS_EXPORTER
