package com.eprocure.notification.application.port.out;

public class EmailDispatchException extends RuntimeException {
    public EmailDispatchException(String message) {
        super(message);
    }

    public EmailDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
