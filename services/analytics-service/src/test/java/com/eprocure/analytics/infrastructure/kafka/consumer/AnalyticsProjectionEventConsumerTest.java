package com.eprocure.analytics.infrastructure.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.analytics.application.usecase.RecordAnalyticsProjectionUseCase;
import com.eprocure.analytics.domain.model.projection.AnalyticsEventMetadata;
import com.eprocure.analytics.domain.model.projection.ApprovalSlaBreachProjection;
import com.eprocure.analytics.domain.model.projection.ApprovalStepAssignedProjection;
import com.eprocure.analytics.domain.model.projection.GoodsReceiptCreatedProjection;
import com.eprocure.analytics.domain.model.projection.InvoiceMatchedProjection;
import com.eprocure.analytics.domain.model.projection.PoIssuedProjection;
import com.eprocure.analytics.domain.model.projection.PrSubmittedProjection;
import com.eprocure.analytics.domain.model.projection.RfqAwardedProjection;
import com.eprocure.analytics.domain.repository.AnalyticsProjectionRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class AnalyticsProjectionEventConsumerTest {
    private static final Instant NOW = Instant.parse("2026-06-05T04:00:00Z");

    @Test
    void should_route_rfq_awarded_event_to_projection_use_case() {
        FakeAnalyticsProjectionRepository repository = new FakeAnalyticsProjectionRepository();
        AnalyticsProjectionEventConsumer consumer = consumer(repository);

        consumer.consume(record("procurement.rfq.awarded", 1, 42, rfqAwardedEvent()));

        assertThat(repository.rfqAwardedProjection).isNotNull();
        assertThat(repository.rfqAwardedProjection.eventMetadata().eventId()).isEqualTo("evt-rfq-1");
        assertThat(repository.rfqAwardedProjection.eventMetadata().topic()).isEqualTo("procurement.rfq.awarded");
        assertThat(repository.rfqAwardedProjection.eventMetadata().partitionId()).isEqualTo(1);
        assertThat(repository.rfqAwardedProjection.eventMetadata().offsetValue()).isEqualTo(42);
        assertThat(repository.rfqAwardedProjection.rfqNumber()).isEqualTo("RFQ-2026-0001");
        assertThat(repository.rfqAwardedProjection.totalAmount()).isEqualByComparingTo("1200000.0000");
        assertThat(repository.rfqAwardedProjection.lineItems()).singleElement().satisfies(line -> {
            assertThat(line.categoryCode()).isEqualTo("IT_HARDWARE");
            assertThat(line.totalPrice()).isEqualByComparingTo("1200000.0000");
        });
        assertThat(repository.processedEvents).contains("evt-rfq-1");
        assertThat(repository.refreshes).hasSize(2);
    }

    @Test
    void should_route_goods_receipt_created_event_to_projection_use_case() {
        FakeAnalyticsProjectionRepository repository = new FakeAnalyticsProjectionRepository();
        AnalyticsProjectionEventConsumer consumer = consumer(repository);

        consumer.consume(record("inventory.gr.created", 2, 43, goodsReceiptCreatedEvent()));

        assertThat(repository.goodsReceiptCreatedProjection).isNotNull();
        assertThat(repository.goodsReceiptCreatedProjection.eventMetadata().eventId()).isEqualTo("evt-gr-1");
        assertThat(repository.goodsReceiptCreatedProjection.eventMetadata().topic()).isEqualTo("inventory.gr.created");
        assertThat(repository.goodsReceiptCreatedProjection.eventMetadata().partitionId()).isEqualTo(2);
        assertThat(repository.goodsReceiptCreatedProjection.eventMetadata().offsetValue()).isEqualTo(43);
        assertThat(repository.goodsReceiptCreatedProjection.grNumber()).isEqualTo("GR-2026-0001");
        assertThat(repository.goodsReceiptCreatedProjection.status()).isEqualTo("COMPLETE");
        assertThat(repository.goodsReceiptCreatedProjection.lineItems()).singleElement().satisfies(line -> {
            assertThat(line.itemCode()).isEqualTo("LAPTOP-001");
            assertThat(line.receivedQuantity()).isEqualByComparingTo("1.0000");
            assertThat(line.rejectedQuantity()).isEqualByComparingTo("0.0000");
        });
        assertThat(repository.processedEvents).contains("evt-gr-1");
        assertThat(repository.refreshes).hasSize(2);
    }

    private AnalyticsProjectionEventConsumer consumer(FakeAnalyticsProjectionRepository repository) {
        var mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .build();
        var useCase = new RecordAnalyticsProjectionUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        return new AnalyticsProjectionEventConsumer(useCase, mapper);
    }

    private ConsumerRecord<String, String> record(String topic, int partition, long offset, String value) {
        return new ConsumerRecord<>(topic, partition, offset, "key", value);
    }

    private String rfqAwardedEvent() {
        return """
                {
                  "eventId": "evt-rfq-1",
                  "eventType": "RFQ_AWARDED",
                  "version": "1.0",
                  "source": "vendor-service",
                  "timestamp": "2026-06-04T05:00:00Z",
                  "traceId": "00000000-0000-4000-8000-000000000001",
                  "payload": {
                    "rfqId": "80000000-0000-4000-8000-000000000001",
                    "rfqNumber": "RFQ-2026-0001",
                    "prId": "20000000-0000-4000-8000-000000000001",
                    "prNumber": "PR-2026-0001",
                    "vendorId": "30000000-0000-4000-8000-000000000001",
                    "vendorName": "Acme Supplier",
                    "totalAmount": 1200000.0000,
                    "currency": "VND",
                    "lineItems": [
                      {
                        "rfqLineItemId": "81000000-0000-4000-8000-000000000001",
                        "prLineItemId": "50000000-0000-4000-8000-000000000001",
                        "itemName": "Laptop",
                        "categoryCode": "IT_HARDWARE",
                        "quantity": 2.0000,
                        "unit": "EA",
                        "unitPrice": 600000.0000,
                        "totalPrice": 1200000.0000,
                        "currency": "VND"
                      }
                    ]
                  }
                }
                """;
    }

    private String goodsReceiptCreatedEvent() {
        return """
                {
                  "eventId": "evt-gr-1",
                  "eventType": "GR_CREATED",
                  "version": "1.0",
                  "source": "inventory-service",
                  "timestamp": "2026-06-04T06:10:00Z",
                  "traceId": "00000000-0000-4000-8000-000000000002",
                  "payload": {
                    "grId": "90000000-0000-4000-8000-000000000001",
                    "grNumber": "GR-2026-0001",
                    "poId": "10000000-0000-4000-8000-000000000001",
                    "poNumber": "PO-2026-0001",
                    "warehouseId": "91000000-0000-4000-8000-000000000001",
                    "warehouseKeeperId": "92000000-0000-4000-8000-000000000001",
                    "status": "COMPLETE",
                    "receivedAt": "2026-06-04T06:00:00Z",
                    "completedAt": "2026-06-04T06:10:00Z",
                    "lineItems": [
                      {
                        "grLineItemId": "93000000-0000-4000-8000-000000000001",
                        "poLineItemId": "40000000-0000-4000-8000-000000000001",
                        "itemCode": "LAPTOP-001",
                        "itemName": "Laptop",
                        "orderedQuantity": 2.0000,
                        "receivedQuantity": 1.0000,
                        "rejectedQuantity": 0.0000,
                        "unit": "EA"
                      }
                    ]
                  }
                }
                """;
    }

    private record RefreshRequest(UUID dashboardId, int fiscalYear, Integer quarter, Instant cachedAt) {
    }

    private static final class FakeAnalyticsProjectionRepository implements AnalyticsProjectionRepository {
        private final Set<String> processedEvents = new HashSet<>();
        private final List<RefreshRequest> refreshes = new ArrayList<>();
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
        }

        @Override
        public void upsertPoIssued(PoIssuedProjection projection) {
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
