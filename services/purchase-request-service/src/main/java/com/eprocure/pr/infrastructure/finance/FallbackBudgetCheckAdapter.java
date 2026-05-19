package com.eprocure.pr.infrastructure.finance;

import com.eprocure.pr.application.port.out.BudgetCheckPort;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.model.vo.BudgetCheckResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.pr.integration.fallback-enabled", havingValue = "true", matchIfMissing = true)
public class FallbackBudgetCheckAdapter implements BudgetCheckPort {
    private static final Logger log = LogManager.getLogger(FallbackBudgetCheckAdapter.class);

    @Override
    public BudgetCheckResult check(BudgetCheckQuery query) {
        log.debug("[ACTION] Step BudgetCheckFallback | prId={} | fiscalYear={}",
                LogMaskingUtil.maskId(query.purchaseRequestId()),
                query.fiscalYear());
        return BudgetCheckResult.pass(query.totalAmount());
    }
}
