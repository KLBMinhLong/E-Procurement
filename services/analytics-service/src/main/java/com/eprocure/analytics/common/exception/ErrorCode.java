package com.eprocure.analytics.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    IAM_004("IAM_004", "Permission denied", HttpStatus.FORBIDDEN),
    ANL_001("ANL_001", "Analytics dashboard snapshot not found", HttpStatus.NOT_FOUND),
    ANL_002("ANL_002", "Report job not found", HttpStatus.NOT_FOUND),
    ANL_003("ANL_003", "Report file is not ready", HttpStatus.NOT_FOUND),
    SYS_005("SYS_005", "Idempotency-Key is required and must be UUID v4", HttpStatus.BAD_REQUEST),
    SYS_001("SYS_001", "Unexpected system error", HttpStatus.INTERNAL_SERVER_ERROR),
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
