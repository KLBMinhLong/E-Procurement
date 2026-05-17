package com.eprocure.iam.presentation.internal;

import com.eprocure.iam.application.service.InternalApiKeyGuard;
import com.eprocure.iam.application.service.KeycloakUserView;
import com.eprocure.iam.application.usecase.GetKeycloakUserUseCase;
import com.eprocure.iam.application.usecase.VerifyKeycloakCredentialUseCase;
import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/keycloak")
public class KeycloakInternalController {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final Logger log = LogManager.getLogger(KeycloakInternalController.class);
    private final InternalApiKeyGuard internalApiKeyGuard;
    private final GetKeycloakUserUseCase getKeycloakUserUseCase;
    private final VerifyKeycloakCredentialUseCase verifyKeycloakCredentialUseCase;

    public KeycloakInternalController(
            InternalApiKeyGuard internalApiKeyGuard,
            GetKeycloakUserUseCase getKeycloakUserUseCase,
            VerifyKeycloakCredentialUseCase verifyKeycloakCredentialUseCase) {
        this.internalApiKeyGuard = internalApiKeyGuard;
        this.getKeycloakUserUseCase = getKeycloakUserUseCase;
        this.verifyKeycloakCredentialUseCase = verifyKeycloakCredentialUseCase;
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<KeycloakUserView>> getUserById(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String internalApiKey,
            @PathVariable UUID userId,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] GET /internal/keycloak/users/{} | userId=internal", LogMaskingUtil.maskId(userId));
        return ResponseEntity.ok(ApiResponse.success(getKeycloakUserUseCase.byId(userId), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<KeycloakUserView>> getUserByLogin(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String internalApiKey,
            @RequestParam String login,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] GET /internal/keycloak/users | userId=internal");
        return ResponseEntity.ok(ApiResponse.success(getKeycloakUserUseCase.byLogin(login), RequestIdUtil.resolve(request)));
    }

    @PostMapping("/credentials/verify")
    public ResponseEntity<ApiResponse<KeycloakCredentialVerifyResponse>> verifyCredential(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String internalApiKey,
            @Valid @RequestBody KeycloakCredentialVerifyRequest body,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] POST /internal/keycloak/credentials/verify | userId=internal");
        boolean valid = verifyKeycloakCredentialUseCase.execute(body.username(), body.password());
        return ResponseEntity.ok(ApiResponse.success(new KeycloakCredentialVerifyResponse(valid), RequestIdUtil.resolve(request)));
    }
}
