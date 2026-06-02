package com.eprocure.vendor.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Object meta,
        Object details,
        Instant timestamp,
        String requestId) {

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(true, "SUCCESS", null, data, null, null, Instant.now(), requestId);
    }

    public static <T> ApiResponse<T> successWithMeta(T data, Object meta, String requestId) {
        return new ApiResponse<>(true, "SUCCESS", null, data, meta, null, Instant.now(), requestId);
    }

    public static ApiResponse<Void> failure(String code, String message, Object details, String requestId) {
        return new ApiResponse<>(false, code, message, null, null, details, Instant.now(), requestId);
    }
}
