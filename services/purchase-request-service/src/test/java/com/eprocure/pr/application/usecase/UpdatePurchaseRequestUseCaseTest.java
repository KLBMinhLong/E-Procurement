package com.eprocure.pr.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.pr.application.port.in.UpdatePurchaseRequestCommand;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.application.service.UpdatedPurchaseRequestView;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PrStatus;
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

class UpdatePurchaseRequestUseCaseTest {

    private static final UUID REQUESTER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("99999999-9999-4999-8999-999999999999");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-42d3-a456-426614174000";
    private static final String JUSTIFICATION =
            "This justification is long enough to satisfy the fifty character minimum validation rule here.";

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-20T00:00:00Z"), ZoneOffset.UTC);
    private InMemoryPurchaseRequestRepository purchaseRequestRepository;
    private FakeIdempotencyService idempotencyService;
    private UpdatePurchaseRequestUseCase useCase;

    @BeforeEach
    void setUp() {
        purchaseRequestRepository = new InMemoryPurchaseRequestRepository();
        idempotencyService = new FakeIdempotencyService();
        useCase = new UpdatePurchaseRequestUseCase(purchaseRequestRepository, idempotencyService, clock);
    }

    @Test
    void should_update_pr_when_valid_command() {
        PurchaseRequest pr = buildDraftPr(REQUESTER_ID);
        purchaseRequestRepository.stored = pr;

        UpdatedPurchaseRequestView result = useCase.execute(buildCommand(REQUESTER_ID, pr.getId(), LocalDate.of(2026, 12, 31)), IDEMPOTENCY_KEY);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(PrStatus.DRAFT);
        assertThat(result.title()).isEqualTo("Updated Title For Test PR Record");
        assertThat(purchaseRequestRepository.updatedWithLineItems).isSameAs(pr);
        assertThat(idempotencyService.savedResponse).isEqualTo(result);
    }

    @Test
    void should_return_cached_when_idempotency_hit() {
        UUID prId = UUID.randomUUID();
        UpdatedPurchaseRequestView cached = new UpdatedPurchaseRequestView(
                prId, "PR-2026-05-00001", PrStatus.DRAFT,
                "cached title", JUSTIFICATION, PrPriority.NORMAL,
                null, null, null, 2026, null);
        idempotencyService.cached = cached;

        UpdatedPurchaseRequestView result = useCase.execute(buildCommand(REQUESTER_ID, prId, null), IDEMPOTENCY_KEY);

        assertThat(result.prNumber()).isEqualTo("PR-2026-05-00001");
        assertThat(purchaseRequestRepository.findCalls).isZero();
    }

    @Test
    void should_throw_PR_001_when_pr_not_found() {
        // Nothing stored in repository

        assertThatThrownBy(() -> useCase.execute(buildCommand(REQUESTER_ID, UUID.randomUUID(), LocalDate.of(2026, 12, 31)), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PR_001);
    }

    @Test
    void should_throw_IAM_004_when_actor_is_not_requester() {
        PurchaseRequest pr = buildDraftPr(REQUESTER_ID);
        purchaseRequestRepository.stored = pr;

        assertThatThrownBy(() -> useCase.execute(buildCommand(OTHER_USER_ID, pr.getId(), null), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_004);
    }

    @Test
    void should_throw_PR_013_when_needByDate_is_in_past() {
        PurchaseRequest pr = buildDraftPr(REQUESTER_ID);
        purchaseRequestRepository.stored = pr;

        assertThatThrownBy(() -> useCase.execute(buildCommand(REQUESTER_ID, pr.getId(), LocalDate.of(2020, 1, 1)), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PR_013);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private PurchaseRequest buildDraftPr(UUID requesterId) {
        return PurchaseRequest.create(
                "PR-2026-05-00001", requesterId, DEPARTMENT_ID,
                "Original Title", JUSTIFICATION,
                PrPriority.NORMAL, null, 2026,
                LocalDate.of(2026, 12, 31), null, false,
                List.of(com.eprocure.pr.domain.model.PrLineItem.create(
                        null, "Item A", null, "IT_HARDWARE",
                        new com.eprocure.pr.domain.model.vo.Quantity(BigDecimal.ONE, "cái"),
                        new com.eprocure.pr.domain.model.vo.Money(new BigDecimal("1000000"), "VND"),
                        null, null, "6002", false)),
                Instant.now(clock));
    }

    private UpdatePurchaseRequestCommand buildCommand(UUID actorId, UUID prId, LocalDate needByDate) {
        return new UpdatePurchaseRequestCommand(
                prId, actorId,
                "Updated Title For Test PR Record",
                JUSTIFICATION,
                "NORMAL", null,
                needByDate,
                List.of(new UpdatePurchaseRequestCommand.LineItemCommand(
                        null, "Laptop Dell", null, "IT_HARDWARE",
                        BigDecimal.ONE, "cái", new BigDecimal("35000000"), "VND",
                        null, null, "6002", false)));
    }

    // ── In-memory test doubles ────────────────────────────────────────────────

    private static final class InMemoryPurchaseRequestRepository implements PurchaseRequestRepository {
        private PurchaseRequest stored;
        private PurchaseRequest updatedWithLineItems;
        private int findCalls;

        @Override
        public void save(PurchaseRequest pr) { this.stored = pr; }

        @Override
        public void update(PurchaseRequest pr) { this.stored = pr; }

        @Override
        public void updateWithLineItems(PurchaseRequest pr) {
            this.updatedWithLineItems = pr;
            this.stored = pr;
        }

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
        public boolean existsByPrNumber(String prNumber) {
            return findByPrNumber(prNumber).isPresent();
        }

        @Override
        public void softDelete(UUID id, UUID deletedBy, Instant deletedAt) {}

        @Override
        public List<PurchaseRequest> findByFilter(PurchaseRequestFilter filter) { return List.of(); }

        @Override
        public long countByFilter(PurchaseRequestFilter filter) { return 0L; }
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
        private UpdatedPurchaseRequestView cached;
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
