package com.eprocure.iam.presentation.user;

import com.eprocure.iam.application.service.CurrentUserView;
import com.eprocure.iam.application.usecase.GetCurrentUserUseCase;
import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.security.UserPrincipal;
import com.eprocure.iam.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public UserController(GetCurrentUserUseCase getCurrentUserUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('IAM_PROFILE_READ')")
    public ResponseEntity<ApiResponse<CurrentUserView>> getMe(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/users/me | userId={}", LogMaskingUtil.maskId(principal.getId()));
        return ResponseEntity.ok(ApiResponse.success(
                getCurrentUserUseCase.execute(principal.getId()),
                RequestIdUtil.resolve(request)));
    }
}
