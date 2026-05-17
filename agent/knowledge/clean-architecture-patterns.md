# Clean Architecture Patterns

## Dependency Rule
- Domain does not depend on any external framework.
- Application depends on domain.
- Infrastructure depends on application and domain.
- Presentation depends on application.

## Layers
1. Domain: entities, value objects, domain services, repository ports.
2. Application: use cases, service interfaces, command/result DTOs.
3. Infrastructure: MyBatis entities, mappers, repositories, adapters, cache.
4. Presentation: controllers, request/response DTOs, API wiring.

## Ports and Adapters
- Define repository interfaces in domain.
- Implement them in infrastructure adapters.
- Use mappers to translate between domain and persistence models.

## Package Sketch
- domain/
  - model/
  - repository/
  - exception/
- application/
  - service/
  - usecase/
  - dto/
- infrastructure/
  - persistence/
    - entity/
    - mapper/
    - repository/
  - cache/
- presentation/
  - controller/
  - dto/

## Mapping Guidance
- Domain entity should be free of framework annotations.
- MyBatis entity mirrors DB columns; keep conversion logic in mapper or ObjectMapper.
- Avoid business logic in persistence entities.
