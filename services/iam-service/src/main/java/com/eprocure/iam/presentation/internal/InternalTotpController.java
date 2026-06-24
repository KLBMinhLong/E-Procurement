package com.eprocure.iam.presentation.internal;

import com.eprocure.iam.application.port.in.VerifyUserTotpCommand;
import com.eprocure.iam.application.service.InternalApiKeyGuard;
import com.eprocure.iam.application.usecase.VerifyUserTotpUseCase;
import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/security/totp")
public class InternalTotpController {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final Logger log = LogManager.getLogger(InternalTotpController.class);

    private final InternalApiKeyGuard internalApiKeyGuard;
    private final VerifyUserTotpUseCase verifyUserTotpUseCase;

    public InternalTotpController(
            InternalApiKeyGuard internalApiKeyGuard,
            VerifyUserTotpUseCase verifyUserTotpUseCase) {
        this.internalApiKeyGuard = internalApiKeyGuard;
        this.verifyUserTotpUseCase = verifyUserTotpUseCase;
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyTotp(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String internalApiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody InternalTotpVerificationRequest body,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] POST /internal/security/totp/verify | userId=internal | targetUserId={}",
                LogMaskingUtil.maskId(body.userId()));
        verifyUserTotpUseCase.execute(new VerifyUserTotpCommand(body.userId(), body.code()), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.successMessage("TOTP verified", RequestIdUtil.resolve(request)));
    }
}
