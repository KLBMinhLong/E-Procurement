package com.eprocure.approval.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    IAM_004("IAM_004", "Permission denied", HttpStatus.FORBIDDEN),
    APR_001("APR_001", "Conflict of interest detected", HttpStatus.FORBIDDEN),
    APR_002("APR_002", "Actor is not assigned to this approval task", HttpStatus.FORBIDDEN),
    APR_003("APR_003", "Approval task has already been processed", HttpStatus.CONFLICT),
    APR_004("APR_004", "Approval task not found", HttpStatus.NOT_FOUND),
    APR_005("APR_005", "Approval comment is required or too short", HttpStatus.UNPROCESSABLE_ENTITY),
    APR_006("APR_006", "Approval task cannot be forwarded to this user", HttpStatus.UNPROCESSABLE_ENTITY),
    APR_007("APR_007", "Approval rule is invalid or inactive", HttpStatus.UNPROCESSABLE_ENTITY),
    APR_008("APR_008", "Approval process has already been completed", HttpStatus.CONFLICT),
    APR_009("APR_009", "Approval rule not found", HttpStatus.NOT_FOUND),
    APR_010("APR_010", "Approval rule name already exists", HttpStatus.CONFLICT),
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
