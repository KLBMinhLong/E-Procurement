package com.eprocure.iam.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    IAM_001("IAM_001", "Invalid credentials", HttpStatus.UNAUTHORIZED),
    IAM_002("IAM_002", "Account is not allowed to login", HttpStatus.LOCKED),
    IAM_003("IAM_003", "Session is invalid or expired", HttpStatus.UNAUTHORIZED),
    IAM_004("IAM_004", "Permission denied", HttpStatus.FORBIDDEN),
    IAM_005("IAM_005", "Validation failed", HttpStatus.BAD_REQUEST),
    IAM_009("IAM_009", "IAM resource already exists", HttpStatus.CONFLICT),
    IAM_020("IAM_020", "Delegate must be at the same or higher organization level", HttpStatus.UNPROCESSABLE_ENTITY),
    IAM_021("IAM_021", "Delegation exceeds delegator authority", HttpStatus.UNPROCESSABLE_ENTITY),
    IAM_022("IAM_022", "Overlapping active delegation already exists", HttpStatus.CONFLICT),
    IAM_030("IAM_030", "User not found", HttpStatus.NOT_FOUND),
    IAM_031("IAM_031", "Role not found", HttpStatus.NOT_FOUND),
    IAM_032("IAM_032", "Permission not found", HttpStatus.NOT_FOUND),
    IAM_033("IAM_033", "Department not found", HttpStatus.NOT_FOUND),
    IAM_034("IAM_034", "Approver not found", HttpStatus.UNPROCESSABLE_ENTITY),
    IAM_035("IAM_035", "Delegation not found", HttpStatus.NOT_FOUND),
    SYS_001("SYS_001", "Unexpected system error", HttpStatus.INTERNAL_SERVER_ERROR),
    SYS_005("SYS_005", "Idempotency-Key is required and must be UUID v4", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus status() {
        return status;
    }
}
