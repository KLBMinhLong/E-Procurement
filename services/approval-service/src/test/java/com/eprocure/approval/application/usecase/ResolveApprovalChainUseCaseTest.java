package com.eprocure.approval.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.approval.application.port.in.ResolveApprovalChainCommand;
import com.eprocure.approval.application.port.out.OrgApproverPort;
import com.eprocure.approval.application.service.ApprovalChainResolutionService;
import com.eprocure.approval.application.service.ApprovalRuleSelectionService;
import com.eprocure.approval.application.service.BusinessHoursCalendar;
import com.eprocure.approval.application.service.ResolvedApprovalChainView;
import com.eprocure.approval.application.service.SlaDeadlineCalculator;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResolveApprovalChainUseCaseTest {
    private static final UUID PR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440100");
    private static final UUID DEPARTMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID REQUESTER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID MANAGER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID FINANCE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final Instant MONDAY_08_VN = Instant.parse("2026-06-01T01:00:00Z");

    @Test
    void should_resolve_chain_when_rule_steps_have_approvers() {
        FakeOrgApproverPort orgApproverPort = new FakeOrgApproverPort(Map.of(
                "MANAGER", List.of(candidate(MANAGER_ID, "manager")),
                "FINANCE", List.of(candidate(FINANCE_ID, "finance"))));
        ResolveApprovalChainUseCase useCase = newUseCase(orgApproverPort);

        ResolvedApprovalChainView result = useCase.execute(defaultCommand());

        assertThat(result.primaryRuleName()).isEqualTo("VALUE_DEFAULT");
        assertThat(result.steps())
                .extracting(ResolvedApprovalChainView.ResolvedApprovalStepView::approverRole)
                .containsExactly("MANAGER", "FINANCE");
        assertThat(result.steps())
                .extracting(step -> step.approver().id())
                .containsExactly(MANAGER_ID, FINANCE_ID);
        assertThat(result.steps())
                .extracting(ResolvedApprovalChainView.ResolvedApprovalStepView::slaDeadline)
                .containsExactly(
                        Instant.parse("2026-06-03T06:00:00Z"),
                        Instant.parse("2026-06-08T01:30:00Z"));
        assertThat(orgApproverPort.queries)
                .extracting(OrgApproverPort.ResolveApproverQuery::requesterId)
                .containsOnly(REQUESTER_ID);
    }

    @Test
    void should_skip_requester_candidate_when_other_approver_exists() {
        FakeOrgApproverPort orgApproverPort = new FakeOrgApproverPort(Map.of(
                "MANAGER", List.of(candidate(REQUESTER_ID, "requester"), candidate(MANAGER_ID, "manager")),
                "FINANCE", List.of(candidate(FINANCE_ID, "finance"))));
        ResolveApprovalChainUseCase useCase = newUseCase(orgApproverPort);

        ResolvedApprovalChainView result = useCase.execute(defaultCommand());

        assertThat(result.steps().get(0).approver().id()).isEqualTo(MANAGER_ID);
    }

    @Test
    void should_throw_apr_004_when_only_requester_can_approve() {
        FakeOrgApproverPort orgApproverPort = new FakeOrgApproverPort(Map.of(
                "MANAGER", List.of(candidate(REQUESTER_ID, "requester")),
                "FINANCE", List.of(candidate(FINANCE_ID, "finance"))));
        ResolveApprovalChainUseCase useCase = newUseCase(orgApproverPort);

        assertThatThrownBy(() -> useCase.execute(defaultCommand()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.APR_004);
    }

    @Test
    void should_throw_apr_002_when_no_approver_exists() {
        FakeOrgApproverPort orgApproverPort = new FakeOrgApproverPort(Map.of(
                "MANAGER", List.of(),
                "FINANCE", List.of(candidate(FINANCE_ID, "finance"))));
        ResolveApprovalChainUseCase useCase = newUseCase(orgApproverPort);

        assertThatThrownBy(() -> useCase.execute(defaultCommand()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.APR_002);
    }

    private ResolveApprovalChainUseCase newUseCase(FakeOrgApproverPort orgApproverPort) {
        return new ResolveApprovalChainUseCase(new ApprovalChainResolutionService(
                new ApprovalRuleSelectionService(new StubApprovalRuleRepository(List.of(defaultRule()))),
                orgApproverPort,
                new SlaDeadlineCalculator(
                        BusinessHoursCalendar.from("Asia/Ho_Chi_Minh", "08:00", "17:30", "MON,TUE,WED,THU,FRI"),
                        Clock.fixed(MONDAY_08_VN, ZoneOffset.UTC))));
    }

    private ResolveApprovalChainCommand defaultCommand() {
        return new ResolveApprovalChainCommand(
                PR_ID,
                REQUESTER_ID,
                DEPARTMENT_ID,
                Money.vnd("12000000.0000"),
                Set.of("OFFICE_SUPPLIES"),
                PurchaseRequestPriority.NORMAL);
    }

    private ApprovalRule defaultRule() {
        return ApprovalRule.restore(
                UUID.fromString("550e8400-e29b-41d4-a716-446655441000"),
                "VALUE_DEFAULT",
                100,
                true,
                ApprovalRuleType.DEFAULT,
                ApprovalCondition.of(null, null, Set.of(), Set.of(), Set.of(PurchaseRequestPriority.NORMAL)),
                List.of(
                        new ApprovalStepTemplate(1, "MANAGER", ApprovalStepType.SEQUENTIAL, 24, true),
                        new ApprovalStepTemplate(2, "FINANCE", ApprovalStepType.SEQUENTIAL, 48, true)),
                null);
    }

    private static OrgApproverPort.ApproverCandidate candidate(UUID id, String username) {
        return new OrgApproverPort.ApproverCandidate(
                id,
                "EMP-" + username,
                username,
                username,
                username + "@eprocure.local",
                DEPARTMENT_ID);
    }

    private record StubApprovalRuleRepository(List<ApprovalRule> rules) implements ApprovalRuleRepository {
        @Override
        public List<ApprovalRule> findActiveRules() {
            return rules;
        }
    }

    private static final class FakeOrgApproverPort implements OrgApproverPort {
        private final Map<String, List<ApproverCandidate>> candidatesByRole;
        private final List<ResolveApproverQuery> queries = new ArrayList<>();

        private FakeOrgApproverPort(Map<String, List<ApproverCandidate>> candidatesByRole) {
            this.candidatesByRole = candidatesByRole;
        }

        @Override
        public List<ApproverCandidate> resolveApprovers(ResolveApproverQuery query) {
            queries.add(query);
            return candidatesByRole.getOrDefault(query.approverRole(), List.of());
        }
    }
}
