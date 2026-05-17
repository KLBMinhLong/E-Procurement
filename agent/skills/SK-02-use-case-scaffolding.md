## SK-02 · Use Case Scaffolding

### Trigger
Agent tạo use case mới (một nghiệp vụ cụ thể: CreatePR, SubmitPR, ApproveStep...).

### Inputs Required
- Tên use case + tên command
- Aggregate/Repository liên quan
- External port cần gọi
- Idempotency key (header name)

### Rules
```
[R1] @Transactional đặt tại Use Case, KHÔNG phải Repository hay Controller
[R2] Read-only use case dùng @Transactional(readOnly = true)
[R3] Use Case không inject Controller hay infrastructure class trực tiếp
[R4] Use Case giao tiếp với external service qua Port (interface)
[R5] Tên class: [Verb][Noun]UseCase
[R6] Một Use Case = một method execute()
[R7] Domain events được publish SAU khi transaction commit (ApplicationEventPublisher)
[R8] Idempotency key được check ở đầu execute()
```

### Template — Use Case
```java
package com.eprocure.{service}.application.usecase;

import com.eprocure.{service}.application.port.in.{CommandName};
import com.eprocure.{service}.application.port.out.*;
import com.eprocure.{service}.common.exception.*;
import com.eprocure.{service}.domain.model.*;
import com.eprocure.{service}.domain.repository.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: {UseCaseName}
 * <p>Thực hiện [mô tả nghiệp vụ ngắn gọn].
 *
 * @see {CommandName}
 */
@Service
public class {UseCaseName} {

    private static final Logger log = LogManager.getLogger({UseCaseName}.class);

    private final {Entity}Repository repository;
    private final IdempotencyService idempotencyService;
    private final {ExternalPort} externalPort;         // Out port to external service
    private final ApplicationEventPublisher eventPublisher;

    public {UseCaseName}(
            {Entity}Repository repository,
            IdempotencyService idempotencyService,
            {ExternalPort} externalPort,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.externalPort = externalPort;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Executes the use case.
     *
     * @param command       Input command with validated data
     * @param idempotencyKey Client-provided idempotency key (UUID)
     * @return result domain object
     * @throws {Entity}NotFoundException if referenced entity not found
     * @throws BusinessException on business rule violation
     */
    @Transactional
    public {ResultType} execute({CommandName} command, String idempotencyKey) {
        log.info("[ACTION] Start | useCase={} | actor={} | idempotencyKey={}",
                "{UseCaseName}", command.getActorId(), idempotencyKey);

        // 1. Idempotency check
        return idempotencyService.getOrExecute(
            idempotencyKey,
            {ResultType}.class,
            () -> doExecute(command)
        );
    }

    private {ResultType} doExecute({CommandName} command) {
        // 2. Load aggregate
        {Entity} entity = repository.findById(command.getEntityId())
            .orElseThrow(() -> new {Entity}NotFoundException(command.getEntityId()));

        // 3. Execute domain logic
        entity.{businessMethod}(command.getActorId(), command.getData());

        // 4. Persist
        repository.save(entity);

        // 5. Publish domain events (after commit)
        entity.pullDomainEvents().forEach(eventPublisher::publishEvent);

        log.info("[ACTION] Complete | useCase={} | entityId={} | newStatus={}",
                "{UseCaseName}", entity.getId(), entity.getStatus());

        return entity;
    }
}
```

### Template — Command
```java
package com.eprocure.{service}.application.port.in;

import java.util.UUID;

/**
 * Command: {CommandName}
 * Input DTO cho use case {UseCaseName}.
 * Immutable after construction.
 */
public final class {CommandName} {

    private final UUID actorId;
    private final UUID entityId;
    // ... other fields

    public {CommandName}(UUID actorId, UUID entityId /*, ...*/) {
        this.actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        this.entityId = Objects.requireNonNull(entityId, "entityId must not be null");
    }

    // Getters only — no setters
    public UUID getActorId() { return actorId; }
    public UUID getEntityId() { return entityId; }
}
```

### Checklist
```
[ ] @Transactional nằm ở Use Case
[ ] Idempotency check ở đầu execute()
[ ] Không inject Controller hoặc infrastructure class trực tiếp
[ ] Domain events publish sau commit
```
