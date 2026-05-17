package com.eprocure.iam.application.port.out;

public interface CredentialVerificationPort {
    boolean verify(String username, String password);
}
