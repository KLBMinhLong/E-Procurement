package com.eprocure.vendor.application.service;

public record RfqMutationResult(RfqDetailView view, boolean replayed) {
    public static RfqMutationResult fresh(RfqDetailView view) {
        return new RfqMutationResult(view, false);
    }

    public static RfqMutationResult replayed(RfqDetailView view) {
        return new RfqMutationResult(view, true);
    }
}
