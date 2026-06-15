package com.eprocure.admin.application.port.out;

import java.util.UUID;

public interface IamSecurityConfirmationPort {
    void verifyTotp(UUID userId, String confirmationCode);
}
