package com.eprocure.iam.application.service;

public record GoogleOAuthRedirect(String authorizationUrl, String state) {
}
