package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.GetPurchaseOrderQuery;
import com.eprocure.finance.application.service.PurchaseOrderView;
import com.eprocure.finance.application.service.PurchaseOrderViewAssembler;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPurchaseOrderUseCase {
    private static final Logger log = LogManager.getLogger(GetPurchaseOrderUseCase.class);
    private static final String PERMISSION_VIEW_ALL = "PO_VIEW_ALL";
    private static final String PERMISSION_VIEW_OWN = "PO_VIEW_OWN";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderViewAssembler viewAssembler;

    public GetPurchaseOrderUseCase(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderViewAssembler viewAssembler) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.viewAssembler = viewAssembler;
    }

    @Transactional(readOnly = true)
    public PurchaseOrderView execute(GetPurchaseOrderQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        log.info("[ACTION] Start GetPurchaseOrder | userId={} | poId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.purchaseOrderId()));
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(query.purchaseOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_006));
        verifyScope(query, purchaseOrder);
        log.info("[ACTION] Complete GetPurchaseOrder | userId={} | poId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.purchaseOrderId()));
        return viewAssembler.toView(purchaseOrder);
    }

    private void verifyScope(GetPurchaseOrderQuery query, PurchaseOrder purchaseOrder) {
        if (query.hasPermission(PERMISSION_VIEW_ALL)) {
            return;
        }
        if (query.hasPermission(PERMISSION_VIEW_OWN) && purchaseOrder.isPurchasingOfficer(query.actorId())) {
            return;
        }
        throw new BusinessException(ErrorCode.IAM_004);
    }
}
