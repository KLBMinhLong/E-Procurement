package com.eprocure.notification.infrastructure.email;

import com.eprocure.notification.application.usecase.DispatchPendingEmailsUseCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.notification.email.dispatch-enabled", havingValue = "true", matchIfMissing = true)
public class EmailDispatchScheduler {
    private static final Logger log = LogManager.getLogger(EmailDispatchScheduler.class);

    private final DispatchPendingEmailsUseCase dispatchPendingEmailsUseCase;

    public EmailDispatchScheduler(DispatchPendingEmailsUseCase dispatchPendingEmailsUseCase) {
        this.dispatchPendingEmailsUseCase = dispatchPendingEmailsUseCase;
    }

    @Scheduled(fixedDelayString = "${eprocure.notification.email.retry-fixed-delay-ms:60000}")
    public void dispatchPendingEmails() {
        int attempted = dispatchPendingEmailsUseCase.execute();
        if (attempted > 0) {
            log.info("[EMAIL] Dispatch batch attempted | count={}", attempted);
        }
    }
}
