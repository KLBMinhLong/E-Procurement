package com.eprocure.iam.application.service;

import java.util.List;

public record TwoFactorConfirmView(List<String> backupCodes) {
    public TwoFactorConfirmView {
        backupCodes = List.copyOf(backupCodes);
    }
}
