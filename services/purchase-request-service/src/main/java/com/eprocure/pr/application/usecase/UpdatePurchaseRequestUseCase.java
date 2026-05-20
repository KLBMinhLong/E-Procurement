package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.port.in.UpdatePurchaseRequestCommand;
import com.eprocure.pr.application.port.in.UpdatePurchaseRequestCommand.LineItemCommand;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.application.service.UpdatedPurchaseRequestView;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.model.PrLineItem;
import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.model.vo.Money;
import com.eprocure.pr.domain.model.vo.Quantity;
import com.eprocure.pr.domain.repository.PurchaseRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Update an existing DRAFT or CHANGES_REQUESTED purchase request.
 *
 * <p>Flow:
 * <ol>
 *   <li>Verify idempotency key format</li>
 *   <li>Check idempotency cache (replay)</li>
 *   <li>Load PR — throw PR_001 if not found</li>
 *   <li>Validate actor is requester and status is editable</li>
 *   <li>Validate needByDate not in past</li>
 *   <li>Apply domain updateDraft() — replaces line items, recalculates total</li>
 *   <li>Persist updated PR (root only — line items replaced via delete+insert)</li>
 *   <li>Cache idempotency result</li>
 * </ol>
 */
@Service
public class UpdatePurchaseRequestUseCase {

    private static final Logger log = LogManager.getLogger(UpdatePurchaseRequestUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "update-pr";

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public UpdatePurchaseRequestUseCase(
            PurchaseRequestRepository purchaseRequestRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public UpdatedPurchaseRequestView execute(UpdatePurchaseRequestCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);

        // 1. Idempotency check
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                UpdatedPurchaseRequestView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit UpdatePurchaseRequest | userId={} | key={}",
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return cached.get();
        }

        log.info("[ACTION] Start UpdatePurchaseRequest | userId={} | prId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.purchaseRequestId()));

        // 2. Load PR
        PurchaseRequest pr = purchaseRequestRepository.findById(command.purchaseRequestId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_001));

        // 3. Validate actor and status
        if (!pr.canBeEditedBy(command.actorId())) {
            if (!pr.getRequesterId().equals(command.actorId())) {
                throw new BusinessException(ErrorCode.IAM_004);
            }
            throw new BusinessException(ErrorCode.PR_003);
        }

        // 4. Validate needByDate not in past
        validateNeedByDate(command.needByDate());

        // 5. Apply domain update
        Instant now = Instant.now(clock);
        try {
            pr.updateDraft(
                    command.actorId(),
                    command.title(),
                    command.justification(),
                    resolvePriority(command.priority()),
                    command.urgencyReason(),
                    command.needByDate(),
                    null,   // relatedContractId — not exposed in UpdatePrRequest per OpenAPI spec
                    false,  // blanketRelease — not exposed in UpdatePrRequest per OpenAPI spec
                    toDomainLineItems(command.lineItems()),
                    now);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw mapDomainException(ex);
        }

        log.info("[ACTION] Step replaceLineItems | prId={} | lineItemCount={}",
                LogMaskingUtil.maskId(pr.getId()), pr.getLineItems().size());

        // 6. Persist — delete old line items, insert new ones, update root
        purchaseRequestRepository.updateWithLineItems(pr);

        // 7. Cache idempotency result
        UpdatedPurchaseRequestView view = UpdatedPurchaseRequestView.from(pr);
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);

        log.info("[ACTION] Complete UpdatePurchaseRequest | userId={} | prId={} | prNumber={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(pr.getId()),
                pr.getPrNumber());
        return view;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void validateNeedByDate(LocalDate needByDate) {
        if (needByDate != null && needByDate.isBefore(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.PR_013);
        }
    }

    private PrPriority resolvePriority(String priority) {
        if (priority == null) return PrPriority.NORMAL;
        try {
            return PrPriority.valueOf(priority);
        } catch (IllegalArgumentException ex) {
            return PrPriority.NORMAL;
        }
    }

    private List<PrLineItem> toDomainLineItems(List<LineItemCommand> lineItems) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new BusinessException(ErrorCode.PR_012);
        }
        return lineItems.stream().map(this::toDomainLineItem).toList();
    }

    private PrLineItem toDomainLineItem(LineItemCommand cmd) {
        try {
            return PrLineItem.create(
                    cmd.itemCode(),
                    cmd.itemName(),
                    cmd.description(),
                    cmd.categoryCode(),
                    new Quantity(cmd.quantityAmount(), cmd.quantityUnit()),
                    new Money(cmd.unitPriceAmount(), cmd.unitPriceCurrency()),
                    cmd.preferredVendorId(),
                    cmd.specifications(),
                    cmd.glAccountCode(),
                    cmd.fromCatalog());
        } catch (IllegalArgumentException ex) {
            throw mapDomainException(ex);
        }
    }

    private BusinessException mapDomainException(RuntimeException ex) {
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        if (msg.contains("line item"))    return new BusinessException(ErrorCode.PR_012);
        if (msg.contains("urgencyReason")) return new BusinessException(ErrorCode.PR_006);
        if (msg.contains("justification")) return new BusinessException(ErrorCode.PR_014);
        if (msg.contains("quantity"))      return new BusinessException(ErrorCode.PR_010);
        if (msg.contains("unitPrice") || msg.contains("amount must not be negative"))
                                           return new BusinessException(ErrorCode.PR_011);
        if (msg.contains("cannot be edited")) return new BusinessException(ErrorCode.PR_003);
        return new BusinessException(ErrorCode.VAL_001);
    }
}
