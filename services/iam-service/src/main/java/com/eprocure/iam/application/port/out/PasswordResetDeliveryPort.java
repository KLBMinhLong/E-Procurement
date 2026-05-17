package com.eprocure.iam.application.port.out;

import com.eprocure.iam.domain.model.User;
import java.time.Instant;

public interface PasswordResetDeliveryPort {
    void sendResetInstructions(User user, String rawToken, Instant expiresAt);
}
