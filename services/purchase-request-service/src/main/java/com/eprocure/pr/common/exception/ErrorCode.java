package com.eprocure.pr.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    IAM_004("IAM_004", "Permission denied", HttpStatus.FORBIDDEN),
    PR_001("PR_001", "Purchase request not found", HttpStatus.NOT_FOUND),
    PR_002("PR_002", "Department budget is insufficient", HttpStatus.UNPROCESSABLE_ENTITY),
    PR_003("PR_003", "Purchase request cannot be changed in current status", HttpStatus.CONFLICT),
    PR_006("PR_006", "Urgency reason must be at least 100 characters", HttpStatus.UNPROCESSABLE_ENTITY),
    PR_008("PR_008", "File size exceeds limit (10MB)", HttpStatus.UNPROCESSABLE_ENTITY),
    PR_009("PR_009", "Invalid file type", HttpStatus.UNPROCESSABLE_ENTITY),
    PR_010("PR_010", "Quantity must be greater than zero", HttpStatus.BAD_REQUEST),
    PR_011("PR_011", "Unit price must be greater than or equal to zero", HttpStatus.BAD_REQUEST),
    PR_012("PR_012", "Purchase request must have at least one line item", HttpStatus.BAD_REQUEST),
    PR_013("PR_013", "Need-by date must not be in the past", HttpStatus.BAD_REQUEST),
    PR_014("PR_014", "Justification must be at least 50 characters", HttpStatus.UNPROCESSABLE_ENTITY),
    PR_015("PR_015", "Catalog item is not found or inactive", HttpStatus.UNPROCESSABLE_ENTITY),
    PR_016("PR_016", "Catalog category is not found or inactive", HttpStatus.NOT_FOUND),
    PR_017("PR_017", "Catalog category already exists", HttpStatus.CONFLICT),
    PR_018("PR_018", "Catalog category cannot be deactivated while active items exist", HttpStatus.CONFLICT),
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
