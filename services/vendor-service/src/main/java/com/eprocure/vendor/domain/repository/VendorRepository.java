package com.eprocure.vendor.domain.repository;

import com.eprocure.vendor.domain.model.Vendor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository {
    String nextVendorCode();

    boolean existsByTaxCode(String taxCode);

    Optional<Vendor> findById(UUID id);

    Optional<Vendor> findByIdempotencyKey(UUID idempotencyKey);

    List<Vendor> findByFilter(VendorFilter filter);

    long countByFilter(VendorFilter filter);

    void save(Vendor vendor);

    void updateApproval(Vendor vendor);
}
