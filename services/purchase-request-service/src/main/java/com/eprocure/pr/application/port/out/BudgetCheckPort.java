package com.eprocure.pr.application.port.out;

import com.eprocure.pr.domain.model.vo.BudgetCheckResult;
import com.eprocure.pr.domain.model.vo.Money;
import java.util.Objects;
import java.util.UUID;

public interface BudgetCheckPort {
    BudgetCheckResult check(BudgetCheckQuery query);

    record BudgetCheckQuery(
            UUID purchaseRequestId,
            UUID departmentId,
            int fiscalYear,
            Money totalAmount) {
        public BudgetCheckQuery {
            purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
            departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
            totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        }
    }
}
