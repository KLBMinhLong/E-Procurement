package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.in.RecordApprovalSlaBreachedProjectionCommand;
import com.eprocure.analytics.application.port.in.RecordInvoiceMatchedProjectionCommand;
import com.eprocure.analytics.application.port.in.RecordPoIssuedProjectionCommand;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.domain.model.projection.AnalyticsEventMetadata;
import com.eprocure.analytics.domain.model.projection.ApprovalSlaBreachProjection;
import com.eprocure.analytics.domain.model.projection.InvoiceMatchedProjection;
import com.eprocure.analytics.domain.model.projection.PoIssuedProjection;
import com.eprocure.analytics.domain.repository.AnalyticsProjectionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordAnalyticsProjectionUseCase {
    private static final Logger log = LogManager.getLogger(RecordAnalyticsProjectionUseCase.class);

    private final AnalyticsProjectionRepository repository;
    private final Clock clock;

    public RecordAnalyticsProjectionUseCase(AnalyticsProjectionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void recordPoIssued(RecordPoIssuedProjectionCommand command) {
        PoIssuedProjection projection = command.toProjection();
        if (alreadyProcessed(projection.eventMetadata())) {
            return;
        }
        repository.upsertPoIssued(projection);
        repository.saveProcessedEvent(projection.eventMetadata(), "PO_ISSUED");
        refreshFor(projection.issuedAt());
        log.info("[ACTION] Complete RecordPoIssuedProjection | eventId={} | poId={}",
                projection.eventMetadata().eventId(),
                LogMaskingUtil.maskId(projection.poId()));
    }

    @Transactional
    public void recordInvoiceMatched(RecordInvoiceMatchedProjectionCommand command) {
        InvoiceMatchedProjection projection = command.toProjection();
        if (alreadyProcessed(projection.eventMetadata())) {
            return;
        }
        repository.upsertInvoiceMatched(projection);
        repository.saveProcessedEvent(projection.eventMetadata(), "INVOICE_MATCHED");
        refreshFor(projection.matchedAt());
        log.info("[ACTION] Complete RecordInvoiceMatchedProjection | eventId={} | invoiceId={}",
                projection.eventMetadata().eventId(),
                LogMaskingUtil.maskId(projection.invoiceId()));
    }

    @Transactional
    public void recordApprovalSlaBreached(RecordApprovalSlaBreachedProjectionCommand command) {
        ApprovalSlaBreachProjection projection = command.toProjection();
        if (alreadyProcessed(projection.eventMetadata())) {
            return;
        }
        repository.upsertApprovalSlaBreach(projection);
        repository.saveProcessedEvent(projection.eventMetadata(), "APPROVAL_SLA_BREACHED");
        refreshFor(projection.breachedAt());
        log.info("[ACTION] Complete RecordApprovalSlaBreachedProjection | eventId={} | approvalStepId={}",
                projection.eventMetadata().eventId(),
                LogMaskingUtil.maskId(projection.approvalStepId()));
    }

    private boolean alreadyProcessed(AnalyticsEventMetadata metadata) {
        if (!repository.existsProcessedEvent(metadata.eventId())) {
            return false;
        }
        log.info("[ACTION] Skip processed analytics event | eventId={} | topic={}",
                metadata.eventId(),
                metadata.topic());
        return true;
    }

    private void refreshFor(Instant eventInstant) {
        ZonedDateTime dateTime = eventInstant.atZone(ZoneOffset.UTC);
        int fiscalYear = dateTime.getYear();
        int quarter = ((dateTime.getMonthValue() - 1) / 3) + 1;
        Instant cachedAt = Instant.now(clock);
        repository.refreshExecutiveDashboard(UUID.randomUUID(), fiscalYear, null, cachedAt);
        repository.refreshExecutiveDashboard(UUID.randomUUID(), fiscalYear, quarter, cachedAt);
    }
}
