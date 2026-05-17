package com.eprocure.iam.application.port.out;

public interface CredentialResetPort {
    void resetPassword(String keycloakUsername, String newPassword);
}
