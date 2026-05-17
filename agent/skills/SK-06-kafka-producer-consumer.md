## SK-06 · Kafka Producer & Consumer

### Trigger
Agent tạo event-driven communication giữa các service.

### Inputs Required
- Topic name
- Event schema + payload fields
- Message key strategy
- Consumer group id

### Rules
```
[R1] Topic naming: {domain}.{entity}.{event} — lowercase, dot-separated
[R2] Message key = entity UUID (đảm bảo ordering trong partition)
[R3] Mọi event có: eventId (UUID), eventType, occurredAt (Instant), payload
[R4] Producer: gửi sau transaction commit — dùng @TransactionalEventListener
[R5] Consumer: idempotent — check eventId đã xử lý chưa (Redis/DB)
[R6] Consumer: DLQ (Dead Letter Queue) cho messages lỗi sau 3 lần retry
[R7] Không publish event trong domain method — chỉ trong Use Case/Event Handler
[R8] Mọi consumer log đầy đủ eventId, topic, partition, offset
```

### Template — Event Record
```java
package com.eprocure.{service}.infrastructure.kafka.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka Event: {EventName}
 * Topic: {topic.name}
 */
public record {EventName}(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    UUID {entityId},
    // ... payload fields
) {
    public static {EventName} of(UUID {entityId} /*, ...*/) {
        return new {EventName}(
            UUID.randomUUID(),
            "{EventName}",
            Instant.now(),
            {entityId}
        );
    }
}
```

### Checklist
```
[ ] Producer publish sau transaction commit
[ ] Consumer idempotent theo eventId
[ ] DLQ cho messages lỗi sau retry
[ ] Log đầy đủ topic/partition/offset
```

### Template — Producer
```java
package com.eprocure.{service}.infrastructure.kafka.producer;

import com.eprocure.{service}.infrastructure.kafka.event.{EventName};
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Kafka Producer: {Entity}EventPublisher
 * Publishes after transaction COMMIT — never during.
 */
@Component
public class {Entity}EventPublisher {

    private static final Logger log = LogManager.getLogger({Entity}EventPublisher.class);
    private static final String TOPIC = "{domain}.{entity}.{event}";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public {Entity}EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on{DomainEvent}({DomainEvent} domainEvent) {
        var kafkaEvent = {EventName}.of(domainEvent.getEntityId() /*, ...*/);

        kafkaTemplate.send(TOPIC, kafkaEvent.{entityId}().toString(), kafkaEvent)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[KAFKA] Publish failed | topic={} | eventId={} | error={}",
                        TOPIC, kafkaEvent.eventId(), ex.getMessage());
                } else {
                    log.info("[KAFKA] Published | topic={} | eventId={} | partition={} | offset={}",
                        TOPIC, kafkaEvent.eventId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}
```

### Template — Consumer
```java
package com.eprocure.{service}.infrastructure.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer: {Feature}EventConsumer
 * Consumes: {topic.name}
 * Idempotent: Yes (eventId deduplication via Redis)
 */
@Component
public class {Feature}EventConsumer {

    private static final Logger log = LogManager.getLogger({Feature}EventConsumer.class);

    private final IdempotencyService idempotencyService;
    private final {Feature}ProcessingService processingService;

    public {Feature}EventConsumer(
            IdempotencyService idempotencyService,
            {Feature}ProcessingService processingService) {
        this.idempotencyService = idempotencyService;
        this.processingService = processingService;
    }

    @KafkaListener(
        topics = "{topic.name}",
        groupId = "${app.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, {EventName}> record, Acknowledgment ack) {
        var event = record.value();
        log.info("[KAFKA] Received | topic={} | eventId={} | partition={} | offset={}",
            record.topic(), event.eventId(), record.partition(), record.offset());

        // Idempotency check
        if (idempotencyService.hasProcessed("kafka:" + event.eventId())) {
            log.info("[KAFKA] Duplicate skipped | eventId={}", event.eventId());
            ack.acknowledge();
            return;
        }

        try {
            processingService.handle(event);
            idempotencyService.markProcessed("kafka:" + event.eventId());
            ack.acknowledge();
            log.info("[KAFKA] Processed | eventId={}", event.eventId());
        } catch (Exception e) {
            log.error("[KAFKA] Processing failed | eventId={} | error={}", event.eventId(), e.getMessage(), e);
            // Do NOT ack — let retry/DLQ handle
        }
    }
}
```
