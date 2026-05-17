package com.eprocure.iam.application.port.out;

import java.util.Optional;

public interface GoogleOAuthPort {
    String authorizationUrl(String state);

    Optional<GoogleOAuthProfile> fetchProfile(String authorizationCode);
}
