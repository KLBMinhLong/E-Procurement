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
    FIN_012("FIN_012", "Invoice status does not allow matching", HttpStatus.UNPROCESSABLE_ENTITY),
    FIN_013("FIN_013", "Invoice is not ready for approval", HttpStatus.UNPROCESSABLE_ENTITY),
    FIN_014("FIN_014", "Invoice is not ready for dispute", HttpStatus.UNPROCESSABLE_ENTITY),
    FIN_015("FIN_015", "Invoice is not ready for payment", HttpStatus.UNPROCESSABLE_ENTITY),
    FIN_016("FIN_016", "Payment amount must match invoice total", HttpStatus.UNPROCESSABLE_ENTITY),
    FIN_017("FIN_017", "Purchase request is not eligible for manual PO creation", HttpStatus.UNPROCESSABLE_ENTITY),
    FIN_018("FIN_018", "Active purchase order already exists for the purchase request", HttpStatus.CONFLICT),
    FIN_019("FIN_019", "Vendor is not eligible for PO creation", HttpStatus.UNPROCESSABLE_ENTITY),
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
