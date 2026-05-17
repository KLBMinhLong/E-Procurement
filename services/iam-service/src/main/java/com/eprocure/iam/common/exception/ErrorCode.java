package com.eprocure.iam.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    IAM_001("IAM_001", "Invalid credentials", HttpStatus.UNAUTHORIZED),
    IAM_002("IAM_002", "Account is not allowed to login", HttpStatus.LOCKED),
    IAM_003("IAM_003", "Session is invalid or expired", HttpStatus.UNAUTHORIZED),
    IAM_004("IAM_004", "Permission denied", HttpStatus.FORBIDDEN),
    IAM_005("IAM_005", "Validation failed", HttpStatus.BAD_REQUEST),
    IAM_030("IAM_030", "User not found", HttpStatus.NOT_FOUND),
    SYS_001("SYS_001", "Unexpected system error", HttpStatus.INTERNAL_SERVER_ERROR);

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
