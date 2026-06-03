package com.eprocure.inventory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.inventory.application.port.in.RecordIssuedPurchaseOrderCommand;
import com.eprocure.inventory.domain.model.PurchaseOrderSnapshot;
import com.eprocure.inventory.domain.repository.PurchaseOrderSnapshotRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecordIssuedPurchaseOrderUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-06-03T03:00:00Z");
    private static final UUID PO_ID = UUID.fromString("96000000-0000-4000-8000-000000000001");
    private static final UUID PR_ID = UUID.fromString("88000000-0000-4000-8000-000000000001");
    private static final UUID VENDOR_ID = UUID.fromString("92000000-0000-4000-8000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID PO_LINE_ITEM_ID = UUID.fromString("97000000-0000-4000-8000-000000000001");
    private static final UUID PR_LINE_ITEM_ID = UUID.fromString("95000000-0000-4000-8000-000000000001");

    private FakePurchaseOrderSnapshotRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FakePurchaseOrderSnapshotRepository();
    }

    @Test
    void should_record_po_snapshot_when_issued_event_arrives() {
        var useCase = new RecordIssuedPurchaseOrderUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));

        Optional<PurchaseOrderSnapshot> result = useCase.execute(command("evt-po-issued-001"));

        assertThat(result).isPresent();
        assertThat(result.get().poId()).isEqualTo(PO_ID);
        assertThat(result.get().poNumber()).isEqualTo("PO-2026-000001");
        assertThat(result.get().lineItems()).hasSize(1);
        assertThat(result.get().lineItems().get(0).poLineItemId()).isEqualTo(PO_LINE_ITEM_ID);
        assertThat(repository.snapshots).hasSize(1);
        assertThat(repository.processedEvents).contains("evt-po-issued-001");
    }

    @Test
    void should_skip_recording_when_issued_event_is_replayed() {
        var useCase = new RecordIssuedPurchaseOrderUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        useCase.execute(command("evt-po-issued-001"));

        Optional<PurchaseOrderSnapshot> replayed = useCase.execute(command("evt-po-issued-001"));

        assertThat(replayed).isPresent();
        assertThat(repository.snapshots).hasSize(1);
        assertThat(repository.markProcessedCalls).isEqualTo(1);
    }

    @Test
    void should_mark_event_processed_when_snapshot_already_exists_for_po() {
        var useCase = new RecordIssuedPurchaseOrderUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        useCase.execute(command("evt-po-issued-001"));

        Optional<PurchaseOrderSnapshot> result = useCase.execute(command("evt-po-issued-002"));

        assertThat(result).isPresent();
        assertThat(repository.snapshots).hasSize(1);
        assertThat(repository.processedEvents).contains("evt-po-issued-002");
        assertThat(repository.markProcessedCalls).isEqualTo(2);
    }

    private static RecordIssuedPurchaseOrderCommand command(String eventId) {
        return new RecordIssuedPurchaseOrderCommand(
                eventId,
                "procurement.po.issued",
                0,
                42L,
                NOW,
                PO_ID,
                "PO-2026-000001",
                PR_ID,
                "PR-2026-000001",
                VENDOR_ID,
                "Acme Supplier",
                "sales@acme.example",
                "ACME-TAX",
                ACTOR_ID,
                new BigDecimal("25000000.0000"),
                "VND",
                "Floor 10, eProcure Tower",
                LocalDate.parse("2026-06-30"),
                "NET30",
                NOW,
                NOW,
                List.of(new RecordIssuedPurchaseOrderCommand.LineItem(
                        PO_LINE_ITEM_ID,
                        PR_LINE_ITEM_ID,
                        "Laptop",
                        "IT",
                        new BigDecimal("10.0000"),
                        "PCS",
                        new BigDecimal("2500000.0000"),
                        new BigDecimal("25000000.0000"),
                        "VND")));
    }

    private static final class FakePurchaseOrderSnapshotRepository implements PurchaseOrderSnapshotRepository {
        private final List<PurchaseOrderSnapshot> snapshots = new ArrayList<>();
        private final Set<String> processedEvents = new HashSet<>();
        private int markProcessedCalls;

        @Override
        public Optional<PurchaseOrderSnapshot> findByPoId(UUID poId) {
            return snapshots.stream()
                    .filter(snapshot -> snapshot.poId().equals(poId))
                    .findFirst();
        }

        @Override
        public Optional<PurchaseOrderSnapshot> findBySourceEventId(String sourceEventId) {
            return snapshots.stream()
                    .filter(snapshot -> snapshot.sourceEventId().equals(sourceEventId))
                    .findFirst();
        }

        @Override
        public boolean existsProcessedEvent(String eventId) {
            return processedEvents.contains(eventId);
        }

        @Override
        public void insert(PurchaseOrderSnapshot purchaseOrderSnapshot) {
            snapshots.add(purchaseOrderSnapshot);
        }

        @Override
        public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
            processedEvents.add(eventId);
            markProcessedCalls++;
        }
    }
}
