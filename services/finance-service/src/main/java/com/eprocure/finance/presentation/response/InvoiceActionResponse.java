package com.eprocure.finance.presentation.response;

import com.eprocure.finance.domain.model.InvoiceStatus;
import java.util.UUID;

public record InvoiceActionResponse(UUID invoiceId, InvoiceStatus status) {
}
