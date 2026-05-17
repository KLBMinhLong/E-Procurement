package com.eprocure.iam.presentation.auth;

import com.eprocure.iam.application.port.in.ClientContext;
import com.eprocure.iam.application.port.in.ForgotPasswordCommand;
import com.eprocure.iam.application.port.in.GoogleOAuthCallbackCommand;
import com.eprocure.iam.application.port.in.LoginCommand;
import com.eprocure.iam.application.port.in.ResetPasswordCommand;
import com.eprocure.iam.application.port.in.VerifyTwoFactorCommand;
import com.eprocure.iam.application.port.out.PublicKeyInfo;
import com.eprocure.iam.application.service.GoogleOAuthRedirect;
import com.eprocure.iam.application.service.LoginResult;
import com.eprocure.iam.application.service.TwoFactorVerificationResult;
import com.eprocure.iam.application.service.UserSummaryView;
import com.eprocure.iam.application.usecase.GetPublicKeyUseCase;
import com.eprocure.iam.application.usecase.ForgotPasswordUseCase;
import com.eprocure.iam.application.usecase.HandleGoogleOAuthCallbackUseCase;
import com.eprocure.iam.application.usecase.LoginUseCase;
import com.eprocure.iam.application.usecase.LogoutUseCase;
import com.eprocure.iam.application.usecase.ResetPasswordUseCase;
import com.eprocure.iam.application.usecase.StartGoogleOAuthUseCase;
import com.eprocure.iam.application.usecase.VerifyTwoFactorUseCase;
import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.security.UserPrincipal;
import com.eprocure.iam.common.util.LogMaskingUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger log = LogManager.getLogger(AuthController.class);
    private final GetPublicKeyUseCase getPublicKeyUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final VerifyTwoFactorUseCase verifyTwoFactorUseCase;
    private final StartGoogleOAuthUseCase startGoogleOAuthUseCase;
    private final HandleGoogleOAuthCallbackUseCase handleGoogleOAuthCallbackUseCase;
    private final String cookieName;
    private final String twoFactorCookieName;
    private final String oauthStateCookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final String cookieDomain;
    private final Duration sessionTtl;
    private final Duration twoFactorChallengeTtl;
    private final Duration oauthStateTtl;

    public AuthController(
            GetPublicKeyUseCase getPublicKeyUseCase,
            LoginUseCase loginUseCase,
            LogoutUseCase logoutUseCase,
            ForgotPasswordUseCase forgotPasswordUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            VerifyTwoFactorUseCase verifyTwoFactorUseCase,
            StartGoogleOAuthUseCase startGoogleOAuthUseCase,
            HandleGoogleOAuthCallbackUseCase handleGoogleOAuthCallbackUseCase,
            @Value("${eprocure.security.cookie-name:ep_session}") String cookieName,
            @Value("${eprocure.two-factor.challenge-cookie-name:ep_2fa}") String twoFactorCookieName,
            @Value("${eprocure.google-oauth.state-cookie-name:ep_oauth_state}") String oauthStateCookieName,
            @Value("${eprocure.security.cookie-secure:false}") boolean cookieSecure,
            @Value("${eprocure.security.cookie-same-site:Strict}") String cookieSameSite,
            @Value("${eprocure.security.cookie-domain:}") String cookieDomain,
            @Value("${eprocure.session.ttl-hours:8}") long sessionTtlHours,
            @Value("${eprocure.two-factor.challenge-ttl-minutes:5}") long twoFactorChallengeTtlMinutes,
            @Value("${eprocure.google-oauth.state-ttl-minutes:5}") long oauthStateTtlMinutes) {
        this.getPublicKeyUseCase = getPublicKeyUseCase;
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.forgotPasswordUseCase = forgotPasswordUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.verifyTwoFactorUseCase = verifyTwoFactorUseCase;
        this.startGoogleOAuthUseCase = startGoogleOAuthUseCase;
        this.handleGoogleOAuthCallbackUseCase = handleGoogleOAuthCallbackUseCase;
        this.cookieName = cookieName;
        this.twoFactorCookieName = twoFactorCookieName;
        this.oauthStateCookieName = oauthStateCookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.cookieDomain = cookieDomain;
        this.sessionTtl = Duration.ofHours(sessionTtlHours);
        this.twoFactorChallengeTtl = Duration.ofMinutes(twoFactorChallengeTtlMinutes);
        this.oauthStateTtl = Duration.ofMinutes(oauthStateTtlMinutes);
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
        if (result.requiresTwoFactor()) {
            response.addHeader(HttpHeaders.SET_COOKIE, clearCookie(cookieName).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, twoFactorChallengeCookie(result.rawToken()).toString());
        } else {
            response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(result.rawToken()).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, clearCookie(twoFactorCookieName).toString());
        }
        LoginResponse data = new LoginResponse(
                result.userId(),
                result.fullName(),
                result.avatarUrl(),
                result.requiresTwoFactor());
        return ResponseEntity.ok(ApiResponse.success(data, RequestIdUtil.resolve(request)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ForgotPasswordRequest body,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/auth/forgot-password | email={}", LogMaskingUtil.maskEmail(body.email()));
        forgotPasswordUseCase.execute(new ForgotPasswordCommand(body.email(), idempotencyKey));
        return ResponseEntity.ok(ApiResponse.successMessage(
                "If the email exists, reset instructions will be sent",
                RequestIdUtil.resolve(request)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ResetPasswordRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("[CONTROLLER] POST /api/v1/auth/reset-password | userId=anonymous");
        resetPasswordUseCase.execute(new ResetPasswordCommand(
                body.resetToken(),
                body.newPassword(),
                body.confirmPassword(),
                idempotencyKey));
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie(cookieName).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie(twoFactorCookieName).toString());
        return ResponseEntity.ok(ApiResponse.successMessage("Password has been reset", RequestIdUtil.resolve(request)));
    }

    @GetMapping("/oauth/google")
    public ResponseEntity<Void> startGoogleOAuth(HttpServletRequest request, HttpServletResponse response) {
        log.info("[CONTROLLER] GET /api/v1/auth/oauth/google | userId=anonymous");
        GoogleOAuthRedirect redirect = startGoogleOAuthUseCase.execute();
        response.addHeader(HttpHeaders.SET_COOKIE, oauthStateCookie(redirect.state()).toString());
        return ResponseEntity.status(302)
                .location(URI.create(redirect.authorizationUrl()))
                .build();
    }

    @GetMapping("/oauth/google/callback")
    public ResponseEntity<ApiResponse<LoginResponse>> handleGoogleOAuthCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("[CONTROLLER] GET /api/v1/auth/oauth/google/callback | userId=anonymous");
        LoginResult result = handleGoogleOAuthCallbackUseCase.execute(new GoogleOAuthCallbackCommand(
                code,
                state,
                extractCookie(request, oauthStateCookieName).orElse(""),
                ClientContext.of(resolveClientIp(request), request.getHeader(HttpHeaders.USER_AGENT))));
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie(oauthStateCookieName).toString());
        if (result.requiresTwoFactor()) {
            response.addHeader(HttpHeaders.SET_COOKIE, clearCookie(cookieName).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, twoFactorChallengeCookie(result.rawToken()).toString());
        } else {
            response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(result.rawToken()).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, clearCookie(twoFactorCookieName).toString());
        }
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
        logoutUseCase.execute(extractCookie(request, cookieName).orElse(""), principal.getId(), idempotencyKey);
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie(cookieName).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie(twoFactorCookieName).toString());
        return ResponseEntity.ok(ApiResponse.successMessage("Logged out", RequestIdUtil.resolve(request)));
    }

    @PostMapping("/two-factor/verify")
    public ResponseEntity<ApiResponse<UserSummaryView>> verifyTwoFactor(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TwoFactorVerifyRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("[CONTROLLER] POST /api/v1/auth/two-factor/verify | userId=pending");
        TwoFactorVerificationResult result = verifyTwoFactorUseCase.execute(new VerifyTwoFactorCommand(
                extractCookie(request, twoFactorCookieName).orElse(""),
                body.code(),
                idempotencyKey,
                ClientContext.of(resolveClientIp(request), request.getHeader(HttpHeaders.USER_AGENT))));
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(result.rawToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie(twoFactorCookieName).toString());
        return ResponseEntity.ok(ApiResponse.success(result.user(), RequestIdUtil.resolve(request)));
    }

    private ResponseCookie sessionCookie(String rawToken) {
        return cookie(cookieName, rawToken, sessionTtl);
    }

    private ResponseCookie twoFactorChallengeCookie(String rawToken) {
        return cookie(twoFactorCookieName, rawToken, twoFactorChallengeTtl);
    }

    private ResponseCookie oauthStateCookie(String rawToken) {
        return cookie(oauthStateCookieName, rawToken, oauthStateTtl);
    }

    private ResponseCookie cookie(String name, String rawToken, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, rawToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    private ResponseCookie clearCookie(String name) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
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

    private Optional<String> extractCookie(HttpServletRequest request, String name) {
        return Optional.ofNullable(request.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> name.equals(cookie.getName()))
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
