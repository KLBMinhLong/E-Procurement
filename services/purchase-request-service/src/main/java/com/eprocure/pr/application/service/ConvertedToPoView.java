package com.eprocure.pr.application.service;

import com.eprocure.pr.domain.model.PrStatus;
import java.util.UUID;

public record ConvertedToPoView(
        UUID id,
        String prNumber,
        PrStatus status,
        UUID poId,
        String poNumber) {
}
