package com.eprocure.approval.domain.repository;

import com.eprocure.approval.domain.model.ApprovalRule;
import java.util.List;

public interface ApprovalRuleRepository {
    List<ApprovalRule> findActiveRules();
}
