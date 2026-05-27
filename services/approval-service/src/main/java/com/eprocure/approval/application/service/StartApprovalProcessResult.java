package com.eprocure.approval.application.service;

import java.util.Optional;

public record StartApprovalProcessResult(
        boolean skipped,
        Optional<StartedApprovalProcessView> process) {

    public StartApprovalProcessResult {
        process = process == null ? Optional.empty() : process;
    }

    public static StartApprovalProcessResult started(StartedApprovalProcessView process) {
        return new StartApprovalProcessResult(false, Optional.of(process));
    }

    public static StartApprovalProcessResult skippedEvent() {
        return new StartApprovalProcessResult(true, Optional.empty());
    }
}
