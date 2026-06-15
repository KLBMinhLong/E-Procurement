package com.eprocure.admin.common.api;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Object meta,
        Instant timestamp,
        String requestId) {

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(true, "SUCCESS", null, data, null, Instant.now(), requestId);
    }

    public static <T> ApiResponse<T> successWithMeta(T data, Object meta, String requestId) {
        return new ApiResponse<>(true, "SUCCESS", null, data, meta, Instant.now(), requestId);
    }

    public static ApiResponse<Void> failure(String code, String message, Object meta, String requestId) {
        return new ApiResponse<>(false, code, message, null, meta, Instant.now(), requestId);
    }
}
