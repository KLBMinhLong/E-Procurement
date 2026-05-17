## SK-08 · Exception & Error Code

### Trigger
Agent thêm exception mới hoặc định nghĩa error codes cho một service.

### Inputs Required
- Service prefix
- Danh sách error codes
- Exception types + use cases

### Rules
```
[R1] Mỗi service có prefix riêng theo chuẩn {PREFIX}_{NNN} (IAM_001, PR_001...)
[R2] BusinessException: lỗi nghiệp vụ dự kiến — log WARN
[R3] SystemException: lỗi kỹ thuật không mong đợi — log ERROR
[R4] Không throw RuntimeException generic — luôn dùng typed exception
[R5] Exception message là message hiển thị cho user (VI), log có thể kèm English
[R6] GlobalExceptionHandler trả về ApiResponse với error code
[R7] Stack trace chỉ log ở ERROR level, WARN level chỉ log message
```

### Reference — Error Code Convention
```
Service Prefix (underscore):
    IAM_001–IAM_099 : Identity & Access Management
    PR_001–PR_099   : Purchase Request
    APR_001–APR_099 : Approval Engine
    FIN_001–FIN_099 : Finance / Budget
    INV_001–INV_099 : Inventory
    VND_001–VND_099 : Vendor / RFQ
    NTF_001–NTF_099 : Notification
    GW_001–GW_099   : Gateway
    SYS_001–SYS_099 : System / Infrastructure
    VAL_001–VAL_099 : Validation
```

### Template
```java
// === Error Codes Enum ===
package com.eprocure.pr.common.constant;

/**
 * PR Service Error Codes.
 * Range: PR_001 to PR_099
 */
public enum PrErrorCode {

    // Validation
     PR_012_MISSING_LINE_ITEMS      ("PR_012", "PR phải có ít nhất 1 mục hàng hóa"),
    PR_010_INVALID_QUANTITY        ("PR_010", "Số lượng phải lớn hơn 0"),
    PR_014_MISSING_JUSTIFICATION   ("PR_014", "Lý do mua hàng phải ít nhất 50 ký tự"),

    // Not Found
    PR_001_NOT_FOUND               ("PR_001", "Yêu cầu mua sắm không tồn tại"),

    // Business Rules
    PR_003_INVALID_STATUS_TRANSITION ("PR_003", "Không thể thao tác ở trạng thái hiện tại"),
    PR_002_BUDGET_EXCEEDED           ("PR_002", "Ngân sách phòng ban không đủ"),
    PR_004_SELF_APPROVAL_NOT_ALLOWED ("PR_004", "Người tạo PR không được tự phê duyệt");

    private final String code;
    private final String defaultMessage;

    PrErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}

// === Base Exception ===
package com.eprocure.pr.common.exception;

public abstract class BusinessException extends RuntimeException {
    private final String errorCode;
    private final Object[] args;

    protected BusinessException(PrErrorCode error, Object... args) {
        super(error.getDefaultMessage());
        this.errorCode = error.getCode();
        this.args = args;
    }

    public String getErrorCode() { return errorCode; }
    public Object[] getArgs() { return args; }
}

// === Specific Exception ===
public class PrNotFoundException extends BusinessException {
    public PrNotFoundException(UUID id) {
        super(PrErrorCode.PR_001_NOT_FOUND, id);
    }
}

public class InsufficientBudgetException extends BusinessException {
    public InsufficientBudgetException(BigDecimal required, BigDecimal available) {
        super(PrErrorCode.PR_002_BUDGET_EXCEEDED, required, available);
    }
}

// === Global Handler ===
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("[EXCEPTION][{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("[EXCEPTION][SYS_001] Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("SYS_001", "Lỗi hệ thống, vui lòng thử lại sau"));
    }
}
```

### Checklist
```
[ ] Exception dùng typed class, không throw RuntimeException generic
[ ] BusinessException log WARN, SystemException log ERROR
[ ] ApiResponse trả về đúng error code
[ ] Error code theo prefix service
```
