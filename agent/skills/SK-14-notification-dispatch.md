## SK-14 · Notification Dispatch

### Trigger
Agent triển khai hoặc mở rộng luồng gửi thông báo (email/SMS/in-app/push) khi có sự kiện nghiệp vụ.

### Inputs Required
- Event/action name (VD: PR.SUBMITTED, PR.APPROVED)
- Recipient rules (userId/role/department)
- Channels (EMAIL, SMS, IN_APP, PUSH)
- Template key + params
- Priority + schedule (immediate/delayed)

### Rules
```
[R1] Use Case chỉ gọi NotificationPort (interface) — không gọi HTTP client trực tiếp
[R2] Dispatch bất đồng bộ (Kafka topic hoặc outbox) — không block luồng nghiệp vụ
[R3] Idempotent theo notificationId hoặc eventId
[R4] Retry tối đa 3 lần, sau đó đẩy DLQ
[R5] Locale bắt buộc để resolve template
[R6] Không chứa dữ liệu nhạy cảm trong payload hoặc log
[R7] Log tối thiểu: notificationId, channel, recipientId, templateKey, status
```

### Template — Port
```java
package com.eprocure.{service}.application.port.out;

/**
 * Port: NotificationPort
 * Dispatch notification message to external service.
 */
public interface NotificationPort {
	void dispatch(NotificationMessage message);
}
```

### Template — Message Model
```java
package com.eprocure.{service}.application.port.out;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationMessage(
	UUID notificationId,
	String eventType,
	UUID recipientId,
	String channel,        // EMAIL | SMS | IN_APP | PUSH
	String templateKey,
	Map<String, Object> params,
	String locale,
	String priority,       // LOW | NORMAL | HIGH
	Instant createdAt
) {
	public static NotificationMessage of(
			String eventType,
			UUID recipientId,
			String channel,
			String templateKey,
			Map<String, Object> params,
			String locale,
			String priority) {
		return new NotificationMessage(
			UUID.randomUUID(),
			eventType,
			recipientId,
			channel,
			templateKey,
			params,
			locale,
			priority,
			Instant.now()
		);
	}
}
```

### Template — Event Handler (After Commit)
```java
package com.eprocure.{service}.application.handler;

import com.eprocure.{service}.application.port.out.NotificationMessage;
import com.eprocure.{service}.application.port.out.NotificationPort;
import com.eprocure.{service}.domain.event.{DomainEvent};
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.Map;

@Component
public class {Entity}NotificationHandler {

	private final NotificationPort notificationPort;

	public {Entity}NotificationHandler(NotificationPort notificationPort) {
		this.notificationPort = notificationPort;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void on{DomainEvent}({DomainEvent} event) {
		NotificationMessage message = NotificationMessage.of(
			event.getEventType(),
			event.getRecipientId(),
			"IN_APP",
			"{feature}.notification.{event}",
			Map.of("entityId", event.getEntityId()),
			event.getLocale(),
			"NORMAL"
		);

		notificationPort.dispatch(message);
	}
}
```

### Checklist
```
[ ] Dispatch sau transaction commit
[ ] Idempotency theo notificationId/eventId
[ ] Không chứa dữ liệu nhạy cảm trong payload/log
[ ] Locale và templateKey đầy đủ
[ ] Retry + DLQ đã cấu hình
```
