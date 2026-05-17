package com.eprocure.iam.application.port.in;

public record GoogleOAuthCallbackCommand(String code, String state, String stateCookie, ClientContext clientContext) {
}
