package com.eprocure.vendor.infrastructure.persistence.repository;

import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.repository.VendorFilter;
import com.eprocure.vendor.domain.repository.VendorRepository;
import com.eprocure.vendor.infrastructure.persistence.entity.VendorContactDbEntity;
import com.eprocure.vendor.infrastructure.persistence.entity.VendorDbEntity;
import com.eprocure.vendor.infrastructure.persistence.mapper.VendorMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class VendorRepositoryImpl implements VendorRepository {
    private final VendorMapper vendorMapper;
    private final ObjectMapper objectMapper;

    public VendorRepositoryImpl(
            VendorMapper vendorMapper,
            @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.vendorMapper = vendorMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public String nextVendorCode() {
        return vendorMapper.nextVendorCode();
    }

    @Override
    public boolean existsByTaxCode(String taxCode) {
        return vendorMapper.existsByTaxCode(taxCode);
    }

    @Override
    public Optional<Vendor> findById(UUID id) {
        return vendorMapper.findVendorById(id).map(this::hydrate);
    }

    @Override
    public Optional<Vendor> findByIdempotencyKey(UUID idempotencyKey) {
        return vendorMapper.findVendorByIdempotencyKey(idempotencyKey).map(this::hydrate);
    }

    @Override
    public List<Vendor> findByFilter(VendorFilter filter) {
        return vendorMapper.findByFilter(filter).stream()
                .map(this::hydrate)
                .toList();
    }

    @Override
    public long countByFilter(VendorFilter filter) {
        return vendorMapper.countByFilter(filter);
    }

    @Override
    public void save(Vendor vendor) {
        vendorMapper.insertVendor(VendorDbEntity.from(vendor));
        vendor.contacts().stream()
                .map(contact -> VendorContactDbEntity.from(vendor.id(), contact, vendor.createdBy()))
                .forEach(vendorMapper::insertContact);
    }

    @Override
    public void updateApproval(Vendor vendor) {
        vendorMapper.updateApproval(VendorDbEntity.from(vendor));
    }

    private Vendor hydrate(VendorDbEntity entity) {
        entity.setContacts(vendorMapper.findContactsByVendorId(entity.getId()));
        entity.setScorecard(vendorMapper.findScoreByVendorId(entity.getId()).orElse(null));
        return objectMapper.convertValue(entity, Vendor.class);
    }
}
