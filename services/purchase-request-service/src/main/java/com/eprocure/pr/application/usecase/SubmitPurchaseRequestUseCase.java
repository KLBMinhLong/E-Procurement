package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.port.in.SubmitPurchaseRequestCommand;
import com.eprocure.pr.application.port.out.BudgetCheckPort;
import com.eprocure.pr.application.port.out.BudgetCheckPort.BudgetCheckQuery;
import com.eprocure.pr.application.port.out.InventoryCheckPort;
import com.eprocure.pr.application.port.out.InventoryCheckPort.InventoryCheckQuery;
import com.eprocure.pr.application.port.out.InventoryCheckPort.LineItem;
import com.eprocure.pr.application.port.out.PrSubmittedEventPublisher;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.application.service.SubmitPurchaseRequestResult;
import com.eprocure.pr.application.service.SubmittedPurchaseRequestView;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.event.PrSubmittedEvent;
import com.eprocure.pr.domain.model.PrLineItem;
import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.model.vo.BudgetCheckResult;
import com.eprocure.pr.domain.model.vo.InventoryCheckResult;
import com.eprocure.pr.domain.repository.PurchaseRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitPurchaseRequestUseCase {
    private static final Logger log = LogManager.getLogger(SubmitPurchaseRequestUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "submit-pr";

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final BudgetCheckPort budgetCheckPort;
    private final InventoryCheckPort inventoryCheckPort;
    private final PrSubmittedEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public SubmitPurchaseRequestUseCase(
            PurchaseRequestRepository purchaseRequestRepository,
            BudgetCheckPort budgetCheckPort,
            InventoryCheckPort inventoryCheckPort,
            PrSubmittedEventPublisher eventPublisher,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.budgetCheckPort = budgetCheckPort;
        this.inventoryCheckPort = inventoryCheckPort;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public SubmitPurchaseRequestResult execute(SubmitPurchaseRequestCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                SubmittedPurchaseRequestView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit SubmitPurchaseRequest | userId={} | key={}",
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return new SubmitPurchaseRequestResult(cached.get(), true);
        }

        log.info("[ACTION] Start SubmitPurchaseRequest | userId={} | prId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.purchaseRequestId()));

        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(command.purchaseRequestId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PR_001));
        ensureSubmitAllowed(purchaseRequest, command);
        BudgetCheckResult budgetCheck = runBudgetCheck(purchaseRequest);
        if (budgetCheck.requiresOverride()) {
            throw new BusinessException(ErrorCode.PR_002, new BudgetOverrideRequired(true, budgetCheck));
        }
        InventoryCheckResult inventoryCheck = runInventoryCheck(purchaseRequest);

        purchaseRequest.submit(command.actorId(), Instant.now(clock), budgetCheck, inventoryCheck);
        purchaseRequestRepository.update(purchaseRequest);
        publishSubmittedEvents(purchaseRequest.pullDomainEvents());

        SubmittedPurchaseRequestView view = new SubmittedPurchaseRequestView(
                purchaseRequest.getId(),
                purchaseRequest.getPrNumber(),
                purchaseRequest.getStatus());
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete SubmitPurchaseRequest | userId={} | prId={} | prNumber={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(purchaseRequest.getId()),
                purchaseRequest.getPrNumber());
        return new SubmitPurchaseRequestResult(view, false);
    }

    private void ensureSubmitAllowed(PurchaseRequest purchaseRequest, SubmitPurchaseRequestCommand command) {
        if (!purchaseRequest.getRequesterId().equals(command.actorId())) {
            throw new BusinessException(ErrorCode.IAM_004);
        }
        if (!purchaseRequest.getStatus().isEditableByRequester()) {
            throw new BusinessException(ErrorCode.PR_003);
        }
        if (purchaseRequest.getNeedByDate().isEmpty()) {
            throw new BusinessException(ErrorCode.PR_013);
        }
    }

    private BudgetCheckResult runBudgetCheck(PurchaseRequest purchaseRequest) {
        return Objects.requireNonNull(
                budgetCheckPort.check(new BudgetCheckQuery(
                        purchaseRequest.getId(),
                        purchaseRequest.getDepartmentId(),
                        purchaseRequest.getFiscalYear(),
                        purchaseRequest.getTotalAmount())),
                "budgetCheck must not be null");
    }

    private InventoryCheckResult runInventoryCheck(PurchaseRequest purchaseRequest) {
        return Objects.requireNonNull(
                inventoryCheckPort.check(new InventoryCheckQuery(
                        purchaseRequest.getId(),
                        purchaseRequest.getLineItems().stream()
                                .map(this::toInventoryLineItem)
                                .toList())),
                "inventoryCheck must not be null");
    }

    private LineItem toInventoryLineItem(PrLineItem lineItem) {
        return new LineItem(
                lineItem.getItemCode().orElse(null),
                lineItem.getItemName(),
                lineItem.getQuantity(),
                lineItem.isFromCatalog());
    }

    private void publishSubmittedEvents(List<Object> events) {
        events.stream()
                .filter(PrSubmittedEvent.class::isInstance)
                .map(PrSubmittedEvent.class::cast)
                .forEach(eventPublisher::publish);
    }

    public record BudgetOverrideRequired(boolean requiresOverrideApproval, BudgetCheckResult budgetCheck) {
    }
}
