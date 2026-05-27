package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.port.in.ResolveApprovalChainCommand;
import com.eprocure.approval.application.service.ApprovalChainResolutionService;
import com.eprocure.approval.application.service.ResolvedApprovalChainView;
import com.eprocure.approval.common.util.LogMaskingUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ResolveApprovalChainUseCase {
    private static final Logger log = LogManager.getLogger(ResolveApprovalChainUseCase.class);

    private final ApprovalChainResolutionService approvalChainResolutionService;

    public ResolveApprovalChainUseCase(ApprovalChainResolutionService approvalChainResolutionService) {
        this.approvalChainResolutionService = approvalChainResolutionService;
    }

    @Transactional(readOnly = true)
    public ResolvedApprovalChainView execute(ResolveApprovalChainCommand command) {
        log.info("[ACTION] Start ResolveApprovalChain | prId={} | requesterId={} | departmentId={}",
                LogMaskingUtil.maskId(command.purchaseRequestId()),
                LogMaskingUtil.maskId(command.requesterId()),
                LogMaskingUtil.maskId(command.departmentId()));

        ResolvedApprovalChainView result = approvalChainResolutionService.resolve(command);

        log.info("[ACTION] Complete ResolveApprovalChain | prId={} | primaryRule={} | stepCount={}",
                LogMaskingUtil.maskId(command.purchaseRequestId()),
                result.primaryRuleName(),
                result.steps().size());
        return result;
    }
}
