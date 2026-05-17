package com.eprocure.iam.application.service;

public record TwoFactorSetupView(String secret, String qrCodeUrl, String manualEntryKey) {
}
