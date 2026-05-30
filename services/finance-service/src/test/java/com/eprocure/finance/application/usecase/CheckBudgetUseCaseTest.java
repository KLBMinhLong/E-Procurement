package com.eprocure.finance.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.finance.application.port.in.CheckBudgetCommand;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.domain.model.BudgetCheckCriteria;
import com.eprocure.finance.domain.model.BudgetCommitmentHold;
import com.eprocure.finance.domain.model.BudgetCheckStatus;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.model.BudgetOverrideApproval;
import com.eprocure.finance.domain.model.BudgetStatus;
import com.eprocure.finance.domain.model.BudgetTransaction;
import com.eprocure.finance.domain.model.BudgetTransactionType;
import com.eprocure.finance.domain.model.BudgetTransfer;
import com.eprocure.finance.domain.repository.BudgetFilter;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.domain.repository.BudgetRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckBudgetUseCaseTest {
    private static final UUID BUDGET_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");

    private FakeBudgetRepository budgetRepository;
    private CheckBudgetUseCase useCase;

    @BeforeEach
    void setUp() {
        budgetRepository = new FakeBudgetRepository();
        useCase = new CheckBudgetUseCase(budgetRepository);
    }

    @Test
    void should_return_pass_when_budget_is_available() {
        budgetRepository.summary = summary("500000000.0000", "100000000.0000", "50000000.0000");

        var result = useCase.execute(command("70000000.0000"));

        assertThat(result.status()).isEqualTo(BudgetCheckStatus.PASS);
        assertThat(result.available().amount()).isEqualByComparingTo("350000000.0000");
        assertThat(result.warningMessage()).isNull();
        assertThat(budgetRepository.criteria.departmentId()).isEqualTo(DEPARTMENT_ID);
    }

    @Test
    void should_return_warning_when_available_after_request_is_below_threshold() {
        budgetRepository.summary = summary("500000000.0000", "320000000.0000", "50000000.0000");

        var result = useCase.execute(command("50000000.0000"));

        assertThat(result.status()).isEqualTo(BudgetCheckStatus.WARNING);
        assertThat(result.warningMessage()).contains("below 20%");
    }

    @Test
    void should_return_fail_when_request_exceeds_available_budget() {
        budgetRepository.summary = summary("100000000.0000", "70000000.0000", "20000000.0000");

        var result = useCase.execute(command("20000000.0000"));

        assertThat(result.status()).isEqualTo(BudgetCheckStatus.FAIL);
        assertThat(result.warningMessage()).contains("insufficient");
    }

    @Test
    void should_throw_fin_001_when_active_budget_not_found() {
        budgetRepository.summary = null;

        assertThatThrownBy(() -> useCase.execute(command("10000000.0000")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FIN_001);
    }

    @Test
    void should_throw_fin_002_when_amount_is_not_positive() {
        assertThatThrownBy(() -> useCase.execute(command("0.0000")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FIN_002);
    }

    private CheckBudgetCommand command(String amount) {
        return new CheckBudgetCommand(
                DEPARTMENT_ID,
                2026,
                "6002",
                new Money(new BigDecimal(amount), "VND"));
    }

    private static BudgetLedgerSummary summary(String allocated, String committed, String spent) {
        return new BudgetLedgerSummary(
                BUDGET_ID,
                DEPARTMENT_ID,
                2026,
                null,
                "6002",
                new Money(new BigDecimal(allocated), "VND"),
                new Money(new BigDecimal(committed), "VND"),
                new Money(new BigDecimal(spent), "VND"),
                BudgetStatus.ACTIVE);
    }

    private static final class FakeBudgetRepository implements BudgetRepository {
        private BudgetLedgerSummary summary;
        private BudgetCheckCriteria criteria;

        @Override
        public Optional<BudgetLedgerSummary> findActiveSummary(BudgetCheckCriteria criteria) {
            this.criteria = criteria;
            return Optional.ofNullable(summary);
        }

        @Override
        public List<BudgetLedgerSummary> findByFilter(BudgetFilter filter) {
            return List.of();
        }

        @Override
        public long countByFilter(BudgetFilter filter) {
            return 0;
        }

        @Override
        public Optional<BudgetLedgerSummary> findSummaryById(UUID budgetId) {
            return Optional.empty();
        }

        @Override
        public boolean lockBudgetForUpdate(UUID budgetId) {
            return true;
        }

        @Override
        public boolean existsProcessedEvent(String eventId) {
            return false;
        }

        @Override
        public void markEventProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
        }

        @Override
        public boolean existsTransaction(UUID budgetId, BudgetTransactionType transactionType, String referenceType, UUID referenceId) {
            return false;
        }

        @Override
        public void insertTransaction(BudgetTransaction transaction) {
        }

        @Override
        public Optional<BudgetCommitmentHold> findHeldCommitment(String referenceType, UUID referenceId) {
            return Optional.empty();
        }

        @Override
        public Optional<BudgetOverrideApproval> findOverrideApprovalByIdempotencyKey(UUID idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public void insertOverrideApproval(BudgetOverrideApproval approval) {
        }

        @Override
        public Optional<BudgetTransfer> findTransferByIdempotencyKey(UUID idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public void insertTransfer(BudgetTransfer transfer) {
        }

        @Override
        public void adjustAllocatedAmount(UUID budgetId, Money delta, UUID actorId) {
        }
    }
}
