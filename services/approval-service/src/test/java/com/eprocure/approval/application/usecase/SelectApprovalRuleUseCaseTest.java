package com.eprocure.approval.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.approval.application.port.in.SelectApprovalRuleQuery;
import com.eprocure.approval.application.service.ApprovalRuleSelectionService;
import com.eprocure.approval.application.service.SelectedApprovalRuleView;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.domain.model.ApprovalCondition;
import com.eprocure.approval.domain.model.ApprovalRule;
import com.eprocure.approval.domain.model.ApprovalRuleType;
import com.eprocure.approval.domain.model.ApprovalStepTemplate;
import com.eprocure.approval.domain.model.ApprovalStepType;
import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.eprocure.approval.domain.model.vo.Money;
import com.eprocure.approval.domain.repository.ApprovalRuleRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SelectApprovalRuleUseCaseTest {
    private static final UUID PR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440100");
    private static final UUID DEPARTMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Test
    @DisplayName("Chọn rule value đúng tại biên 20M")
    void should_select_twenty_to_fifty_rule_when_amount_is_exactly_twenty_million() {
        // Given
        SelectApprovalRuleUseCase useCase = newUseCase(defaultRules());

        // When
        SelectedApprovalRuleView result = useCase.execute(new SelectApprovalRuleQuery(
                PR_ID,
                DEPARTMENT_ID,
                Money.vnd("20000000.0000"),
                Set.of("OFFICE_SUPPLIES"),
                PurchaseRequestPriority.NORMAL));

        // Then
        assertThat(result.primaryRuleName()).isEqualTo("VALUE_20M_50M");
        assertThat(result.steps())
                .extracting(SelectedApprovalRuleView.StepView::approverRole)
                .containsExactly("MANAGER", "DIRECTOR", "FINANCE");
    }

    @Test
    @DisplayName("Rule emergency ưu tiên hơn rule value")
    void should_select_emergency_rule_when_priority_is_emergency() {
        // Given
        SelectApprovalRuleUseCase useCase = newUseCase(defaultRules());

        // When
        SelectedApprovalRuleView result = useCase.execute(new SelectApprovalRuleQuery(
                PR_ID,
                DEPARTMENT_ID,
                Money.vnd("1000000.0000"),
                Set.of("OFFICE_SUPPLIES"),
                PurchaseRequestPriority.EMERGENCY));

        // Then
        assertThat(result.primaryRuleName()).isEqualTo("EMERGENCY");
        assertThat(result.steps())
                .extracting(SelectedApprovalRuleView.StepView::approverRole)
                .containsExactly("MANAGER", "DIRECTOR", "POST_AUDIT");
    }

    @Test
    @DisplayName("Rule category được cộng thêm vào chain chính")
    void should_append_additive_category_steps_when_category_rule_matches() {
        // Given
        SelectApprovalRuleUseCase useCase = newUseCase(defaultRules());

        // When
        SelectedApprovalRuleView result = useCase.execute(new SelectApprovalRuleQuery(
                PR_ID,
                DEPARTMENT_ID,
                Money.vnd("6000000.0000"),
                Set.of("SAAS"),
                PurchaseRequestPriority.NORMAL));

        // Then
        assertThat(result.primaryRuleName()).isEqualTo("VALUE_5M_20M");
        assertThat(result.appliedRuleNames()).containsExactly("VALUE_5M_20M", "CAT_IT_SOFTWARE");
        assertThat(result.steps())
                .extracting(SelectedApprovalRuleView.StepView::approverRole)
                .containsExactly("MANAGER", "FINANCE", "CISO", "IT_MANAGER");
    }

    @Test
    @DisplayName("Ném APR_001 khi không có rule chính nào match")
    void should_throw_apr_001_when_no_primary_rule_matches() {
        // Given
        SelectApprovalRuleUseCase useCase = newUseCase(List.of(categorySoftwareRule()));

        // When/Then
        assertThatThrownBy(() -> useCase.execute(new SelectApprovalRuleQuery(
                PR_ID,
                DEPARTMENT_ID,
                Money.vnd("6000000.0000"),
                Set.of("SAAS"),
                PurchaseRequestPriority.NORMAL)))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode()).isEqualTo(ErrorCode.APR_007));
    }

    private List<ApprovalRule> defaultRules() {
        return List.of(
                emergencyRule(),
                categorySoftwareRule(),
                value20To50Rule(),
                value5To20Rule());
    }

    private SelectApprovalRuleUseCase newUseCase(List<ApprovalRule> rules) {
        return new SelectApprovalRuleUseCase(new ApprovalRuleSelectionService(new StubApprovalRuleRepository(rules)));
    }

    private ApprovalRule emergencyRule() {
        return ApprovalRule.restore(
                UUID.fromString("550e8400-e29b-41d4-a716-446655441000"),
                "EMERGENCY",
                1000,
                true,
                ApprovalRuleType.DEFAULT,
                ApprovalCondition.of(null, null, Set.of(), Set.of(), Set.of(PurchaseRequestPriority.EMERGENCY)),
                List.of(
                        new ApprovalStepTemplate(1, "MANAGER", ApprovalStepType.PARALLEL, 2, true),
                        new ApprovalStepTemplate(1, "DIRECTOR", ApprovalStepType.PARALLEL, 4, true),
                        new ApprovalStepTemplate(2, "POST_AUDIT", ApprovalStepType.SEQUENTIAL, 24, true)),
                null);
    }

    private ApprovalRule categorySoftwareRule() {
        return ApprovalRule.restore(
                UUID.fromString("550e8400-e29b-41d4-a716-446655441001"),
                "CAT_IT_SOFTWARE",
                650,
                true,
                ApprovalRuleType.CATEGORY,
                ApprovalCondition.of(null, null, Set.of("SOFTWARE", "SAAS"), Set.of(), Set.of()),
                List.of(
                        new ApprovalStepTemplate(1, "CISO", ApprovalStepType.SEQUENTIAL, 48, true),
                        new ApprovalStepTemplate(2, "IT_MANAGER", ApprovalStepType.SEQUENTIAL, 48, true)),
                null);
    }

    private ApprovalRule value20To50Rule() {
        return ApprovalRule.restore(
                UUID.fromString("550e8400-e29b-41d4-a716-446655441002"),
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
                List.of(
                        new ApprovalStepTemplate(1, "MANAGER", ApprovalStepType.SEQUENTIAL, 48, true),
                        new ApprovalStepTemplate(2, "DIRECTOR", ApprovalStepType.SEQUENTIAL, 48, true),
                        new ApprovalStepTemplate(3, "FINANCE", ApprovalStepType.SEQUENTIAL, 48, true)),
                null);
    }

    private ApprovalRule value5To20Rule() {
        return ApprovalRule.restore(
                UUID.fromString("550e8400-e29b-41d4-a716-446655441003"),
                "VALUE_5M_20M",
                200,
                true,
                ApprovalRuleType.VALUE,
                ApprovalCondition.of(
                        Money.vnd("5000000.0000"),
                        Money.vnd("20000000.0000"),
                        Set.of(),
                        Set.of(),
                        Set.of(PurchaseRequestPriority.NORMAL, PurchaseRequestPriority.URGENT)),
                List.of(
                        new ApprovalStepTemplate(1, "MANAGER", ApprovalStepType.SEQUENTIAL, 48, true),
                        new ApprovalStepTemplate(2, "FINANCE", ApprovalStepType.SEQUENTIAL, 48, true)),
                null);
    }

    private record StubApprovalRuleRepository(List<ApprovalRule> rules) implements ApprovalRuleRepository {
        @Override
        public List<ApprovalRule> findActiveRules() {
            return rules;
        }
    }
}
