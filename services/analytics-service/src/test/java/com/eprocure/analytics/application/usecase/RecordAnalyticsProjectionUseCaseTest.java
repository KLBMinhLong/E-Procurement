package com.eprocure.analytics.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.analytics.application.port.in.RecordGoodsReceiptCreatedProjectionCommand;
import com.eprocure.analytics.application.port.in.RecordPoIssuedProjectionCommand;
import com.eprocure.analytics.application.port.in.RecordPrSubmittedProjectionCommand;
import com.eprocure.analytics.application.port.in.RecordRfqAwardedProjectionCommand;
import com.eprocure.analytics.domain.model.projection.AnalyticsEventMetadata;
import com.eprocure.analytics.domain.model.projection.ApprovalSlaBreachProjection;
import com.eprocure.analytics.domain.model.projection.ApprovalStepAssignedProjection;
import com.eprocure.analytics.domain.model.projection.GoodsReceiptCreatedProjection;
import com.eprocure.analytics.domain.model.projection.InvoiceMatchedProjection;
import com.eprocure.analytics.domain.model.projection.PoIssuedProjection;
import com.eprocure.analytics.domain.model.projection.PrSubmittedProjection;
import com.eprocure.analytics.domain.model.projection.RfqAwardedProjection;
import com.eprocure.analytics.domain.repository.AnalyticsProjectionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordAnalyticsProjectionUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-04T04:00:00Z");

    @Test
    void should_store_pr_submitted_projection_without_refreshing_dashboard_snapshots() {
        FakeAnalyticsProjectionRepository repository = new FakeAnalyticsProjectionRepository();
        RecordAnalyticsProjectionUseCase useCase = new RecordAnalyticsProjectionUseCase(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC));

        useCase.recordPrSubmitted(prSubmittedCommand("evt-pr-1"));

        assertThat(repository.prSubmittedProjection).isNotNull();
        assertThat(repository.prSubmittedProjection.prNumber()).isEqualTo("PR-2026-0001");
        assertThat(repository.prSubmittedProjection.priority()).isEqualTo("NORMAL");
        assertThat(repository.processedEvents).contains("evt-pr-1");
        assertThat(repository.refreshes).isEmpty();
    }

    @Test
    void should_store_po_issued_projection_and_refresh_year_and_quarter_snapshots() {
        FakeAnalyticsProjectionRepository repository = new FakeAnalyticsProjectionRepository();
        RecordAnalyticsProjectionUseCase useCase = new RecordAnalyticsProjectionUseCase(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC));

        useCase.recordPoIssued(poIssuedCommand("evt-po-1"));

        assertThat(repository.poIssuedProjection).isNotNull();
        assertThat(repository.poIssuedProjection.poNumber()).isEqualTo("PO-2026-0001");
        assertThat(repository.processedEvents).contains("evt-po-1");
        assertThat(repository.refreshes)
                .extracting(RefreshRequest::fiscalYear)
                .containsExactly(2026, 2026);
        assertThat(repository.refreshes)
                .extracting(RefreshRequest::quarter)
                .containsExactly(null, 2);
    }

    @Test
    void should_skip_projection_when_event_already_processed() {
        FakeAnalyticsProjectionRepository repository = new FakeAnalyticsProjectionRepository();
        repository.processedEvents.add("evt-po-1");
        RecordAnalyticsProjectionUseCase useCase = new RecordAnalyticsProjectionUseCase(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC));

        useCase.recordPoIssued(poIssuedCommand("evt-po-1"));

        assertThat(repository.poIssuedProjection).isNull();
        assertThat(repository.refreshes).isEmpty();
    }

    @Test
    void should_store_rfq_awarded_projection_and_refresh_year_and_quarter_snapshots() {
        FakeAnalyticsProjectionRepository repository = new FakeAnalyticsProjectionRepository();
        RecordAnalyticsProjectionUseCase useCase = new RecordAnalyticsProjectionUseCase(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC));

        useCase.recordRfqAwarded(rfqAwardedCommand("evt-rfq-1"));

        assertThat(repository.rfqAwardedProjection).isNotNull();
        assertThat(repository.rfqAwardedProjection.rfqNumber()).isEqualTo("RFQ-2026-0001");
        assertThat(repository.rfqAwardedProjection.lineItems()).hasSize(1);
        assertThat(repository.processedEvents).contains("evt-rfq-1");
        assertThat(repository.refreshes)
                .extracting(RefreshRequest::quarter)
                .containsExactly(null, 2);
    }

    @Test
    void should_store_goods_receipt_created_projection_and_refresh_year_and_quarter_snapshots() {
        FakeAnalyticsProjectionRepository repository = new FakeAnalyticsProjectionRepository();
        RecordAnalyticsProjectionUseCase useCase = new RecordAnalyticsProjectionUseCase(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC));

        useCase.recordGoodsReceiptCreated(goodsReceiptCreatedCommand("evt-gr-1"));

        assertThat(repository.goodsReceiptCreatedProjection).isNotNull();
        assertThat(repository.goodsReceiptCreatedProjection.grNumber()).isEqualTo("GR-2026-0001");
        assertThat(repository.goodsReceiptCreatedProjection.lineItems()).singleElement()
                .satisfies(line -> assertThat(line.receivedQuantity()).isEqualByComparingTo("1.0000"));
        assertThat(repository.processedEvents).contains("evt-gr-1");
        assertThat(repository.refreshes)
                .extracting(RefreshRequest::quarter)
                .containsExactly(null, 2);
    }

    private static RecordPrSubmittedProjectionCommand prSubmittedCommand(String eventId) {
        return new RecordPrSubmittedProjectionCommand(
                eventId,
                "procurement.pr.submitted",
                0,
                9,
                Instant.parse("2026-06-04T02:00:00Z"),
                UUID.fromString("20000000-0000-4000-8000-000000000001"),
                "PR-2026-0001",
                UUID.fromString("60000000-0000-4000-8000-000000000001"),
                UUID.fromString("70000000-0000-4000-8000-000000000001"),
                "NORMAL",
                2026,
                new BigDecimal("1250000.0000"),
                "VND",
                Instant.parse("2026-06-04T02:00:00Z"));
    }

    private static RecordPoIssuedProjectionCommand poIssuedCommand(String eventId) {
        return new RecordPoIssuedProjectionCommand(
                eventId,
                "procurement.po.issued",
                0,
                10,
                NOW,
                UUID.fromString("10000000-0000-4000-8000-000000000001"),
                "PO-2026-0001",
                UUID.fromString("20000000-0000-4000-8000-000000000001"),
                "PR-2026-0001",
                UUID.fromString("30000000-0000-4000-8000-000000000001"),
                "Acme Supplier",
                new BigDecimal("1250000.0000"),
                "VND",
                Instant.parse("2026-06-04T03:00:00Z"),
                List.of(new RecordPoIssuedProjectionCommand.LineItem(
                        UUID.fromString("40000000-0000-4000-8000-000000000001"),
                        UUID.fromString("50000000-0000-4000-8000-000000000001"),
                        "Laptop",
                        "IT_HARDWARE",
                        new BigDecimal("2.0000"),
                        "EA",
                        new BigDecimal("625000.0000"),
                        new BigDecimal("1250000.0000"),
                        "VND")));
    }

    private static RecordRfqAwardedProjectionCommand rfqAwardedCommand(String eventId) {
        return new RecordRfqAwardedProjectionCommand(
                eventId,
                "procurement.rfq.awarded",
                0,
                11,
                NOW,
                UUID.fromString("80000000-0000-4000-8000-000000000001"),
                "RFQ-2026-0001",
                UUID.fromString("20000000-0000-4000-8000-000000000001"),
                "PR-2026-0001",
                UUID.fromString("30000000-0000-4000-8000-000000000001"),
                "Acme Supplier",
                new BigDecimal("1200000.0000"),
                "VND",
                Instant.parse("2026-06-04T05:00:00Z"),
                List.of(new RecordRfqAwardedProjectionCommand.LineItem(
                        UUID.fromString("81000000-0000-4000-8000-000000000001"),
                        UUID.fromString("50000000-0000-4000-8000-000000000001"),
                        "Laptop",
                        "IT_HARDWARE",
                        new BigDecimal("2.0000"),
                        "EA",
                        new BigDecimal("600000.0000"),
                        new BigDecimal("1200000.0000"),
                        "VND")));
    }

    private static RecordGoodsReceiptCreatedProjectionCommand goodsReceiptCreatedCommand(String eventId) {
        return new RecordGoodsReceiptCreatedProjectionCommand(
                eventId,
                "inventory.gr.created",
                0,
                12,
                NOW,
                UUID.fromString("90000000-0000-4000-8000-000000000001"),
                "GR-2026-0001",
                UUID.fromString("10000000-0000-4000-8000-000000000001"),
                "PO-2026-0001",
                UUID.fromString("91000000-0000-4000-8000-000000000001"),
                UUID.fromString("92000000-0000-4000-8000-000000000001"),
                "COMPLETE",
                Instant.parse("2026-06-04T06:00:00Z"),
                Instant.parse("2026-06-04T06:10:00Z"),
                List.of(new RecordGoodsReceiptCreatedProjectionCommand.LineItem(
                        UUID.fromString("93000000-0000-4000-8000-000000000001"),
                        UUID.fromString("40000000-0000-4000-8000-000000000001"),
                        "LAPTOP-001",
                        "Laptop",
                        new BigDecimal("2.0000"),
                        new BigDecimal("1.0000"),
                        BigDecimal.ZERO,
                        "EA")));
    }

    private record RefreshRequest(UUID dashboardId, int fiscalYear, Integer quarter, Instant cachedAt) {
    }

    private static final class FakeAnalyticsProjectionRepository implements AnalyticsProjectionRepository {
        private final Set<String> processedEvents = new HashSet<>();
        private final List<RefreshRequest> refreshes = new ArrayList<>();
        private PrSubmittedProjection prSubmittedProjection;
        private PoIssuedProjection poIssuedProjection;
        private RfqAwardedProjection rfqAwardedProjection;
        private GoodsReceiptCreatedProjection goodsReceiptCreatedProjection;

        @Override
        public boolean existsProcessedEvent(String eventId) {
            return processedEvents.contains(eventId);
        }

        @Override
        public void saveProcessedEvent(AnalyticsEventMetadata metadata, String handlerName) {
            processedEvents.add(metadata.eventId());
        }

        @Override
        public void upsertPrSubmitted(PrSubmittedProjection projection) {
            this.prSubmittedProjection = projection;
        }

        @Override
        public void upsertPoIssued(PoIssuedProjection projection) {
            this.poIssuedProjection = projection;
        }

        @Override
        public void upsertInvoiceMatched(InvoiceMatchedProjection projection) {
        }

        @Override
        public void upsertApprovalSlaBreach(ApprovalSlaBreachProjection projection) {
        }

        @Override
        public void upsertApprovalStepAssigned(ApprovalStepAssignedProjection projection) {
        }

        @Override
        public void upsertRfqAwarded(RfqAwardedProjection projection) {
            this.rfqAwardedProjection = projection;
        }

        @Override
        public void upsertGoodsReceiptCreated(GoodsReceiptCreatedProjection projection) {
            this.goodsReceiptCreatedProjection = projection;
        }

        @Override
        public void refreshExecutiveDashboard(UUID dashboardId, int fiscalYear, Integer quarter, Instant cachedAt) {
            refreshes.add(new RefreshRequest(dashboardId, fiscalYear, quarter, cachedAt));
        }
    }
}
