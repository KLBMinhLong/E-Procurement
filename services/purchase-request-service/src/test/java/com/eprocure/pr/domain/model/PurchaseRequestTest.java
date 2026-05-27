package com.eprocure.pr.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.pr.domain.event.PrCancelledEvent;
import com.eprocure.pr.domain.event.PrSubmittedEvent;
import com.eprocure.pr.domain.model.vo.BudgetCheckResult;
import com.eprocure.pr.domain.model.vo.InventoryCheckResult;
import com.eprocure.pr.domain.model.vo.Money;
import com.eprocure.pr.domain.model.vo.Quantity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurchaseRequestTest {
    private static final UUID REQUESTER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID DEPARTMENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final String JUSTIFICATION =
            "May tinh cu da hong va can thay the de dam bao tien do du an quan trong.";

    @Test
    void should_create_draft_pr_and_calculate_total() {
        PurchaseRequest purchaseRequest = createDefaultPr(List.of(
                lineItem("Laptop Dell XPS 15", "2", "35000000"),
                lineItem("USB-C dock", "3", "1000000.5000")));

        assertThat(purchaseRequest.getStatus()).isEqualTo(PrStatus.DRAFT);
        assertThat(purchaseRequest.getTotalAmount().amount()).isEqualByComparingTo("73000001.5000");
        assertThat(purchaseRequest.getLineItems()).hasSize(2);
        assertThat(purchaseRequest.getLineItems().get(0).getLineNumber()).isEqualTo(1);
        assertThat(purchaseRequest.getLineItems().get(1).getLineNumber()).isEqualTo(2);
    }

    @Test
    void should_throw_when_line_items_empty() {
        assertThatThrownBy(() -> createDefaultPr(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line item");
    }

    @Test
    void should_throw_when_emergency_reason_is_missing() {
        assertThatThrownBy(() -> PurchaseRequest.create(
                        "PR-2026-05-00002",
                        REQUESTER_ID,
                        DEPARTMENT_ID,
                        "Mua may chu khan cap",
                        JUSTIFICATION,
                        PrPriority.EMERGENCY,
                        "Can gap",
                        2026,
                        LocalDate.parse("2026-06-01"),
                        null,
                        false,
                        List.of(lineItem("Server", "1", "120000000")),
                        Instant.parse("2026-05-19T01:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("urgencyReason");
    }

    @Test
    void should_submit_draft_pr_and_record_event() {
        PurchaseRequest purchaseRequest = createDefaultPr(List.of(lineItem("Laptop Dell XPS 15", "2", "35000000")));
        Instant submittedAt = Instant.parse("2026-05-19T02:00:00Z");

        purchaseRequest.submit(REQUESTER_ID, submittedAt, budgetPass(purchaseRequest), InventoryCheckResult.empty());

        assertThat(purchaseRequest.getStatus()).isEqualTo(PrStatus.SUBMITTED);
        assertThat(purchaseRequest.getSubmittedAt()).contains(submittedAt);
        assertThat(purchaseRequest.getBudgetCheck()).contains(budgetPass(purchaseRequest));
        assertThat(purchaseRequest.getInventoryCheck()).contains(InventoryCheckResult.empty());
        assertThat(purchaseRequest.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(PrSubmittedEvent.class)
                .satisfies(event -> {
                    PrSubmittedEvent submittedEvent = (PrSubmittedEvent) event;
                    assertThat(submittedEvent.payload().title()).isEqualTo("Mua laptop Dell XPS 15 cho team phat trien");
                    assertThat(submittedEvent.payload().categories()).containsExactly("IT_HARDWARE");
                });
    }

    @Test
    void should_throw_when_non_owner_updates_draft() {
        PurchaseRequest purchaseRequest = createDefaultPr(List.of(lineItem("Laptop Dell XPS 15", "2", "35000000")));

        assertThatThrownBy(() -> purchaseRequest.updateDraft(
                        OTHER_USER_ID,
                        "Mua laptop cho team backend",
                        JUSTIFICATION,
                        PrPriority.NORMAL,
                        null,
                        LocalDate.parse("2026-06-01"),
                        null,
                        false,
                        List.of(lineItem("Laptop Dell XPS 15", "1", "35000000")),
                        Instant.parse("2026-05-19T02:00:00Z")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("edited");
    }

    @Test
    void should_cancel_submitted_pr_by_requester() {
        PurchaseRequest purchaseRequest = createDefaultPr(List.of(lineItem("Laptop Dell XPS 15", "2", "35000000")));
        purchaseRequest.submit(
                REQUESTER_ID,
                Instant.parse("2026-05-19T02:00:00Z"),
                budgetPass(purchaseRequest),
                InventoryCheckResult.empty());
        purchaseRequest.pullDomainEvents();

        purchaseRequest.cancel(
                REQUESTER_ID,
                "Khong con nhu cau mua sam",
                Instant.parse("2026-05-19T03:00:00Z"));

        assertThat(purchaseRequest.getStatus()).isEqualTo(PrStatus.CANCELLED);
        assertThat(purchaseRequest.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(PrCancelledEvent.class);
    }

    private static PurchaseRequest createDefaultPr(List<PrLineItem> lineItems) {
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
                lineItems,
                Instant.parse("2026-05-19T01:00:00Z"));
    }

    private static BudgetCheckResult budgetPass(PurchaseRequest purchaseRequest) {
        return BudgetCheckResult.pass(purchaseRequest.getTotalAmount());
    }

    private static PrLineItem lineItem(String itemName, String quantity, String unitPrice) {
        return PrLineItem.create(
                null,
                itemName,
                "Thong so theo de xuat cua phong ban",
                "IT_HARDWARE",
                new Quantity(new BigDecimal(quantity), "cai"),
                new Money(new BigDecimal(unitPrice), "VND"),
                null,
                "Core i7, 16GB RAM, 512GB SSD",
                "6002",
                false);
    }
}
