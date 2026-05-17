## SK-22 · Log4j2 Custom Layout

### Trigger
Agent cấu hình logging layout cho service.

### Inputs Required
- Service name
- Log directory
- Masking rules

### Rules
```
[R1] Bỏ Logback mặc định — exclude spring-boot-starter-logging
[R2] Log tiếng Anh toàn bộ
[R3] Ẩn sensitive data: password, token, private key, card number
[R4] Email mask: u***@***.com; Phone mask: 09*****678
[R5] Log có màu terminal (PatternLayout với ANSI colors)
[R6] Mỗi log line có: timestamp | level | traceId | spanId | service | thread | logger | message
[R7] Audit log ra file riêng: logs/audit.log (append-only rotation)
[R8] OpenTelemetry trace context tự động inject vào MDC
```

### log4j2.xml Template
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN" monitorInterval="30">

    <Properties>
        <Property name="SERVICE_NAME">${env:SERVICE_NAME:-unknown-service}</Property>
        <Property name="LOG_DIR">./logs</Property>
        <Property name="CONSOLE_PATTERN">
            %highlight{%d{yyyy-MM-dd HH:mm:ss.SSS}}{FATAL=red, ERROR=red, WARN=yellow, INFO=white, DEBUG=cyan}
            %highlight{[%-5level]}{FATAL=red blink, ERROR=red, WARN=yellow bold, INFO=green, DEBUG=cyan}
            [%X{traceId}/%X{spanId}]
            [${SERVICE_NAME}]
            [%t]
            %cyan{%-40.40c{1.}}
            %msg%n
        </Property>
        <Property name="FILE_PATTERN">
            %d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%X{traceId}/%X{spanId}] [${SERVICE_NAME}] [%t] %-40.40c{1.} %msg%n
        </Property>
    </Properties>

    <Appenders>
        <!-- Console with colors -->
        <Console name="CONSOLE" target="SYSTEM_OUT">
            <PatternLayout pattern="${CONSOLE_PATTERN}" disableAnsi="false"/>
        </Console>

        <!-- Application log file -->
        <RollingFile name="FILE"
                     fileName="${LOG_DIR}/application.log"
                     filePattern="${LOG_DIR}/application-%d{yyyy-MM-dd}-%i.log.gz">
            <PatternLayout pattern="${FILE_PATTERN}"/>
            <Policies>
                <TimeBasedTriggeringPolicy interval="1" modulate="true"/>
                <SizeBasedTriggeringPolicy size="50MB"/>
            </Policies>
            <DefaultRolloverStrategy max="30"/>
        </RollingFile>

        <!-- Audit log — append-only, separate file -->
        <RollingFile name="AUDIT"
                     fileName="${LOG_DIR}/audit.log"
                     filePattern="${LOG_DIR}/audit-%d{yyyy-MM-dd}.log.gz">
            <PatternLayout pattern="${FILE_PATTERN}"/>
            <Policies>
                <TimeBasedTriggeringPolicy interval="1" modulate="true"/>
            </Policies>
            <DefaultRolloverStrategy max="1825"/> <!-- 5 years -->
        </RollingFile>
    </Appenders>

    <Loggers>
        <!-- Application -->
        <Logger name="com.eprocure" level="INFO" additivity="false">
            <AppenderRef ref="CONSOLE"/>
            <AppenderRef ref="FILE"/>
        </Logger>

        <!-- Audit logger -->
        <Logger name="AUDIT" level="INFO" additivity="false">
            <AppenderRef ref="AUDIT"/>
        </Logger>

        <!-- SQL (dev only — controlled by profile) -->
        <Logger name="com.eprocure.*.infrastructure.persistence.mapper" level="${env:SQL_LOG_LEVEL:-WARN}" additivity="false">
            <AppenderRef ref="CONSOLE"/>
        </Logger>

        <Root level="WARN">
            <AppenderRef ref="CONSOLE"/>
            <AppenderRef ref="FILE"/>
        </Root>
    </Loggers>

</Configuration>
```

### Checklist
```
[ ] Logback bị exclude
[ ] Audit log ra file riêng
[ ] traceId/spanId có trong log
[ ] Sensitive data được mask
```
