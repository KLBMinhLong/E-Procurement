package com.eprocure.notification.application.port.out;

public interface EmailSenderPort {
    EmailSendResult send(EmailSendCommand command);
}
