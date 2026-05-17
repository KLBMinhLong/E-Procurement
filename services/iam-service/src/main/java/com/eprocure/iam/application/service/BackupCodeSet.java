package com.eprocure.iam.application.service;

import java.util.List;

public record BackupCodeSet(List<String> rawCodes, List<String> codeHashes) {
    public BackupCodeSet {
        rawCodes = List.copyOf(rawCodes);
        codeHashes = List.copyOf(codeHashes);
    }
}
