package com.eprocure.vendor.infrastructure.persistence.repository;

import com.eprocure.vendor.domain.model.VendorQuote;
import com.eprocure.vendor.domain.repository.VendorQuoteRepository;
import com.eprocure.vendor.infrastructure.persistence.entity.VendorQuoteDbEntity;
import com.eprocure.vendor.infrastructure.persistence.entity.VendorQuoteLineItemDbEntity;
import com.eprocure.vendor.infrastructure.persistence.mapper.VendorQuoteMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class VendorQuoteRepositoryImpl implements VendorQuoteRepository {
    private final VendorQuoteMapper quoteMapper;
    private final ObjectMapper objectMapper;

    public VendorQuoteRepositoryImpl(
            VendorQuoteMapper quoteMapper,
            @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.quoteMapper = quoteMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<VendorQuote> findById(UUID id) {
        return quoteMapper.findQuoteById(id).map(this::hydrate);
    }

    @Override
    public Optional<VendorQuote> findByIdempotencyKey(UUID idempotencyKey) {
        return quoteMapper.findQuoteByIdempotencyKey(idempotencyKey).map(this::hydrate);
    }

    @Override
    public Optional<VendorQuote> findByRfqIdAndVendorId(UUID rfqId, UUID vendorId) {
        return quoteMapper.findByRfqIdAndVendorId(rfqId, vendorId).map(this::hydrate);
    }

    @Override
    public List<VendorQuote> findByRfqId(UUID rfqId) {
        return quoteMapper.findByRfqId(rfqId).stream()
                .map(this::hydrate)
                .toList();
    }

    @Override
    public void save(VendorQuote quote) {
        quoteMapper.insertQuote(VendorQuoteDbEntity.from(quote));
        quote.lineItems().stream()
                .map(item -> VendorQuoteLineItemDbEntity.from(quote.id(), item, quote.createdBy()))
                .forEach(quoteMapper::insertLineItem);
    }

    @Override
    public void updateEvaluation(VendorQuote quote) {
        quoteMapper.updateEvaluation(VendorQuoteDbEntity.from(quote));
    }

    private VendorQuote hydrate(VendorQuoteDbEntity entity) {
        entity.setLineItems(quoteMapper.findLineItemsByQuoteId(entity.getId()));
        return objectMapper.convertValue(entity, VendorQuote.class);
    }
}
