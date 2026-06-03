package com.eprocure.finance.presentation.response;

import java.util.UUID;

public record VendorSnapshotResponse(UUID id, String name, String email, String taxCode) {
}
