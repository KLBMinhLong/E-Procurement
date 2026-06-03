package com.eprocure.finance.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    IAM_004("IAM_004", "Permission denied", HttpStatus.FORBIDDEN),
    FIN_001("FIN_001", "Active budget not found", HttpStatus.NOT_FOUND),
    FIN_002("FIN_002", "Requested amount must be greater than zero", HttpStatus.BAD_REQUEST),
    FIN_003("FIN_003", "Budget currency mismatch", HttpStatus.UNPROCESSABLE_ENTITY),
    FIN_004("FIN_004", "Budget override exceeds configured threshold", HttpStatus.FORBIDDEN),
    FIN_005("FIN_005", "Budget is not active", HttpStatus.UNPROCESSABLE_ENTITY),
    FIN_006("FIN_006", "Purchase Order not found", HttpStatus.NOT_FOUND),
    FIN_007("FIN_007", "Invoice not found", HttpStatus.NOT_FOUND),
    FIN_008("FIN_008", "Invoice already exists", HttpStatus.CONFLICT),
    FIN_009("FIN_009", "Source budget is insufficient for transfer", HttpStatus.UNPROCESSABLE_ENTITY),
    FIN_010("FIN_010", "Invoice does not match Purchase Order", HttpStatus.UNPROCESSABLE_ENTITY),
    FIN_011("FIN_011", "Invalid Purchase Order status transition", HttpStatus.UNPROCESSABLE_ENTITY),
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
