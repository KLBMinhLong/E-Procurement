package com.eprocure.inventory.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    IAM_004("IAM_004", "Permission denied", HttpStatus.FORBIDDEN),
    INV_001("INV_001", "Item not found", HttpStatus.NOT_FOUND),
    INV_004("INV_004", "Goods Receipt not found", HttpStatus.NOT_FOUND),
    INV_008("INV_008", "Invalid inventory event payload", HttpStatus.UNPROCESSABLE_ENTITY),
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
