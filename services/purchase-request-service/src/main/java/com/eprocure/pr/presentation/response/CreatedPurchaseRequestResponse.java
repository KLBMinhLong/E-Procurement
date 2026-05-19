package com.eprocure.pr.presentation.response;

import com.eprocure.pr.domain.model.PrStatus;
import java.util.UUID;

public record CreatedPurchaseRequestResponse(UUID id, String prNumber, PrStatus status) {
}
