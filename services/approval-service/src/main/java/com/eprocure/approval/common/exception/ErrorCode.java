package com.eprocure.approval.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    IAM_004("IAM_004", "Permission denied", HttpStatus.FORBIDDEN),
    APR_001("APR_001", "Approval rule not found", HttpStatus.NOT_FOUND),
    APR_002("APR_002", "Approver not found", HttpStatus.UNPROCESSABLE_ENTITY),
    APR_003("APR_003", "Approval task has already been processed", HttpStatus.CONFLICT),
    APR_004("APR_004", "Conflict of interest detected", HttpStatus.FORBIDDEN),
    APR_005("APR_005", "Approval comment is required or too short", HttpStatus.UNPROCESSABLE_ENTITY),
    SYS_001("SYS_001", "Unexpected system error", HttpStatus.INTERNAL_SERVER_ERROR),
    SYS_005("SYS_005", "Idempotency-Key is required and must be UUID v4", HttpStatus.BAD_REQUEST),
    VAL_001("VAL_001", "Validation failed", HttpStatus.BAD_REQUEST);

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
