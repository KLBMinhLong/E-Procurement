package com.eprocure.pr.application.usecase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.pr.application.port.in.CancelPurchaseRequestCommand;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.repository.PurchaseRequestFilter;
import com.eprocure.pr.domain.repository.PurchaseRequestRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CancelPurchaseRequestUseCaseTest {

    private static final UUID REQUESTER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("99999999-9999-4999-8999-999999999999");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-42d3-a456-426614174000";

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-20T00:00:00Z"), ZoneOffset.UTC);
    private InMemoryPurchaseRequestRepository purchaseRequestRepository;
    private FakeIdempotencyService idempotencyService;
    private CancelPurchaseRequestUseCase useCase;

    @BeforeEach
    void setUp() {
        purchaseRequestRepository = new InMemoryPurchaseRequestRepository();
        idempotencyService = new FakeIdempotencyService();
        useCase = new CancelPurchaseRequestUseCase(purchaseRequestRepository, idempotencyService, clock);
    }

    @Test
    void should_cancel_pr_when_valid_command() {
        PurchaseRequest pr = buildDraftPr(REQUESTER_ID);
        purchaseRequestRepository.stored = pr;

        assertThatCode(() -> useCase.execute(
                new CancelPurchaseRequestCommand(pr.getId(), REQUESTER_ID, "Không còn nhu cầu nữa"),
                IDEMPOTENCY_KEY))
                .doesNotThrowAnyException();

        org.assertj.core.api.Assertions.assertThat(purchaseRequestRepository.softDeletedId).isEqualTo(pr.getId());
        org.assertj.core.api.Assertions.assertThat(idempotencyService.savedResponse).isEqualTo("CANCELLED");
    }

    // remove ambiguous assertThat shadow

    @Test
    void should_return_early_on_idempotency_replay() {
        idempotencyService.cached = "CANCELLED";

        assertThatCode(() -> useCase.execute(
                new CancelPurchaseRequestCommand(UUID.randomUUID(), REQUESTER_ID, "reason"),
                IDEMPOTENCY_KEY))
                .doesNotThrowAnyException();

        // No DB access
        org.assertj.core.api.Assertions.assertThat(purchaseRequestRepository.findCalls).isZero();
        org.assertj.core.api.Assertions.assertThat(purchaseRequestRepository.softDeletedId).isNull();
    }

    @Test
    void should_throw_PR_001_when_pr_not_found() {
        assertThatThrownBy(() -> useCase.execute(
                new CancelPurchaseRequestCommand(UUID.randomUUID(), REQUESTER_ID, "reason"),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PR_001);
    }

    @Test
    void should_throw_IAM_004_when_actor_is_not_requester() {
        PurchaseRequest pr = buildDraftPr(REQUESTER_ID);
        purchaseRequestRepository.stored = pr;

        assertThatThrownBy(() -> useCase.execute(
                new CancelPurchaseRequestCommand(pr.getId(), OTHER_USER_ID, "reason"),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_004);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private PurchaseRequest buildDraftPr(UUID requesterId) {
        return PurchaseRequest.create(
                "PR-2026-05-00001", requesterId, DEPARTMENT_ID,
                "Test PR Title",
                "This justification is long enough to satisfy the fifty character minimum validation rule.",
                PrPriority.NORMAL, null, 2026,
                LocalDate.of(2026, 12, 31), null, false,
                List.of(com.eprocure.pr.domain.model.PrLineItem.create(
                        null, "Item A", null, "IT_HARDWARE",
                        new com.eprocure.pr.domain.model.vo.Quantity(BigDecimal.ONE, "cái"),
                        new com.eprocure.pr.domain.model.vo.Money(new BigDecimal("1000000"), "VND"),
                        null, null, "6002", false)),
                Instant.now(clock));
    }

    // ── In-memory test doubles ────────────────────────────────────────────────

    private static final class InMemoryPurchaseRequestRepository implements PurchaseRequestRepository {
        private PurchaseRequest stored;
        private UUID softDeletedId;
        private int findCalls;

        @Override public void save(PurchaseRequest pr) { this.stored = pr; }
        @Override public void update(PurchaseRequest pr) { this.stored = pr; }
        @Override public void updateWithLineItems(PurchaseRequest pr) { this.stored = pr; }

        @Override
        public Optional<PurchaseRequest> findById(UUID id) {
            findCalls++;
            return Optional.ofNullable(stored).filter(pr -> pr.getId().equals(id));
        }

        @Override
        public Optional<PurchaseRequest> findByPrNumber(String prNumber) {
            return Optional.ofNullable(stored).filter(pr -> pr.getPrNumber().equals(prNumber));
        }

        @Override
        public boolean existsByPrNumber(String prNumber) { return findByPrNumber(prNumber).isPresent(); }

        @Override
        public void softDelete(UUID id, UUID deletedBy, Instant deletedAt) {
            this.softDeletedId = id;
        }

        @Override
        public List<PurchaseRequest> findByFilter(PurchaseRequestFilter filter) { return List.of(); }

        @Override
        public long countByFilter(PurchaseRequestFilter filter) { return 0L; }
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
        private String cached;
        private Object savedResponse;

        private FakeIdempotencyService() {
            super(null, null);
        }

        @Override
        public void verify(String idempotencyKey) {}

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> find(String operation, UUID actorId, String idempotencyKey, Class<T> type) {
            return Optional.ofNullable(cached).map(c -> (T) c);
        }

        @Override
        public void save(String operation, UUID actorId, String idempotencyKey, Object response) {
            this.savedResponse = response;
        }
    }
}
