package com.eprocure.approval.domain.repository;

import com.eprocure.approval.domain.model.ApprovalRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRuleRepository {
    List<ApprovalRule> findActiveRules();

    List<ApprovalRule> findAllRules();

    Optional<ApprovalRule> findById(UUID id);

    boolean existsByRuleName(String ruleName, UUID excludedId);

    void save(ApprovalRule rule, UUID actorId);

    void update(ApprovalRule rule, UUID actorId);

    void deactivate(UUID id, UUID actorId);
}
