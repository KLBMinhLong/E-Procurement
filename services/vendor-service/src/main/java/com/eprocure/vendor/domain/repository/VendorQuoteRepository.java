package com.eprocure.vendor.domain.repository;

import com.eprocure.vendor.domain.model.VendorQuote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorQuoteRepository {
    Optional<VendorQuote> findById(UUID id);

    Optional<VendorQuote> findByIdempotencyKey(UUID idempotencyKey);

    Optional<VendorQuote> findByRfqIdAndVendorId(UUID rfqId, UUID vendorId);

    List<VendorQuote> findByRfqId(UUID rfqId);

    void save(VendorQuote quote);

    void updateEvaluation(VendorQuote quote);
}
