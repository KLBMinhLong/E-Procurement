package com.eprocure.analytics.domain.repository;

import com.eprocure.analytics.domain.model.projection.AnalyticsEventMetadata;
import com.eprocure.analytics.domain.model.projection.ApprovalSlaBreachProjection;
import com.eprocure.analytics.domain.model.projection.InvoiceMatchedProjection;
import com.eprocure.analytics.domain.model.projection.PoIssuedProjection;
import java.time.Instant;
import java.util.UUID;

public interface AnalyticsProjectionRepository {
    boolean existsProcessedEvent(String eventId);

    void saveProcessedEvent(AnalyticsEventMetadata metadata, String handlerName);

    void upsertPoIssued(PoIssuedProjection projection);

    void upsertInvoiceMatched(InvoiceMatchedProjection projection);

    void upsertApprovalSlaBreach(ApprovalSlaBreachProjection projection);

    void refreshExecutiveDashboard(UUID dashboardId, int fiscalYear, Integer quarter, Instant cachedAt);
}
