# MyBatis Strategy

## Decision
- Use MyBatis (not JPA/Hibernate).
- Annotation mapper for simple CRUD.
- XML mapper for complex SQL (joins, dynamic filters).

## Layering
- Repository interface in domain layer.
- Repository implementation in infrastructure layer.
- MyBatis mapper used only in infrastructure.

## Mapping
- Use ObjectMapper.convertValue for domain <-> entity.
- Keep domain models free of framework annotations.
