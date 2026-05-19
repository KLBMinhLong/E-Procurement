package com.eprocure.pr.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.pr.application.port.in.CreatePurchaseRequestCommand;
import com.eprocure.pr.application.service.CreatedPurchaseRequestView;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.application.service.PurchaseRequestNumberGenerator;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.PurchaseRequest;
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

class CreatePurchaseRequestUseCaseTest {
    private static final UUID REQUESTER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-42d3-a456-426614174000";
    private static final String JUSTIFICATION =
            "May tinh cu da hong va can thay the de dam bao tien do du an quan trong.";

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-19T01:00:00Z"), ZoneOffset.UTC);
    private InMemoryPurchaseRequestRepository purchaseRequestRepository;
    private FixedPurchaseRequestNumberGenerator numberGenerator;
    private FakeIdempotencyService idempotencyService;
    private CreatePurchaseRequestUseCase useCase;

    @BeforeEach
    void setUp() {
        purchaseRequestRepository = new InMemoryPurchaseRequestRepository();
        numberGenerator = new FixedPurchaseRequestNumberGenerator(clock);
        idempotencyService = new FakeIdempotencyService();
        useCase = new CreatePurchaseRequestUseCase(
                purchaseRequestRepository,
                numberGenerator,
                idempotencyService,
                clock);
    }

    @Test
    void should_create_pr_when_valid_command() {
        var result = useCase.execute(validCommand(), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.view().prNumber()).isEqualTo("PR-2026-05-00001");
        assertThat(result.view().status()).isEqualTo(PrStatus.DRAFT);
        assertThat(purchaseRequestRepository.saved.getTotalAmount().amount()).isEqualByComparingTo("70000000.0000");
        assertThat(idempotencyService.savedResponse).isEqualTo(result.view());
    }

    @Test
    void should_return_cached_result_when_idempotency_hit() {
        CreatedPurchaseRequestView cached = new CreatedPurchaseRequestView(
                UUID.fromString("33333333-3333-4333-8333-333333333333"),
                "PR-2026-05-00001",
                PrStatus.DRAFT);
        idempotencyService.cached = cached;

        var result = useCase.execute(validCommand(), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isTrue();
        assertThat(result.view()).isEqualTo(cached);
        assertThat(numberGenerator.calls).isZero();
        assertThat(purchaseRequestRepository.saved).isNull();
    }

    @Test
    void should_throw_pr_012_when_line_items_empty() {
        CreatePurchaseRequestCommand command = new CreatePurchaseRequestCommand(
                REQUESTER_ID,
                DEPARTMENT_ID,
                "Mua laptop Dell XPS 15",
                JUSTIFICATION,
                PrPriority.NORMAL,
                null,
                LocalDate.parse("2026-06-01"),
                null,
                false,
                List.of(),
                List.of());

        assertThatThrownBy(() -> useCase.execute(command, IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PR_012);
    }

    private CreatePurchaseRequestCommand validCommand() {
        return new CreatePurchaseRequestCommand(
                REQUESTER_ID,
                DEPARTMENT_ID,
                "Mua laptop Dell XPS 15 cho team phat trien",
                JUSTIFICATION,
                PrPriority.NORMAL,
                null,
                LocalDate.parse("2026-06-01"),
                null,
                false,
                List.of(new CreatePurchaseRequestCommand.LineItemCommand(
                        null,
                        "Laptop Dell XPS 15",
                        "Thong so theo de xuat cua phong ban",
                        "IT_HARDWARE",
                        new BigDecimal("2.00"),
                        "cai",
                        new BigDecimal("35000000.0000"),
                        "VND",
                        null,
                        "Core i7, 16GB RAM, 512GB SSD",
                        "6002",
                        false)),
                List.of());
    }

    private static final class InMemoryPurchaseRequestRepository implements PurchaseRequestRepository {
        private PurchaseRequest saved;

        @Override
        public void save(PurchaseRequest purchaseRequest) {
            this.saved = purchaseRequest;
        }

        @Override
        public void update(PurchaseRequest purchaseRequest) {
            this.saved = purchaseRequest;
        }

        @Override
        public Optional<PurchaseRequest> findById(UUID id) {
            return Optional.ofNullable(saved).filter(value -> value.getId().equals(id));
        }

        @Override
        public Optional<PurchaseRequest> findByPrNumber(String prNumber) {
            return Optional.ofNullable(saved).filter(value -> value.getPrNumber().equals(prNumber));
        }

        @Override
        public boolean existsByPrNumber(String prNumber) {
            return findByPrNumber(prNumber).isPresent();
        }

        @Override
        public void softDelete(UUID id, UUID deletedBy, Instant deletedAt) {
        }
    }

    private static final class FixedPurchaseRequestNumberGenerator extends PurchaseRequestNumberGenerator {
        private int calls;

        private FixedPurchaseRequestNumberGenerator(Clock clock) {
            super(() -> 1L, clock, "PR");
        }

        @Override
        public String next() {
            calls++;
            return super.next();
        }
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
        private CreatedPurchaseRequestView cached;
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
