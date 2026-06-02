package com.eprocure.vendor.common.exception;

import java.util.Optional;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public BusinessException(ErrorCode errorCode, Object details) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Optional<Object> getDetails() {
        return Optional.ofNullable(details);
    }
}
