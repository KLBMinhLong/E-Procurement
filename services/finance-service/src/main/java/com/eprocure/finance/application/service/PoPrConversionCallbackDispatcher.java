package com.eprocure.finance.application.service;

import com.eprocure.finance.application.port.out.PurchaseRequestConversionCallbackPort;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.PoPrConversionCallback;
import com.eprocure.finance.domain.model.PoPrConversionCallbackStatus;
import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.repository.PoPrConversionCallbackRepository;
import com.eprocure.finance.domain.repository.PurchaseOrderRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PoPrConversionCallbackDispatcher {
    private static final Logger log = LogManager.getLogger(PoPrConversionCallbackDispatcher.class);
    private static final int ERROR_MAX_LENGTH = 500;

    private final PoPrConversionCallbackRepository callbackRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseRequestConversionCallbackPort callbackPort;
    private final Clock clock;
    private final int batchSize;
    private final int maxAttempts;

    public PoPrConversionCallbackDispatcher(
            PoPrConversionCallbackRepository callbackRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseRequestConversionCallbackPort callbackPort,
            Clock clock,
            @Value("${eprocure.finance.po-conversion-callback.batch-size:20}") int batchSize,
            @Value("${eprocure.finance.po-conversion-callback.max-attempts:5}") int maxAttempts) {
        this.callbackRepository = callbackRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.callbackPort = callbackPort;
        this.clock = clock;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${eprocure.finance.po-conversion-callback.fixed-delay-ms:60000}")
    public void dispatchDue() {
        callbackRepository.findDispatchable(Instant.now(clock), batchSize)
                .forEach(callback -> dispatch(callback.id()));
    }

    public void dispatch(UUID callbackId) {
        callbackRepository.findById(callbackId)
                .filter(callback -> callback.status() != PoPrConversionCallbackStatus.DELIVERED)
                .ifPresent(this::dispatch);
    }

    private void dispatch(PoPrConversionCallback callback) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(callback.poId())
                .orElse(null);
        if (purchaseOrder == null) {
            markFailure(callback, "Purchase Order not found for callback");
            return;
        }
        try {
            callbackPort.markConverted(
                    callback.prId(),
                    callback.poId(),
                    purchaseOrder.poNumber(),
                    callback.idempotencyKey());
            callbackRepository.markDelivered(callback.id(), Instant.now(clock));
            log.info("[ACTION] Delivered PoPrConversionCallback | poId={} | prId={}",
                    LogMaskingUtil.maskId(callback.poId()),
                    LogMaskingUtil.maskId(callback.prId()));
        } catch (RuntimeException exception) {
            markFailure(callback, exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private void markFailure(PoPrConversionCallback callback, String error) {
        int attempts = callback.attempts() + 1;
        String sanitizedError = truncate(error);
        if (attempts >= maxAttempts) {
            callbackRepository.markExhausted(callback.id(), attempts, sanitizedError);
        } else {
            callbackRepository.markRetryable(
                    callback.id(),
                    attempts,
                    Instant.now(clock).plus(retryDelay(attempts)),
                    sanitizedError);
        }
        log.warn("[ACTION] PoPrConversionCallback retry scheduled | poId={} | prId={} | attempts={}",
                LogMaskingUtil.maskId(callback.poId()),
                LogMaskingUtil.maskId(callback.prId()),
                attempts);
    }

    private Duration retryDelay(int attempts) {
        return Duration.ofMinutes(Math.min(30L, attempts * 2L));
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown callback failure";
        }
        return value.length() <= ERROR_MAX_LENGTH ? value : value.substring(0, ERROR_MAX_LENGTH);
    }
}
