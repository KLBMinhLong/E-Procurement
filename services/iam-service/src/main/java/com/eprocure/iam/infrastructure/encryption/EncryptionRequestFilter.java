package com.eprocure.iam.infrastructure.encryption;

import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(name = "eprocure.encryption.enabled", havingValue = "true")
public class EncryptionRequestFilter extends OncePerRequestFilter {
    private static final Logger log = LogManager.getLogger(EncryptionRequestFilter.class);
    private static final Set<String> METHODS_WITH_BODY = Set.of("POST", "PUT", "PATCH");
    private final ObjectMapper objectMapper;
    private final HybridEncryptionService hybridEncryptionService;

    public EncryptionRequestFilter(ObjectMapper objectMapper, HybridEncryptionService hybridEncryptionService) {
        this.objectMapper = objectMapper;
        this.hybridEncryptionService = hybridEncryptionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !METHODS_WITH_BODY.contains(request.getMethod())
                || path.startsWith("/actuator/")
                || path.equals("/api/v1/auth/public-key")
                || !isJson(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (requestBody.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            EncryptedRequest encryptedRequest = objectMapper.readValue(requestBody, EncryptedRequest.class);
            String decryptedBody = hybridEncryptionService.decrypt(encryptedRequest);
            CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(
                    request,
                    decryptedBody.getBytes(StandardCharsets.UTF_8));
            filterChain.doFilter(wrappedRequest, response);
        } catch (InvalidEncryptedPayloadException | IllegalArgumentException exception) {
            log.warn("[SECURITY] Invalid encrypted payload | path={}", request.getRequestURI());
            writeBadRequest(response, request);
        }
    }

    private boolean isJson(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    private void writeBadRequest(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(ErrorCode.IAM_005.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.failure(ErrorCode.IAM_005.code(), "Invalid encrypted payload", null, RequestIdUtil.resolve(request)));
    }
}
