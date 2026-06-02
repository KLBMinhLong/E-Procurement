package com.eprocure.vendor.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    IAM_004("IAM_004", "Permission denied", HttpStatus.FORBIDDEN),
    VND_001("VND_001", "Vendor not found", HttpStatus.NOT_FOUND),
    VND_002("VND_002", "Vendor tax code already exists", HttpStatus.CONFLICT),
    VND_003("VND_003", "Vendor is blacklisted", HttpStatus.UNPROCESSABLE_ENTITY),
    VND_004("VND_004", "RFQ not found", HttpStatus.NOT_FOUND),
    VND_005("VND_005", "RFQ is not changeable in current status", HttpStatus.CONFLICT),
    VND_008("VND_008", "Vendor is not on approved vendor list", HttpStatus.UNPROCESSABLE_ENTITY),
    VND_009("VND_009", "Purchase request is not approved for RFQ", HttpStatus.UNPROCESSABLE_ENTITY),
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
