package com.eprocure.approval.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.approval.domain.model.vo.Money;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApprovalRuleTest {

    private static final UUID DEPARTMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Test
    @DisplayName("Rule value dùng min inclusive và max exclusive")
    void should_match_value_rule_when_amount_is_inside_inclusive_min_exclusive_max_range() {
        // Given
        ApprovalRule rule = ApprovalRule.restore(
                UUID.randomUUID(),
                "VALUE_20M_50M",
                300,
                true,
                ApprovalRuleType.VALUE,
                ApprovalCondition.of(
                        Money.vnd("20000000.0000"),
                        Money.vnd("50000000.0000"),
                        Set.of(),
                        Set.of(),
                        Set.of(PurchaseRequestPriority.NORMAL, PurchaseRequestPriority.URGENT)),
                List.of(new ApprovalStepTemplate(1, "PR_APPROVE_L1", ApprovalStepType.SEQUENTIAL, 48, true)),
                null);

        // When/Then
        assertThat(rule.matches(Money.vnd("20000000.0000"), Set.of(), DEPARTMENT_ID, PurchaseRequestPriority.NORMAL))
                .isTrue();
        assertThat(rule.matches(Money.vnd("50000000.0000"), Set.of(), DEPARTMENT_ID, PurchaseRequestPriority.NORMAL))
                .isFalse();
    }

    @Test
    @DisplayName("Rule category match khi request có ít nhất một category giao nhau")
    void should_match_category_rule_when_request_categories_intersect() {
        // Given
        ApprovalRule rule = ApprovalRule.restore(
                UUID.randomUUID(),
                "CAT_IT_SOFTWARE",
                650,
                true,
                ApprovalRuleType.CATEGORY,
                ApprovalCondition.of(null, null, Set.of("SOFTWARE", "SAAS"), Set.of(), Set.of()),
                List.of(new ApprovalStepTemplate(1, "PR_APPROVE_L3", ApprovalStepType.SEQUENTIAL, 48, true)),
                null);

        // When/Then
        assertThat(rule.matches(Money.vnd("1000000.0000"), Set.of("office", "saas"), DEPARTMENT_ID, PurchaseRequestPriority.NORMAL))
                .isTrue();
        assertThat(rule.matches(Money.vnd("1000000.0000"), Set.of("OFFICE_SUPPLIES"), DEPARTMENT_ID, PurchaseRequestPriority.NORMAL))
                .isFalse();
    }
}
