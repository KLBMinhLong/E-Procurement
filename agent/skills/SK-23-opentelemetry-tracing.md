## SK-23 · OpenTelemetry Tracing

### Trigger
Agent cần thêm tracing cho service hoặc endpoint mới.

### Inputs Required
- OTLP exporter endpoint
- Service name
- Span naming convention

### Rules
```
[R1] Sử dụng W3C TraceContext (traceparent) cho propagation
[R2] Không ghi PII hoặc secrets vào span attributes
[R3] Tạo span custom cho nghiệp vụ quan trọng (Use Case/Integration)
[R4] Errors phải recordException và setStatus(ERROR)
[R5] traceId/spanId phải có trong MDC (log correlation)
[R6] Dùng Java Agent (không tự cấu hình SDK trong app)
```

### Template — Java Agent (no SDK config)
```
JAVA_TOOL_OPTIONS="-javaagent:/app/opentelemetry-javaagent.jar"
OTEL_SERVICE_NAME=pr-service
OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo:4317
```

### Template — Custom Span
```java
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

public class BudgetCheckService {

	private final Tracer tracer = GlobalOpenTelemetry.getTracer("com.eprocure.budget");

	public BudgetCheckResult check(...) {
		Span span = tracer.spanBuilder("BudgetCheck")
			.setSpanKind(SpanKind.INTERNAL)
			.startSpan();

		try (var scope = span.makeCurrent()) {
			span.setAttribute("department.id", departmentId.toString());
			span.setAttribute("amount", requestAmount.doubleValue());
			return doCheck(...);
		} catch (Exception ex) {
			span.recordException(ex);
			span.setStatus(StatusCode.ERROR, ex.getMessage());
			throw ex;
		} finally {
			span.end();
		}
	}
}
```

### Checklist
```
[ ] Custom span cho nghiệp vụ quan trọng
[ ] recordException + StatusCode.ERROR khi lỗi
[ ] Không log/attach PII hoặc secret
[ ] OTLP exporter cấu hình qua env
```
