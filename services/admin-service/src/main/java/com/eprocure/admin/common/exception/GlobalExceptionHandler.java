package com.eprocure.admin.common.exception;

import com.eprocure.admin.common.api.ApiResponse;
import com.eprocure.admin.common.api.RequestIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception, HttpServletRequest request) {
        log.warn("[EXCEPTION][{}] {} | path={}", exception.getErrorCode(), exception.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(exception.getHttpStatus())
                .body(ApiResponse.failure(exception.getErrorCode(), exception.getMessage(), null, RequestIdUtil.resolve(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();
        log.warn("[EXCEPTION][{}] Validation failed | fields={} | path={}",
                ErrorCode.VAL_001.code(),
                details.size(),
                request.getRequestURI());
        return ResponseEntity
                .status(ErrorCode.VAL_001.status())
                .body(ApiResponse.failure(ErrorCode.VAL_001.code(), ErrorCode.VAL_001.message(), details, RequestIdUtil.resolve(request)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        log.warn("[EXCEPTION][{}] Access denied | path={}", ErrorCode.IAM_004.code(), request.getRequestURI());
        return ResponseEntity
                .status(ErrorCode.IAM_004.status())
                .body(ApiResponse.failure(ErrorCode.IAM_004.code(), ErrorCode.IAM_004.message(), null, RequestIdUtil.resolve(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("[EXCEPTION][{}] Unexpected error | path={}", ErrorCode.SYS_001.code(), request.getRequestURI(), exception);
        return ResponseEntity
                .status(ErrorCode.SYS_001.status())
                .body(ApiResponse.failure(ErrorCode.SYS_001.code(), ErrorCode.SYS_001.message(), null, RequestIdUtil.resolve(request)));
    }

    private FieldErrorDetail toDetail(FieldError error) {
        return new FieldErrorDetail(error.getField(), error.getDefaultMessage(), error.getRejectedValue());
    }

    public record FieldErrorDetail(String field, String reason, Object rejectedValue) {
    }
}
