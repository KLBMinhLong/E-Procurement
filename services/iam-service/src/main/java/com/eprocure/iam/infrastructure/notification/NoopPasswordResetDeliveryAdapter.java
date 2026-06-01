package com.eprocure.iam.infrastructure.notification;

import com.eprocure.iam.application.port.out.PasswordResetDeliveryPort;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "eprocure.iam.notification.password-reset.delivery-mode",
        havingValue = "logging",
        matchIfMissing = true)
public class NoopPasswordResetDeliveryAdapter implements PasswordResetDeliveryPort {
    private static final Logger log = LogManager.getLogger(NoopPasswordResetDeliveryAdapter.class);

    @Override
    public void sendResetInstructions(User user, String rawToken, Instant expiresAt) {
        log.info("[ACTION] PasswordResetDeliveryStub | userId={} | email={} | expiresAt={}",
                LogMaskingUtil.maskId(user.getId()),
                LogMaskingUtil.maskEmail(user.getEmail()),
                expiresAt);
    }
}
