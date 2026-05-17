# Mapping Notes (ObjectMapper)

## Project Rule
- MapStruct is not used in this project.
- Use ObjectMapper.convertValue for domain <-> persistence conversion.

## ObjectMapper Helper
```java
@Component
public class ObjectMapperHelper {

    private final ObjectMapper objectMapper;

    public ObjectMapperHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T to(Object source, Class<T> type) {
        return objectMapper.convertValue(source, type);
    }
}
```

## Notes
- Keep domain model free of framework annotations.
- Persistence entities mirror DB columns; mapping stays in infrastructure.
- Avoid manual field-by-field mapping unless business rules require it.
