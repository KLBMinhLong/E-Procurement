package com.eprocure.notification.infrastructure.email;

import com.eprocure.notification.application.port.out.EmailSendCommand;
import com.eprocure.notification.application.port.out.EmailSendResult;
import com.eprocure.notification.application.port.out.EmailSenderPort;
import com.eprocure.notification.common.util.LogMaskingUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.notification.email.provider", havingValue = "logging", matchIfMissing = true)
public class LoggingEmailSenderAdapter implements EmailSenderPort {
    private static final Logger log = LogManager.getLogger(LoggingEmailSenderAdapter.class);

    @Override
    public EmailSendResult send(EmailSendCommand command) {
        log.info("[EMAIL] Dispatch simulated | notificationId={} | to={}",
                LogMaskingUtil.maskId(command.notificationId()),
                LogMaskingUtil.maskEmail(command.to()));
        return new EmailSendResult("logging-" + command.notificationId());
    }
}
