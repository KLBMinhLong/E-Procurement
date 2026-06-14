package com.eprocure.inventory.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    IAM_004("IAM_004", "Permission denied", HttpStatus.FORBIDDEN),
    INV_001("INV_001", "Item not found", HttpStatus.NOT_FOUND),
    INV_002("INV_002", "Warehouse not found", HttpStatus.NOT_FOUND),
    INV_003("INV_003", "Insufficient stock quantity", HttpStatus.UNPROCESSABLE_ENTITY),
    INV_004("INV_004", "Goods Receipt not found", HttpStatus.NOT_FOUND),
    INV_005("INV_005", "Goods Receipt has already been completed", HttpStatus.CONFLICT),
    INV_006("INV_006", "Received quantity exceeds configured tolerance", HttpStatus.UNPROCESSABLE_ENTITY),
    INV_007("INV_007", "Catalog item code already exists", HttpStatus.CONFLICT),
    INV_008("INV_008", "Invalid inventory event payload", HttpStatus.UNPROCESSABLE_ENTITY),
    INV_009("INV_009", "Issued Purchase Order snapshot not found", HttpStatus.NOT_FOUND),
    INV_010("INV_010", "Purchase Order line item not found in snapshot", HttpStatus.UNPROCESSABLE_ENTITY),
    INV_011("INV_011", "Stock adjustment delta must not be zero", HttpStatus.UNPROCESSABLE_ENTITY),
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
