package com.eprocure.approval.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.approval.application.port.in.ApprovalRuleConditionCommand;
import com.eprocure.approval.application.port.in.ApprovalRuleStepCommand;
import com.eprocure.approval.application.port.in.CreateApprovalRuleCommand;
import com.eprocure.approval.application.port.in.DeactivateApprovalRuleCommand;
import com.eprocure.approval.application.port.in.UpdateApprovalRuleCommand;
import com.eprocure.approval.application.service.ApprovalRuleAdminView;
import com.eprocure.approval.application.service.ApprovalRuleMutationResult;
import com.eprocure.approval.application.service.IdempotencyService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApprovalRuleAdminUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000010");
    private static final UUID RULE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655441010");
    private static final UUID DEPARTMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";

    @Test
    void should_create_approval_rule_when_valid_command() {
        FakeApprovalRuleRepository repository = new FakeApprovalRuleRepository();
        FakeIdempotencyService idempotencyService = new FakeIdempotencyService();
        CreateApprovalRuleUseCase useCase = new CreateApprovalRuleUseCase(repository, idempotencyService);

        ApprovalRuleMutationResult result = useCase.execute(createCommand("value_custom"), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.view().ruleName()).isEqualTo("VALUE_CUSTOM");
        assertThat(result.view().conditions().minValue()).isEqualTo("1000000.0000");
        assertThat(repository.savedRule).isNotNull();
        assertThat(repository.savedRule.getStepTemplates()).hasSize(2);
        assertThat(idempotencyService.savedResponse).isInstanceOf(ApprovalRuleAdminView.class);
    }

    @Test
    void should_return_cached_result_when_create_idempotency_replayed() {
        FakeIdempotencyService idempotencyService = new FakeIdempotencyService();
        idempotencyService.cached = ApprovalRuleAdminView.from(existingRule());
        CreateApprovalRuleUseCase useCase = new CreateApprovalRuleUseCase(new FakeApprovalRuleRepository(), idempotencyService);

        ApprovalRuleMutationResult result = useCase.execute(createCommand("value_custom"), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isTrue();
        assertThat(result.view().id()).isEqualTo(RULE_ID);
    }

    @Test
    void should_throw_apr_010_when_rule_name_already_exists() {
        FakeApprovalRuleRepository repository = new FakeApprovalRuleRepository();
        repository.rules.put("VALUE_CUSTOM", existingRule());
        CreateApprovalRuleUseCase useCase = new CreateApprovalRuleUseCase(repository, new FakeIdempotencyService());

        assertThatThrownBy(() -> useCase.execute(createCommand("VALUE_CUSTOM"), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.APR_010);
    }

    @Test
    void should_update_approval_rule_when_rule_exists() {
        FakeApprovalRuleRepository repository = new FakeApprovalRuleRepository();
        repository.rules.put(existingRule().getRuleName(), existingRule());
        UpdateApprovalRuleUseCase useCase = new UpdateApprovalRuleUseCase(repository, new FakeIdempotencyService());

        ApprovalRuleMutationResult result = useCase.execute(updateCommand(), IDEMPOTENCY_KEY);

        assertThat(result.view().priority()).isEqualTo(900);
        assertThat(result.view().steps())
                .extracting(ApprovalRuleAdminView.StepTemplateView::requiredPermission)
                .containsExactly("PR_APPROVE_L2");
        assertThat(repository.updatedRule.getRuleName()).isEqualTo("VALUE_UPDATED");
    }

    @Test
    void should_deactivate_approval_rule_when_rule_exists() {
        FakeApprovalRuleRepository repository = new FakeApprovalRuleRepository();
        repository.rules.put(existingRule().getRuleName(), existingRule());
        DeactivateApprovalRuleUseCase useCase = new DeactivateApprovalRuleUseCase(repository, new FakeIdempotencyService());

        ApprovalRuleMutationResult result = useCase.execute(
                new DeactivateApprovalRuleCommand(RULE_ID, ACTOR_ID, "obsolete"),
                IDEMPOTENCY_KEY);

        assertThat(result.view().active()).isFalse();
        assertThat(repository.deactivatedRuleId).isEqualTo(RULE_ID);
    }

    @Test
    void should_list_all_rules_ordered_by_repository() {
        FakeApprovalRuleRepository repository = new FakeApprovalRuleRepository();
        repository.orderedRules.add(existingRule());
        ListApprovalRulesUseCase useCase = new ListApprovalRulesUseCase(repository);

        List<ApprovalRuleAdminView> result = useCase.execute();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(RULE_ID);
    }

    private CreateApprovalRuleCommand createCommand(String ruleName) {
        return new CreateApprovalRuleCommand(
                ACTOR_ID,
                ruleName,
                800,
                ApprovalRuleType.VALUE,
                new ApprovalRuleConditionCommand(
                        "1000000.0000",
                        "5000000.0000",
                        List.of("office_supplies"),
                        List.of(DEPARTMENT_ID),
                        List.of("normal")),
                List.of(
                        new ApprovalRuleStepCommand(1, "pr_approve_l1", ApprovalStepType.SEQUENTIAL, 24, true),
                        new ApprovalRuleStepCommand(2, "pr_approve_finance", ApprovalStepType.SEQUENTIAL, 48, true)),
                "Custom approval rule");
    }

    private UpdateApprovalRuleCommand updateCommand() {
        return new UpdateApprovalRuleCommand(
                RULE_ID,
                ACTOR_ID,
                "VALUE_UPDATED",
                900,
                true,
                ApprovalRuleType.VALUE,
                new ApprovalRuleConditionCommand(null, "10000000.0000", List.of(), List.of(), List.of("URGENT")),
                List.of(new ApprovalRuleStepCommand(1, "pr_approve_l2", ApprovalStepType.SEQUENTIAL, 12, true)),
                "Updated");
    }

    private static ApprovalRule existingRule() {
        return ApprovalRule.restore(
                RULE_ID,
                "VALUE_CUSTOM",
                800,
                true,
                ApprovalRuleType.VALUE,
                ApprovalCondition.of(
                        Money.vnd("1000000.0000"),
                        Money.vnd("5000000.0000"),
                        Set.of("OFFICE_SUPPLIES"),
                        Set.of(DEPARTMENT_ID),
                        Set.of(PurchaseRequestPriority.NORMAL)),
                List.of(new ApprovalStepTemplate(1, "PR_APPROVE_L1", ApprovalStepType.SEQUENTIAL, 24, true)),
                "Custom");
    }

    private static final class FakeApprovalRuleRepository implements ApprovalRuleRepository {
        private final Map<String, ApprovalRule> rules = new HashMap<>();
        private final List<ApprovalRule> orderedRules = new ArrayList<>();
        private ApprovalRule savedRule;
        private ApprovalRule updatedRule;
        private UUID deactivatedRuleId;

        @Override
        public List<ApprovalRule> findActiveRules() {
            return orderedRules.stream().filter(ApprovalRule::isActive).toList();
        }

        @Override
        public List<ApprovalRule> findAllRules() {
            return orderedRules;
        }

        @Override
        public Optional<ApprovalRule> findById(UUID id) {
            return rules.values().stream().filter(rule -> rule.getId().equals(id)).findFirst();
        }

        @Override
        public boolean existsByRuleName(String ruleName, UUID excludedId) {
            ApprovalRule existing = rules.get(normalize(ruleName));
            return existing != null && (excludedId == null || !existing.getId().equals(excludedId));
        }

        @Override
        public void save(ApprovalRule rule, UUID actorId) {
            savedRule = rule;
            rules.put(rule.getRuleName(), rule);
        }

        @Override
        public void update(ApprovalRule rule, UUID actorId) {
            updatedRule = rule;
            rules.put(rule.getRuleName(), rule);
        }

        @Override
        public void deactivate(UUID id, UUID actorId) {
            deactivatedRuleId = id;
        }

        private String normalize(String value) {
            return value.trim().toUpperCase(Locale.ROOT);
        }
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
        private ApprovalRuleAdminView cached;
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
            savedResponse = response;
        }
    }
}
