package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.out.GoogleOAuthPort;
import com.eprocure.iam.application.service.GoogleOAuthRedirect;
import com.eprocure.iam.application.service.OAuthStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StartGoogleOAuthUseCase {
    private static final Logger log = LoggerFactory.getLogger(StartGoogleOAuthUseCase.class);

    private final GoogleOAuthPort googleOAuthPort;
    private final OAuthStateService oauthStateService;

    public StartGoogleOAuthUseCase(GoogleOAuthPort googleOAuthPort, OAuthStateService oauthStateService) {
        this.googleOAuthPort = googleOAuthPort;
        this.oauthStateService = oauthStateService;
    }

    public GoogleOAuthRedirect execute() {
        log.info("[ACTION] Start GoogleOAuthInit");
        String state = oauthStateService.create();
        log.info("[ACTION] Complete GoogleOAuthInit");
        return new GoogleOAuthRedirect(googleOAuthPort.authorizationUrl(state), state);
    }
}
