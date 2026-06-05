package com.eprocure.analytics.infrastructure.persistence.repository;

import com.eprocure.analytics.domain.model.projection.AnalyticsEventMetadata;
import com.eprocure.analytics.domain.model.projection.ApprovalSlaBreachProjection;
import com.eprocure.analytics.domain.model.projection.InvoiceMatchedProjection;
import com.eprocure.analytics.domain.model.projection.PoIssuedLineProjection;
import com.eprocure.analytics.domain.model.projection.PoIssuedProjection;
import com.eprocure.analytics.domain.model.projection.PrSubmittedProjection;
import com.eprocure.analytics.domain.repository.AnalyticsProjectionRepository;
import com.eprocure.analytics.infrastructure.persistence.mapper.AnalyticsProjectionMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsProjectionRepositoryImpl implements AnalyticsProjectionRepository {
    private final AnalyticsProjectionMapper mapper;

    public AnalyticsProjectionRepositoryImpl(AnalyticsProjectionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean existsProcessedEvent(String eventId) {
        return mapper.existsProcessedEvent(eventId);
    }

    @Override
    public void saveProcessedEvent(AnalyticsEventMetadata metadata, String handlerName) {
        mapper.insertProcessedEvent(
                metadata.eventId(),
                metadata.topic(),
                metadata.partitionId(),
                metadata.offsetValue(),
                handlerName,
                metadata.eventTimestamp());
    }

    @Override
    public void upsertPrSubmitted(PrSubmittedProjection projection) {
        mapper.upsertPrSubmitted(
                projection.purchaseRequestId(),
                projection.prNumber(),
                projection.requesterId(),
                projection.departmentId(),
                projection.priority(),
                projection.fiscalYear(),
                projection.totalAmount(),
                projection.currency(),
                projection.submittedAt(),
                projection.eventMetadata().eventId(),
                projection.eventMetadata().eventTimestamp());
    }

    @Override
    public void upsertPoIssued(PoIssuedProjection projection) {
        mapper.upsertPoIssued(
                projection.poId(),
                projection.poNumber(),
                projection.prId(),
                projection.prNumber(),
                projection.vendorId(),
                projection.vendorName(),
                projection.totalAmount(),
                projection.currency(),
                projection.issuedAt(),
                projection.eventMetadata().eventId(),
                projection.eventMetadata().eventTimestamp());
        for (PoIssuedLineProjection line : projection.lineItems()) {
            mapper.upsertPoIssuedLine(
                    line.poLineItemId(),
                    projection.poId(),
                    line.prLineItemId(),
                    line.itemName(),
                    line.categoryCode(),
                    line.quantity(),
                    line.unit(),
                    line.unitPrice(),
                    line.totalPrice(),
                    line.currency());
        }
    }

    @Override
    public void upsertInvoiceMatched(InvoiceMatchedProjection projection) {
        mapper.upsertInvoiceMatched(
                projection.invoiceId(),
                projection.invoiceNumber(),
                projection.poId(),
                projection.poNumber(),
                projection.vendorId(),
                projection.vendorName(),
                projection.totalAmount(),
                projection.currency(),
                projection.dueDate(),
                projection.matchedAt(),
                projection.eventMetadata().eventId(),
                projection.eventMetadata().eventTimestamp());
    }

    @Override
    public void upsertApprovalSlaBreach(ApprovalSlaBreachProjection projection) {
        mapper.upsertApprovalSlaBreach(
                projection.approvalStepId(),
                projection.processId(),
                projection.purchaseRequestId(),
                projection.prNumber(),
                projection.priority(),
                projection.stepIndex(),
                projection.stepType(),
                projection.approverRole(),
                projection.breachedApproverId(),
                projection.escalatedToApproverId(),
                projection.reassigned(),
                projection.assignedAt(),
                projection.slaDeadline(),
                projection.breachedAt(),
                projection.eventMetadata().eventId(),
                projection.eventMetadata().eventTimestamp());
    }

    @Override
    public void refreshExecutiveDashboard(UUID dashboardId, int fiscalYear, Integer quarter, Instant cachedAt) {
        mapper.insertExecutiveDashboardSnapshot(dashboardId, fiscalYear, quarter, cachedAt);
        mapper.insertCategorySpendSnapshots(dashboardId, fiscalYear, quarter);
        mapper.insertMonthlySpendSnapshots(dashboardId, fiscalYear, quarter);
        mapper.insertTopVendorSnapshots(dashboardId, fiscalYear, quarter);
    }
}
