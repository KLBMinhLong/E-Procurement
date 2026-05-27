package com.eprocure.pr.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.pr.application.port.in.SubmitPurchaseRequestCommand;
import com.eprocure.pr.application.port.out.BudgetCheckPort;
import com.eprocure.pr.application.port.out.InventoryCheckPort;
import com.eprocure.pr.application.port.out.PrSubmittedEventPublisher;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.application.service.SubmittedPurchaseRequestView;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.domain.event.PrSubmittedEvent;
import com.eprocure.pr.domain.model.PrLineItem;
import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.model.vo.BudgetCheckResult;
import com.eprocure.pr.domain.model.vo.BudgetCheckStatus;
import com.eprocure.pr.domain.model.vo.InventoryCheckResult;
import com.eprocure.pr.domain.model.vo.Money;
import com.eprocure.pr.domain.model.vo.Quantity;
import com.eprocure.pr.domain.repository.PurchaseRequestRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmitPurchaseRequestUseCaseTest {
    private static final UUID REQUESTER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID DEPARTMENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-42d3-a456-426614174000";
    private static final String JUSTIFICATION =
            "May tinh cu da hong va can thay the de dam bao tien do du an quan trong.";

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-19T02:00:00Z"), ZoneOffset.UTC);
    private InMemoryPurchaseRequestRepository purchaseRequestRepository;
    private FakeBudgetCheckPort budgetCheckPort;
    private FakeInventoryCheckPort inventoryCheckPort;
    private FakePrSubmittedEventPublisher eventPublisher;
    private FakeIdempotencyService idempotencyService;
    private SubmitPurchaseRequestUseCase useCase;

    @BeforeEach
    void setUp() {
        purchaseRequestRepository = new InMemoryPurchaseRequestRepository();
        budgetCheckPort = new FakeBudgetCheckPort();
        inventoryCheckPort = new FakeInventoryCheckPort();
        eventPublisher = new FakePrSubmittedEventPublisher();
        idempotencyService = new FakeIdempotencyService();
        useCase = new SubmitPurchaseRequestUseCase(
                purchaseRequestRepository,
                budgetCheckPort,
                inventoryCheckPort,
                eventPublisher,
                idempotencyService,
                clock);
    }

    @Test
    void should_submit_pr_when_budget_passes() {
        PurchaseRequest purchaseRequest = createDefaultPr();
        purchaseRequestRepository.stored = purchaseRequest;
        budgetCheckPort.result = BudgetCheckResult.pass(purchaseRequest.getTotalAmount());

        var result = useCase.execute(command(purchaseRequest.getId(), REQUESTER_ID), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.view().status()).isEqualTo(PrStatus.SUBMITTED);
        assertThat(purchaseRequestRepository.updated).isSameAs(purchaseRequest);
        assertThat(purchaseRequest.getSubmittedAt()).contains(clock.instant());
        assertThat(purchaseRequest.getBudgetCheck()).contains(budgetCheckPort.result);
        assertThat(purchaseRequest.getInventoryCheck()).contains(InventoryCheckResult.empty());
        assertThat(eventPublisher.events).hasSize(1);
        assertThat(eventPublisher.events.get(0).payload().title()).isEqualTo("Mua laptop Dell XPS 15 cho team phat trien");
        assertThat(eventPublisher.events.get(0).payload().categories()).containsExactly("IT_HARDWARE");
        assertThat(idempotencyService.savedResponse).isEqualTo(result.view());
    }

    @Test
    void should_return_cached_result_when_idempotency_hit() {
        SubmittedPurchaseRequestView cached = new SubmittedPurchaseRequestView(
                UUID.fromString("44444444-4444-4444-8444-444444444444"),
                "PR-2026-05-00001",
                PrStatus.SUBMITTED);
        idempotencyService.cached = cached;

        var result = useCase.execute(command(cached.id(), REQUESTER_ID), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isTrue();
        assertThat(result.view()).isEqualTo(cached);
        assertThat(purchaseRequestRepository.findCalls).isZero();
        assertThat(eventPublisher.events).isEmpty();
    }

    @Test
    void should_throw_pr_002_when_budget_fails() {
        PurchaseRequest purchaseRequest = createDefaultPr();
        purchaseRequestRepository.stored = purchaseRequest;
        budgetCheckPort.result = budgetFail();

        assertThatThrownBy(() -> useCase.execute(command(purchaseRequest.getId(), REQUESTER_ID), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PR_002);
        assertThat(purchaseRequest.getStatus()).isEqualTo(PrStatus.DRAFT);
        assertThat(purchaseRequestRepository.updated).isNull();
        assertThat(eventPublisher.events).isEmpty();
    }

    @Test
    void should_throw_iam_004_when_actor_is_not_requester() {
        PurchaseRequest purchaseRequest = createDefaultPr();
        purchaseRequestRepository.stored = purchaseRequest;

        assertThatThrownBy(() -> useCase.execute(command(purchaseRequest.getId(), OTHER_USER_ID), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_004);
        assertThat(budgetCheckPort.calls).isZero();
    }

    private SubmitPurchaseRequestCommand command(UUID purchaseRequestId, UUID actorId) {
        return new SubmitPurchaseRequestCommand(purchaseRequestId, actorId);
    }

    private static PurchaseRequest createDefaultPr() {
        return PurchaseRequest.create(
                "PR-2026-05-00001",
                REQUESTER_ID,
                DEPARTMENT_ID,
                "Mua laptop Dell XPS 15 cho team phat trien",
                JUSTIFICATION,
                PrPriority.NORMAL,
                null,
                2026,
                LocalDate.parse("2026-06-01"),
                null,
                false,
                List.of(lineItem()),
                Instant.parse("2026-05-19T01:00:00Z"));
    }

    private static PrLineItem lineItem() {
        return PrLineItem.create(
                null,
                "Laptop Dell XPS 15",
                "Thong so theo de xuat cua phong ban",
                "IT_HARDWARE",
                new Quantity(new BigDecimal("2.00"), "cai"),
                new Money(new BigDecimal("35000000.0000"), "VND"),
                null,
                "Core i7, 16GB RAM, 512GB SSD",
                "6002",
                false);
    }

    private static BudgetCheckResult budgetFail() {
        Money zero = Money.zero("VND");
        return new BudgetCheckResult(
                new Money(new BigDecimal("10000000.0000"), "VND"),
                zero,
                new Money(new BigDecimal("10000000.0000"), "VND"),
                zero,
                BudgetCheckStatus.FAIL,
                "Department budget is insufficient");
    }

    private static final class InMemoryPurchaseRequestRepository implements PurchaseRequestRepository {
        private PurchaseRequest stored;
        private PurchaseRequest updated;
        private int findCalls;

        @Override
        public void save(PurchaseRequest purchaseRequest) {
            this.stored = purchaseRequest;
        }

        @Override
        public void update(PurchaseRequest purchaseRequest) {
            this.updated = purchaseRequest;
            this.stored = purchaseRequest;
        }

        @Override
        public Optional<PurchaseRequest> findById(UUID id) {
            findCalls++;
            return Optional.ofNullable(stored).filter(value -> value.getId().equals(id));
        }

        @Override
        public Optional<PurchaseRequest> findByPrNumber(String prNumber) {
            return Optional.ofNullable(stored).filter(value -> value.getPrNumber().equals(prNumber));
        }

        @Override
        public boolean existsByPrNumber(String prNumber) {
            return findByPrNumber(prNumber).isPresent();
        }

        @Override
        public void softDelete(UUID id, UUID deletedBy, Instant deletedAt) {
        }

        @Override
        public void updateWithLineItems(PurchaseRequest purchaseRequest) {
            this.updated = purchaseRequest;
            this.stored = purchaseRequest;
        }

        @Override
        public java.util.List<PurchaseRequest> findByFilter(com.eprocure.pr.domain.repository.PurchaseRequestFilter filter) {
            return java.util.List.of();
        }

        @Override
        public long countByFilter(com.eprocure.pr.domain.repository.PurchaseRequestFilter filter) {
            return 0L;
        }
    }

    private static final class FakeBudgetCheckPort implements BudgetCheckPort {
        private BudgetCheckResult result;
        private int calls;

        @Override
        public BudgetCheckResult check(BudgetCheckQuery query) {
            calls++;
            return result;
        }
    }

    private static final class FakeInventoryCheckPort implements InventoryCheckPort {
        @Override
        public InventoryCheckResult check(InventoryCheckQuery query) {
            return InventoryCheckResult.empty();
        }
    }

    private static final class FakePrSubmittedEventPublisher implements PrSubmittedEventPublisher {
        private final List<PrSubmittedEvent> events = new ArrayList<>();

        @Override
        public void publish(PrSubmittedEvent event) {
            events.add(event);
        }
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
        private SubmittedPurchaseRequestView cached;
        private Object savedResponse;

        private FakeIdempotencyService() {
            super(null, null);
        }

        @Override
        public void verify(String idempotencyKey) {
        }

        @Override
        public <T> Optional<T> find(String operation, UUID actorId, String idempotencyKey, Class<T> type) {
            return Optional.ofNullable(cached).map(type::cast);
        }

        @Override
        public void save(String operation, UUID actorId, String idempotencyKey, Object response) {
            this.savedResponse = response;
        }
    }
}
