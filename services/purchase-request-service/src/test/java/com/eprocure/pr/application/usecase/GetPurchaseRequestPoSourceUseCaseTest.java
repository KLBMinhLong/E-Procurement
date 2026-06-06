package com.eprocure.pr.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.pr.application.service.PoSourceView;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetPurchaseRequestPoSourceUseCaseTest {
    private static final UUID REQUESTER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID DEPARTMENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID PREFERRED_VENDOR_ID = UUID.fromString("88888888-8888-4888-8888-888888888888");
    private static final String JUSTIFICATION =
            "May tinh cu da hong va can thay the de dam bao tien do du an quan trong.";

    private final InMemoryPurchaseRequestRepository purchaseRequestRepository = new InMemoryPurchaseRequestRepository();
    private final GetPurchaseRequestUseCase useCase = new GetPurchaseRequestUseCase(purchaseRequestRepository);

    @Test
    void should_return_po_source_when_pr_is_approved() {
        PurchaseRequest purchaseRequest = approvedPr();
        purchaseRequestRepository.stored = purchaseRequest;

        PoSourceView result = useCase.getPoSource(purchaseRequest.getId());

        assertThat(result.id()).isEqualTo(purchaseRequest.getId());
        assertThat(result.status()).isEqualTo(PrStatus.APPROVED);
        assertThat(result.requesterId()).isEqualTo(REQUESTER_ID);
        assertThat(result.departmentId()).isEqualTo(DEPARTMENT_ID);
        assertThat(result.needByDate()).isEqualTo(LocalDate.parse("2026-06-01"));
        assertThat(result.totalAmount().amount()).isEqualByComparingTo("70000000.0000");
        assertThat(result.lineItems()).hasSize(1);
        assertThat(result.lineItems().get(0).id()).isEqualTo(purchaseRequest.getLineItems().get(0).getId());
        assertThat(result.lineItems().get(0).lineNumber()).isEqualTo(1);
        assertThat(result.lineItems().get(0).itemCode()).isEqualTo("IT-LAPTOP-001");
        assertThat(result.lineItems().get(0).preferredVendorId()).isEqualTo(PREFERRED_VENDOR_ID);
        assertThat(result.lineItems().get(0).unitPrice().amount()).isEqualByComparingTo("35000000.0000");
    }

    @Test
    void should_throw_pr_003_when_pr_is_not_approved() {
        PurchaseRequest purchaseRequest = submittedPr();
        purchaseRequestRepository.stored = purchaseRequest;

        assertThatThrownBy(() -> useCase.getPoSource(purchaseRequest.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PR_003);
    }

    private static PurchaseRequest approvedPr() {
        PurchaseRequest purchaseRequest = submittedPr();
        purchaseRequest.markPendingApproval(Instant.parse("2026-05-19T03:00:00Z"));
        purchaseRequest.approve(Instant.parse("2026-05-19T04:00:00Z"));
        return purchaseRequest;
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
                "IT-LAPTOP-001",
                "Laptop Dell XPS 15",
                "Thong so theo de xuat cua phong ban",
                "IT_HARDWARE",
                new Quantity(new BigDecimal("2.00"), "cai"),
                new Money(new BigDecimal("35000000.0000"), "VND"),
                PREFERRED_VENDOR_ID,
                "Core i7, 16GB RAM, 512GB SSD",
                "6002",
                true);
    }

    private static final class InMemoryPurchaseRequestRepository implements PurchaseRequestRepository {
        private PurchaseRequest stored;

        @Override public void save(PurchaseRequest purchaseRequest) { this.stored = purchaseRequest; }
        @Override public void update(PurchaseRequest purchaseRequest) { this.stored = purchaseRequest; }
        @Override public void updateWithLineItems(PurchaseRequest purchaseRequest) { this.stored = purchaseRequest; }

        @Override
        public Optional<PurchaseRequest> findById(UUID id) {
            return Optional.ofNullable(stored).filter(value -> value.getId().equals(id));
        }

        @Override public Optional<PurchaseRequest> findByPrNumber(String prNumber) { return Optional.empty(); }
        @Override public boolean existsByPrNumber(String prNumber) { return false; }
        @Override public void softDelete(UUID id, UUID deletedBy, Instant deletedAt) { }
        @Override public List<PurchaseRequest> findByFilter(PurchaseRequestFilter filter) { return List.of(); }
        @Override public long countByFilter(PurchaseRequestFilter filter) { return 0; }
    }
}
