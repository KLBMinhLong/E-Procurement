# Spring Boot Best Practices

## Principles
- Keep domain free of Spring dependencies.
- Prefer constructor injection with final fields.
- Use records for immutable DTO, Command, Result types.
- Centralize configuration in @ConfigurationProperties.

## Configuration
- Use application.yml for defaults and per-profile overrides.
- Validate config with @Validated and JSR-303 annotations.
- Avoid putting secrets in repo; use env vars or secret store.

```java
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    @NotBlank String timezone,
    @NotBlank String apiKey
) {}
```

## Application Layer
- Use UseCase classes as the application entrypoint.
- Apply @Transactional at UseCase, not controller.
- Keep controllers thin: request mapping, validation, response mapping.

## Exceptions
- Use domain-specific exceptions with error codes.
- Keep a single exception handler that maps codes to HTTP status.

## Logging
- Use Log4j2 (LogManager + Logger) with structured, consistent messages.
- Mask sensitive data before logging.
- Use MDC keys for requestId, traceId, userId, layer.

## Validation
- Validate request DTOs with @Valid in controllers.
- Use domain methods for business rule validation.

## Data Access
- Use MyBatis mappers in infrastructure.
- Use ObjectMapper.convertValue for mapping when possible.

## Testing
- Unit tests for domain and services with JUnit 5 + Mockito.
- Slice tests for controllers when needed.
- Avoid heavy context loading for unit tests.
