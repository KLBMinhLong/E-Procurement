package com.eprocure.pr.infrastructure.security;

import com.eprocure.pr.common.security.UserPrincipal;
import com.eprocure.pr.common.util.LogMaskingUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LogManager.getLogger(GatewayAuthenticationFilter.class);
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String DEPARTMENT_ID_HEADER = "X-Department-ID";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String FULL_NAME_HEADER = "X-Full-Name";
    private static final String PERMISSIONS_HEADER = "X-Permissions";
    private static final String API_KEY_HEADER = "X-Api-Key";

    private final String expectedApiKey;

    public GatewayAuthenticationFilter(@Value("${eprocure.internal.api-key:}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey == null ? "" : expectedApiKey;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (requiresApiKey() && !expectedApiKey.equals(request.getHeader(API_KEY_HEADER))) {
            log.warn("[APIKEY] Invalid | ip={} | path={}",
                    LogMaskingUtil.maskClientIp(request.getRemoteAddr()),
                    request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateFromGatewayHeaders(request).ifPresent(this::authenticate);
        }
        filterChain.doFilter(request, response);
    }

    private boolean requiresApiKey() {
        return !expectedApiKey.isBlank();
    }

    private Optional<UserPrincipal> authenticateFromGatewayHeaders(HttpServletRequest request) {
        String userIdValue = request.getHeader(USER_ID_HEADER);
        String departmentIdValue = request.getHeader(DEPARTMENT_ID_HEADER);
        if (userIdValue == null || userIdValue.isBlank() || departmentIdValue == null || departmentIdValue.isBlank()) {
            return Optional.empty();
        }
        try {
            UUID userId = UUID.fromString(userIdValue);
            UUID departmentId = UUID.fromString(departmentIdValue);
            String username = normalize(request.getHeader(USERNAME_HEADER), userId.toString());
            String fullName = normalize(request.getHeader(FULL_NAME_HEADER), username);
            Set<String> permissions = parsePermissions(request.getHeader(PERMISSIONS_HEADER));
            return Optional.of(new UserPrincipal(userId, departmentId, username, fullName, permissions));
        } catch (IllegalArgumentException exception) {
            log.warn("[SECURITY] Invalid gateway user headers | path={}", request.getRequestURI());
            return Optional.empty();
        }
    }

    private void authenticate(UserPrincipal principal) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "",
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Set<String> parsePermissions(String rawPermissions) {
        if (rawPermissions == null || rawPermissions.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rawPermissions.split("[, ]+"))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
