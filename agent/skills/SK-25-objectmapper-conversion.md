## SK-25 · ObjectMapper Conversion

### Trigger
Agent cần convert giữa DTO, command, entity, hoặc response.

### Inputs Required
- Source object
- Target type (class)

### Rules
```
[R1] Dùng ObjectMapper.convertValue cho mapping cơ bản
[R2] Không manual mapping trừ khi có business rule
[R3] Bật JavaTimeModule và disable WRITE_DATES_AS_TIMESTAMPS
[R4] FAIL_ON_UNKNOWN_PROPERTIES = false để tránh lỗi khi thêm field
[R5] Không serialize BigDecimal thành scientific notation
```

### Template — ObjectMapper Config
```java
@Configuration
public class ObjectMapperConfig {

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
		return mapper;
	}
}
```

### Template — Conversion Helper
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

	public <T> List<T> toList(Object source, Class<T> type) {
		JavaType listType = objectMapper.getTypeFactory()
			.constructCollectionType(List.class, type);
		return objectMapper.convertValue(source, listType);
	}
}
```

### Checklist
```
[ ] ObjectMapper đã cấu hình JavaTimeModule
[ ] convertValue dùng cho mapping cơ bản
[ ] Mapping custom tách riêng khi có nghiệp vụ
```
