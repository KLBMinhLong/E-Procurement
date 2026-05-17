package com.eprocure.iam.presentation.auth;

import com.eprocure.iam.application.port.in.ClientContext;
import com.eprocure.iam.application.port.in.LoginCommand;
import com.eprocure.iam.application.port.out.PublicKeyInfo;
import com.eprocure.iam.application.service.LoginResult;
import com.eprocure.iam.application.usecase.GetPublicKeyUseCase;
import com.eprocure.iam.application.usecase.LoginUseCase;
import com.eprocure.iam.application.usecase.LogoutUseCase;
import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.security.UserPrincipal;
import com.eprocure.iam.common.util.LogMaskingUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final GetPublicKeyUseCase getPublicKeyUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final String cookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final String cookieDomain;
    private final Duration sessionTtl;

    public AuthController(
            GetPublicKeyUseCase getPublicKeyUseCase,
            LoginUseCase loginUseCase,
            LogoutUseCase logoutUseCase,
            @Value("${eprocure.security.cookie-name:ep_session}") String cookieName,
            @Value("${eprocure.security.cookie-secure:false}") boolean cookieSecure,
            @Value("${eprocure.security.cookie-same-site:Strict}") String cookieSameSite,
            @Value("${eprocure.security.cookie-domain:}") String cookieDomain,
            @Value("${eprocure.session.ttl-hours:8}") long sessionTtlHours) {
        this.getPublicKeyUseCase = getPublicKeyUseCase;
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.cookieDomain = cookieDomain;
        this.sessionTtl = Duration.ofHours(sessionTtlHours);
    }

    @GetMapping("/public-key")
    public ResponseEntity<ApiResponse<PublicKeyInfo>> getPublicKey(HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/auth/public-key | userId=anonymous");
        return ResponseEntity.ok(ApiResponse.success(getPublicKeyUseCase.execute(), RequestIdUtil.resolve(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("[CONTROLLER] POST /api/v1/auth/login | userId=anonymous");
        LoginResult result = loginUseCase.execute(new LoginCommand(
                loginRequest.username(),
                loginRequest.password(),
                idempotencyKey,
                ClientContext.of(resolveClientIp(request), request.getHeader(HttpHeaders.USER_AGENT))));
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(result.rawToken()).toString());
        LoginResponse data = new LoginResponse(
                result.userId(),
                result.fullName(),
                result.avatarUrl(),
                result.requiresTwoFactor());
        return ResponseEntity.ok(ApiResponse.success(data, RequestIdUtil.resolve(request)));
    }

    @PostMapping("/logout")
    @PreAuthorize("hasAuthority('IAM_SESSION_REVOKE')")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("[CONTROLLER] POST /api/v1/auth/logout | userId={}", LogMaskingUtil.maskId(principal.getId()));
        logoutUseCase.execute(extractSessionCookie(request).orElse(""), principal.getId(), idempotencyKey);
        response.addHeader(HttpHeaders.SET_COOKIE, clearSessionCookie().toString());
        return ResponseEntity.ok(ApiResponse.successMessage("Logged out", RequestIdUtil.resolve(request)));
    }

    private ResponseCookie sessionCookie(String rawToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, rawToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(sessionTtl);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    private ResponseCookie clearSessionCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ZERO);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    private Optional<String> extractSessionCookie(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private String resolveClientIp(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .map(value -> value.split(",", 2)[0].trim())
                .filter(value -> !value.isBlank())
                .orElseGet(request::getRemoteAddr);
    }
}
