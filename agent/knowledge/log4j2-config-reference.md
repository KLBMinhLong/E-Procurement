# Log4j2 Config Reference

## Minimal log4j2.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
  <Properties>
    <Property name="LOG_DIR">logs</Property>
    <Property name="PATTERN">%d{ISO8601} %-5level [%X{traceId}] [%X{spanId}] [%X{requestId}] [%X{layer}] %logger - %msg%n</Property>
  </Properties>

  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <PatternLayout pattern="${PATTERN}"/>
    </Console>

    <RollingFile name="Audit" fileName="${LOG_DIR}/audit.log"
                 filePattern="${LOG_DIR}/audit-%d{yyyy-MM-dd}.log.gz">
      <PatternLayout pattern="%d{ISO8601} %msg%n"/>
      <Policies>
        <TimeBasedTriggeringPolicy interval="1"/>
      </Policies>
    </RollingFile>
  </Appenders>

  <Loggers>
    <Logger name="AUDIT" level="info" additivity="false">
      <AppenderRef ref="Audit"/>
    </Logger>
    <Root level="info">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

## Notes
- Exclude Logback from Spring Boot starter logging.
- Keep audit logs in a dedicated logger named AUDIT.
- Use MDC keys: requestId, traceId, userId, layer.
- Avoid logging sensitive values in any logger.
