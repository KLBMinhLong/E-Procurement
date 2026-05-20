package com.eprocure.pr.application.port.in;

import java.util.UUID;

/**
 * Command: Cancel a purchase request (requester only, cancellable statuses).
 */
public record CancelPurchaseRequestCommand(
        UUID purchaseRequestId,
        UUID actorId,
        String reason
) {}
