package com.eprocure.iam.common.exception;

import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("[EXCEPTION][{}] {} | path={}", errorCode.code(), errorCode.message(), request.getRequestURI());
        return ResponseEntity.status(errorCode.status())
                .body(ApiResponse.failure(errorCode.code(), errorCode.message(), null, RequestIdUtil.resolve(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ValidationError> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError)
                .toList();
        log.warn("[EXCEPTION][{}] Validation failed | path={}", ErrorCode.IAM_005.code(), request.getRequestURI());
        return ResponseEntity.status(ErrorCode.IAM_005.status())
                .body(ApiResponse.failure(ErrorCode.IAM_005.code(), ErrorCode.IAM_005.message(), details, RequestIdUtil.resolve(request)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        log.warn("[EXCEPTION][{}] Type mismatch | path={}", ErrorCode.IAM_005.code(), request.getRequestURI());
        return ResponseEntity.status(ErrorCode.IAM_005.status())
                .body(ApiResponse.failure(
                        ErrorCode.IAM_005.code(),
                        ErrorCode.IAM_005.message(),
                        List.of(new ValidationError(exception.getName(), "Invalid value", exception.getValue())),
                        RequestIdUtil.resolve(request)));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public ResponseEntity<ApiResponse<Void>> handleSecurity(Exception exception, HttpServletRequest request) {
        log.warn("[SECURITY] Unauthorized | path={}", request.getRequestURI());
        return ResponseEntity.status(ErrorCode.IAM_004.status())
                .body(ApiResponse.failure(ErrorCode.IAM_004.code(), ErrorCode.IAM_004.message(), null, RequestIdUtil.resolve(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("[EXCEPTION][{}] Unexpected failure | path={}", ErrorCode.SYS_001.code(), request.getRequestURI(), exception);
        return ResponseEntity.status(ErrorCode.SYS_001.status())
                .body(ApiResponse.failure(ErrorCode.SYS_001.code(), ErrorCode.SYS_001.message(), null, RequestIdUtil.resolve(request)));
    }

    private ValidationError toValidationError(FieldError error) {
        return new ValidationError(error.getField(), error.getDefaultMessage(), error.getRejectedValue());
    }

    public record ValidationError(String field, String reason, Object rejectedValue) {
    }
}
