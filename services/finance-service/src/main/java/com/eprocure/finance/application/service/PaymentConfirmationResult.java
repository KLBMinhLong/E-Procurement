package com.eprocure.finance.application.service;

public record PaymentConfirmationResult(PaymentView payment, boolean replayed) {
}
