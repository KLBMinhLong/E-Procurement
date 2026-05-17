## SK-24 · Audit Log Writer

### Trigger
Agent thêm audit log cho các thao tác thay đổi trạng thái.

### Inputs Required
- AuditableEvent fields
- db_audit schema
- Sensitive field list

### Rules
```
[R1] Audit log là IMMUTABLE — không có UPDATE/DELETE trên bảng audit
[R2] Lưu trong database riêng: db_audit schema audit
[R3] Mọi action phải log: WHO, WHAT, WHEN, HOW, RESULT, CHANGE (old→new)
[R4] @TransactionalEventListener(phase = AFTER_COMMIT) — không lose log khi tx rollback
[R5] Log level: INFO với logger name "AUDIT" (ra file audit.log riêng)
[R6] Sensitive fields không được log: password_hash, token, private_key
[R7] JSON diff cho old_value → new_value
```

### Template
```java
@Component
public class AuditLogWriter {

    private static final Logger auditLog = LogManager.getLogger("AUDIT");
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void writeAuditLog(AuditableEvent event) {
        var log = AuditLog.builder()
            .id(UUID.randomUUID())
            .actorId(event.getActorId())
            .actorIp(event.getActorIp())
            .sessionId(event.getSessionId())
            .action(event.getAction())            // PR.SUBMITTED, PR.APPROVED, etc.
            .entityType(event.getEntityType())    // PURCHASE_REQUEST
            .entityId(event.getEntityId())
            .httpMethod(event.getHttpMethod())
            .endpoint(event.getEndpoint())
            .requestId(MDC.get("requestId"))
            .result(event.isSuccess() ? "SUCCESS" : "FAILURE")
            .errorCode(event.getErrorCode())
            .oldValue(toJson(event.getOldValue()))
            .newValue(toJson(event.getNewValue()))
            .occurredAt(Instant.now())
            .build();

        // Persist to db_audit (independent transaction)
        auditLogRepository.insert(log);

        // Also log to audit file
        auditLog.info("[AUDIT] action={} | entity={}:{} | actor={} | result={} | requestId={}",
            log.getAction(), log.getEntityType(), log.getEntityId(),
            log.getActorId(), log.getResult(), log.getRequestId());
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(
                SensitiveFieldMasker.mask(value)  // Mask password_hash, tokens, etc.
            );
        } catch (Exception e) {
            return "[SERIALIZATION_ERROR]";
        }
    }
}
```

### Checklist
```
[ ] Log sau transaction commit
[ ] Không log dữ liệu nhạy cảm
[ ] Audit table chỉ append (no update/delete)
[ ] Log file audit riêng
```
