package com.eprocure.pr.application.port.in;

import com.eprocure.pr.domain.model.PrPriority;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePurchaseRequestCommand(
        UUID requesterId,
        UUID departmentId,
        String title,
        String justification,
        PrPriority priority,
        String urgencyReason,
        LocalDate needByDate,
        UUID relatedContractId,
        boolean blanketRelease,
        List<LineItemCommand> lineItems,
        List<UUID> attachmentIds) {

    public CreatePurchaseRequestCommand {
        lineItems = lineItems == null ? List.of() : List.copyOf(lineItems);
        attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
    }

    public record LineItemCommand(
            String itemCode,
            String itemName,
            String description,
            String categoryCode,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            String currency,
            UUID preferredVendorId,
            String specifications,
            String glAccountCode,
            boolean fromCatalog) {
    }
}
