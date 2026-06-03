package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.InvoiceStatus;
import java.util.UUID;

public record InvoiceActionResult(
        UUID invoiceId,
        InvoiceStatus status,
        boolean replayed) {
}
