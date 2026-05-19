package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.port.in.CreatePurchaseRequestCommand;
import com.eprocure.pr.application.service.CreatePurchaseRequestResult;
import com.eprocure.pr.application.service.CreatedPurchaseRequestView;
import com.eprocure.pr.application.service.IdempotencyService;
import com.eprocure.pr.application.service.PurchaseRequestNumberGenerator;
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
import java.time.Year;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePurchaseRequestUseCase {
    private static final Logger log = LogManager.getLogger(CreatePurchaseRequestUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "create-pr";

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestNumberGenerator numberGenerator;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public CreatePurchaseRequestUseCase(
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestNumberGenerator numberGenerator,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.numberGenerator = numberGenerator;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public CreatePurchaseRequestResult execute(CreatePurchaseRequestCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.requesterId(),
                idempotencyKey,
                CreatedPurchaseRequestView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit CreatePurchaseRequest | userId={} | key={}",
                    LogMaskingUtil.maskId(command.requesterId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return new CreatePurchaseRequestResult(cached.get(), true);
        }

        log.info("[ACTION] Start CreatePurchaseRequest | userId={}", LogMaskingUtil.maskId(command.requesterId()));
        validateNeedByDate(command.needByDate());
        PurchaseRequest purchaseRequest = buildPurchaseRequest(command);
        purchaseRequestRepository.save(purchaseRequest);
        CreatedPurchaseRequestView view = new CreatedPurchaseRequestView(
                purchaseRequest.getId(),
                purchaseRequest.getPrNumber(),
                purchaseRequest.getStatus());
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.requesterId(), idempotencyKey, view);
        log.info("[ACTION] Complete CreatePurchaseRequest | userId={} | prId={} | prNumber={}",
                LogMaskingUtil.maskId(command.requesterId()),
                LogMaskingUtil.maskId(purchaseRequest.getId()),
                purchaseRequest.getPrNumber());
        return new CreatePurchaseRequestResult(view, false);
    }

    private PurchaseRequest buildPurchaseRequest(CreatePurchaseRequestCommand command) {
        try {
            return PurchaseRequest.create(
                    numberGenerator.next(),
                    command.requesterId(),
                    command.departmentId(),
                    command.title(),
                    command.justification(),
                    command.priority() == null ? PrPriority.NORMAL : command.priority(),
                    command.urgencyReason(),
                    resolveFiscalYear(command.needByDate()),
                    command.needByDate(),
                    command.relatedContractId(),
                    command.blanketRelease(),
                    toDomainLineItems(command.lineItems()),
                    Instant.now(clock));
        } catch (IllegalArgumentException exception) {
            throw mapDomainValidation(exception);
        }
    }

    private List<PrLineItem> toDomainLineItems(List<CreatePurchaseRequestCommand.LineItemCommand> lineItems) {
        return lineItems.stream()
                .map(this::toDomainLineItem)
                .toList();
    }

    private PrLineItem toDomainLineItem(CreatePurchaseRequestCommand.LineItemCommand command) {
        try {
            return PrLineItem.create(
                    command.itemCode(),
                    command.itemName(),
                    command.description(),
                    command.categoryCode(),
                    new Quantity(command.quantity(), command.unit()),
                    new Money(command.unitPrice(), command.currency()),
                    command.preferredVendorId(),
                    command.specifications(),
                    command.glAccountCode(),
                    command.fromCatalog());
        } catch (IllegalArgumentException exception) {
            throw mapDomainValidation(exception);
        }
    }

    private int resolveFiscalYear(LocalDate needByDate) {
        return needByDate == null ? Year.now(clock).getValue() : needByDate.getYear();
    }

    private void validateNeedByDate(LocalDate needByDate) {
        if (needByDate != null && needByDate.isBefore(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.PR_013);
        }
    }

    private BusinessException mapDomainValidation(IllegalArgumentException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.contains("line item")) {
            return new BusinessException(ErrorCode.PR_012);
        }
        if (message.contains("urgencyReason")) {
            return new BusinessException(ErrorCode.PR_006);
        }
        if (message.contains("justification")) {
            return new BusinessException(ErrorCode.PR_014);
        }
        if (message.contains("quantity")) {
            return new BusinessException(ErrorCode.PR_010);
        }
        if (message.contains("unitPrice") || message.contains("amount must not be negative")) {
            return new BusinessException(ErrorCode.PR_011);
        }
        return new BusinessException(ErrorCode.VAL_001);
    }
}
