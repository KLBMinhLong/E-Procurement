package com.eprocure.admin.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SERVICE_CONFIG_NOT_FOUND("SYS_010", "Không tìm thấy cấu hình service", HttpStatus.NOT_FOUND),
    AUDIT_EXPORT_JOB_NOT_FOUND("SYS_011", "Không tìm thấy audit export job", HttpStatus.NOT_FOUND),
    AUDIT_EXPORT_FILE_NOT_READY("SYS_012", "Audit export file chưa sẵn sàng", HttpStatus.NOT_FOUND),
    SYS_001("SYS_001", "Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    SYS_002("SYS_002", "Dịch vụ phụ thuộc không khả dụng", HttpStatus.SERVICE_UNAVAILABLE),
    SYS_005("SYS_005", "Idempotency-Key is required and must be UUID v4", HttpStatus.BAD_REQUEST),
    IAM_004("IAM_004", "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    VAL_001("VAL_001", "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST);

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
