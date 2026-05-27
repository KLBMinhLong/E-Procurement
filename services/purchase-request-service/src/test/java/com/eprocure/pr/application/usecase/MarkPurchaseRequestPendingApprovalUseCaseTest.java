package com.eprocure.pr.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.pr.application.port.in.MarkPurchaseRequestPendingApprovalCommand;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.application.service.MarkedPendingApprovalView;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.domain.model.PrLineItem;
import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.model.vo.BudgetCheckResult;
import com.eprocure.pr.domain.model.vo.InventoryCheckResult;
import com.eprocure.pr.domain.model.vo.Money;
import com.eprocure.pr.domain.model.vo.Quantity;
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

class MarkPurchaseRequestPendingApprovalUseCaseTest {
    private static final UUID REQUESTER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID DEPARTMENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID APPROVAL_PROCESS_ID = UUID.fromString("123e4567-e89b-42d3-a456-426614174000");
    private static final String CAMUNDA_PROCESS_INSTANCE_ID = "camunda-approval-001";
    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-42d3-a456-426614174000";
    private static final String JUSTIFICATION =
            "May tinh cu da hong va can thay the de dam bao tien do du an quan trong.";

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-20T02:00:00Z"), ZoneOffset.UTC);
    private InMemoryPurchaseRequestRepository purchaseRequestRepository;
    private FakeIdempotencyService idempotencyService;
    private MarkPurchaseRequestPendingApprovalUseCase useCase;

    @BeforeEach
    void setUp() {
        purchaseRequestRepository = new InMemoryPurchaseRequestRepository();
        idempotencyService = new FakeIdempotencyService();
        useCase = new MarkPurchaseRequestPendingApprovalUseCase(
                purchaseRequestRepository,
                idempotencyService,
                clock);
    }

    @Test
    void should_mark_pr_pending_approval_when_pr_is_submitted() {
        PurchaseRequest purchaseRequest = submittedPr();
        purchaseRequestRepository.stored = purchaseRequest;

        MarkedPendingApprovalView result = useCase.execute(command(purchaseRequest.getId()), IDEMPOTENCY_KEY);

        assertThat(result.status()).isEqualTo(PrStatus.PENDING_APPROVAL);
        assertThat(result.id()).isEqualTo(purchaseRequest.getId());
        assertThat(result.approvalProcessId()).isEqualTo(APPROVAL_PROCESS_ID);
        assertThat(result.camundaProcessInstanceId()).isEqualTo(CAMUNDA_PROCESS_INSTANCE_ID);
        assertThat(purchaseRequestRepository.updated).isSameAs(purchaseRequest);
        assertThat(purchaseRequest.getUpdatedAt()).contains(clock.instant());
        assertThat(idempotencyService.savedResponse).isEqualTo(result);
    }

    @Test
    void should_return_cached_result_when_idempotency_hit() {
        MarkedPendingApprovalView cached = new MarkedPendingApprovalView(
                UUID.fromString("44444444-4444-4444-8444-444444444444"),
                "PR-2026-05-00002",
                PrStatus.PENDING_APPROVAL,
                APPROVAL_PROCESS_ID,
                CAMUNDA_PROCESS_INSTANCE_ID);
        idempotencyService.cached = cached;

        MarkedPendingApprovalView result = useCase.execute(command(cached.id()), IDEMPOTENCY_KEY);

        assertThat(result).isEqualTo(cached);
        assertThat(purchaseRequestRepository.findCalls).isZero();
        assertThat(purchaseRequestRepository.updated).isNull();
    }

    @Test
    void should_skip_update_when_pr_is_already_pending_approval() {
        PurchaseRequest purchaseRequest = submittedPr();
        purchaseRequest.markPendingApproval(clock.instant());
        purchaseRequestRepository.stored = purchaseRequest;

        MarkedPendingApprovalView result = useCase.execute(command(purchaseRequest.getId()), IDEMPOTENCY_KEY);

        assertThat(result.status()).isEqualTo(PrStatus.PENDING_APPROVAL);
        assertThat(purchaseRequestRepository.updated).isNull();
        assertThat(idempotencyService.savedResponse).isEqualTo(result);
    }

    @Test
    void should_throw_pr_003_when_pr_status_is_not_submitted_or_pending_approval() {
        PurchaseRequest purchaseRequest = submittedPr();
        purchaseRequest.markPendingApproval(clock.instant());
        purchaseRequest.approve(clock.instant());
        purchaseRequestRepository.stored = purchaseRequest;

        assertThatThrownBy(() -> useCase.execute(command(purchaseRequest.getId()), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PR_003);
        assertThat(purchaseRequestRepository.updated).isNull();
        assertThat(idempotencyService.savedResponse).isNull();
    }

    private MarkPurchaseRequestPendingApprovalCommand command(UUID purchaseRequestId) {
        return new MarkPurchaseRequestPendingApprovalCommand(
                purchaseRequestId,
                APPROVAL_PROCESS_ID,
                CAMUNDA_PROCESS_INSTANCE_ID);
    }

    private static PurchaseRequest submittedPr() {
        PurchaseRequest purchaseRequest = PurchaseRequest.create(
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
        purchaseRequest.submit(
                REQUESTER_ID,
                Instant.parse("2026-05-19T02:00:00Z"),
                BudgetCheckResult.pass(purchaseRequest.getTotalAmount()),
                InventoryCheckResult.empty());
        return purchaseRequest;
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
        public void updateWithLineItems(PurchaseRequest purchaseRequest) {
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
        public List<PurchaseRequest> findByFilter(PurchaseRequestFilter filter) {
            return List.of();
        }

        @Override
        public long countByFilter(PurchaseRequestFilter filter) {
            return 0L;
        }
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
        private MarkedPendingApprovalView cached;
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
