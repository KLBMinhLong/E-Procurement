package com.eprocure.pr.application.service;

import java.util.Objects;

public record SubmitPurchaseRequestResult(
        SubmittedPurchaseRequestView view,
        boolean replayed) {
    public SubmitPurchaseRequestResult {
        view = Objects.requireNonNull(view, "view must not be null");
    }
}
