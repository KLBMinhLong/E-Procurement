package com.eprocure.pr.infrastructure.inventory;

import com.eprocure.pr.application.port.out.InventoryCheckPort;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.model.vo.InventoryCheckResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class FallbackInventoryCheckAdapter implements InventoryCheckPort {
    private static final Logger log = LogManager.getLogger(FallbackInventoryCheckAdapter.class);

    @Override
    public InventoryCheckResult check(InventoryCheckQuery query) {
        log.debug("[ACTION] Step InventoryCheckFallback | prId={} | lineItems={}",
                LogMaskingUtil.maskId(query.purchaseRequestId()),
                query.lineItems().size());
        return InventoryCheckResult.empty();
    }
}
