package com.eprocure.vendor.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.vendor.application.port.in.CloseRfqCommand;
import com.eprocure.vendor.application.service.IdempotencyService;
import com.eprocure.vendor.domain.model.Rfq;
import com.eprocure.vendor.domain.model.RfqInvitation;
import com.eprocure.vendor.domain.model.RfqLineItem;
import com.eprocure.vendor.domain.model.RfqStatus;
import com.eprocure.vendor.domain.repository.RfqFilter;
import com.eprocure.vendor.domain.repository.RfqRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CloseRfqUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID RFQ_ID = UUID.fromString("81000000-0000-0000-0000-000000000001");
    private static final UUID IDEMPOTENCY_UUID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final String KEY = IDEMPOTENCY_UUID.toString();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final FakeRfqRepository rfqRepository = new FakeRfqRepository(sampleRfq());
    private final FakeIdempotencyService idempotencyService = new FakeIdempotencyService();
    private final CloseRfqUseCase useCase = new CloseRfqUseCase(rfqRepository, idempotencyService, CLOCK);

    @Test
    void should_close_rfq_when_published() {
        var result = useCase.execute(new CloseRfqCommand(ACTOR_ID, RFQ_ID), KEY);

        assertThat(rfqRepository.current.status()).isEqualTo(RfqStatus.CLOSED);
        assertThat(result.view().status()).isEqualTo(RfqStatus.CLOSED);
        assertThat(idempotencyService.savedOperation).isEqualTo("rfq-close");
    }

    private static Rfq sampleRfq() {
        return Rfq.create(
                RFQ_ID,
                "RFQ-2026-01-00001",
                UUID.fromString("50000000-0000-0000-0000-000000000001"),
                "PR-2026-00001",
                "Laptop RFQ",
                Instant.parse("2026-01-05T00:00:00Z"),
                List.of(RfqLineItem.create(
                        UUID.randomUUID(),
                        "Laptop Dell",
                        "IT_EQUIPMENT",
                        new BigDecimal("2.0000"),
                        "cai",
                        "16GB RAM")),
                List.of(
                        RfqInvitation.create(
                                UUID.fromString("80000000-0000-0000-0000-000000000101"),
                                "Vendor One",
                                Instant.parse("2026-01-01T00:00:00Z")),
                        RfqInvitation.create(
                                UUID.fromString("80000000-0000-0000-0000-000000000102"),
                                "Vendor Two",
                                Instant.parse("2026-01-01T00:00:00Z"))),
                null,
                ACTOR_ID,
                IDEMPOTENCY_UUID,
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
        private String savedOperation;

        private FakeIdempotencyService() {
            super(null, null);
        }

        @Override
        public void verify(String idempotencyKey) {
        }

        @Override
        public <T> Optional<T> find(String operation, UUID actorId, String idempotencyKey, Class<T> type) {
            return Optional.empty();
        }

        @Override
        public void save(String operation, UUID actorId, String idempotencyKey, Object response) {
            this.savedOperation = operation;
        }
    }

    private static final class FakeRfqRepository implements RfqRepository {
        private Rfq current;

        private FakeRfqRepository(Rfq current) {
            this.current = current;
        }

        @Override
        public String nextRfqNumber() {
            return "RFQ-2026-01-00001";
        }

        @Override
        public Optional<Rfq> findById(UUID id) {
            return Optional.ofNullable(current).filter(rfq -> rfq.id().equals(id));
        }

        @Override
        public Optional<Rfq> findByIdempotencyKey(UUID idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public List<Rfq> findByFilter(RfqFilter filter) {
            return List.of();
        }

        @Override
        public long countByFilter(RfqFilter filter) {
            return 0;
        }

        @Override
        public void save(Rfq rfq) {
            this.current = rfq;
        }

        @Override
        public void updateStatus(Rfq rfq) {
            this.current = rfq;
        }
    }
}
