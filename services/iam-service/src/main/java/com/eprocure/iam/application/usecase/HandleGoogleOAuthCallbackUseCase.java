package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.GoogleOAuthCallbackCommand;
import com.eprocure.iam.application.port.out.GoogleOAuthPort;
import com.eprocure.iam.application.port.out.GoogleOAuthProfile;
import com.eprocure.iam.application.service.CreatedSession;
import com.eprocure.iam.application.service.CreatedTwoFactorChallenge;
import com.eprocure.iam.application.service.LoginResult;
import com.eprocure.iam.application.service.OAuthStateService;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.application.service.TwoFactorChallengeService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HandleGoogleOAuthCallbackUseCase {
    private static final Logger log = LoggerFactory.getLogger(HandleGoogleOAuthCallbackUseCase.class);

    private final GoogleOAuthPort googleOAuthPort;
    private final OAuthStateService oauthStateService;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final TwoFactorChallengeService twoFactorChallengeService;

    public HandleGoogleOAuthCallbackUseCase(
            GoogleOAuthPort googleOAuthPort,
            OAuthStateService oauthStateService,
            UserRepository userRepository,
            SessionService sessionService,
            TwoFactorChallengeService twoFactorChallengeService) {
        this.googleOAuthPort = googleOAuthPort;
        this.oauthStateService = oauthStateService;
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.twoFactorChallengeService = twoFactorChallengeService;
    }

    @Transactional
    public LoginResult execute(GoogleOAuthCallbackCommand command) {
        log.info("[ACTION] Start GoogleOAuthCallback");
        if (!oauthStateService.consume(command.state(), command.stateCookie())) {
            throw new BusinessException(ErrorCode.IAM_012);
        }
        GoogleOAuthProfile profile = googleOAuthPort.fetchProfile(command.code())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_012));
        if (!profile.emailVerified()) {
            throw new BusinessException(ErrorCode.IAM_010);
        }

        User user = userRepository.findByGoogleOauthId(profile.subject())
                .or(() -> userRepository.findByUsernameOrEmail(profile.email())
                        .map(found -> linkGoogleSubject(found, profile.subject())))
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        if (user.isLocked() || !user.canLogin()) {
            throw new BusinessException(ErrorCode.IAM_002);
        }

        if (user.isTwoFactorEnabled()) {
            user.getTwoFactorSecretEncrypted().orElseThrow(() -> new BusinessException(ErrorCode.IAM_006));
            CreatedTwoFactorChallenge challenge = twoFactorChallengeService.createFor(user, command.clientContext());
            log.info("[ACTION] Complete GoogleOAuthTwoFactorRequired | userId={}", LogMaskingUtil.maskId(user.getId()));
            return new LoginResult(
                    user.getId(),
                    user.getFullName(),
                    user.getAvatarUrl().orElse(null),
                    true,
                    challenge.rawToken());
        }

        CreatedSession createdSession = sessionService.issueFor(user, command.clientContext());
        userRepository.updateLastLoginAt(user.getId(), Instant.now());
        log.info("[ACTION] Complete GoogleOAuthCallback | userId={}", LogMaskingUtil.maskId(user.getId()));
        return new LoginResult(
                user.getId(),
                user.getFullName(),
                user.getAvatarUrl().orElse(null),
                false,
                createdSession.rawToken());
    }

    private User linkGoogleSubject(User user, String googleSubject) {
        if (user.getGoogleOauthId().isEmpty()) {
            userRepository.linkGoogleOauthId(user.getId(), googleSubject, user.getId());
        }
        return user;
    }
}
