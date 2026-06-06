package com.eprocure.finance.infrastructure.persistence.repository;

import com.eprocure.finance.domain.model.Invoice;
import com.eprocure.finance.domain.model.InvoiceLineItem;
import com.eprocure.finance.domain.model.InvoiceStatus;
import com.eprocure.finance.domain.model.MatchStatus;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.domain.repository.InvoiceFilter;
import com.eprocure.finance.domain.repository.InvoiceRepository;
import com.eprocure.finance.infrastructure.persistence.entity.InvoiceDbEntity;
import com.eprocure.finance.infrastructure.persistence.entity.InvoiceLineItemDbEntity;
import com.eprocure.finance.infrastructure.persistence.mapper.InvoiceMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceRepositoryImpl implements InvoiceRepository {
    private final InvoiceMapper invoiceMapper;

    public InvoiceRepositoryImpl(InvoiceMapper invoiceMapper) {
        this.invoiceMapper = invoiceMapper;
    }

    @Override
    public Optional<Invoice> findById(UUID invoiceId) {
        return invoiceMapper.findHeaderById(invoiceId).map(this::toDomain);
    }

    @Override
    public Optional<Invoice> findByIdempotencyKey(UUID idempotencyKey) {
        return invoiceMapper.findHeaderByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public Optional<Invoice> findByIdAndMatchIdempotencyKey(UUID invoiceId, UUID idempotencyKey) {
        return invoiceMapper.findHeaderByIdAndMatchIdempotencyKey(invoiceId, idempotencyKey).map(this::toDomain);
    }

    @Override
    public Optional<Invoice> findByIdAndApprovalIdempotencyKey(UUID invoiceId, UUID idempotencyKey) {
        return invoiceMapper.findHeaderByIdAndApprovalIdempotencyKey(invoiceId, idempotencyKey).map(this::toDomain);
    }

    @Override
    public Optional<Invoice> findByIdAndDisputeIdempotencyKey(UUID invoiceId, UUID idempotencyKey) {
        return invoiceMapper.findHeaderByIdAndDisputeIdempotencyKey(invoiceId, idempotencyKey).map(this::toDomain);
    }

    @Override
    public Optional<Invoice> findByVendorIdAndInvoiceNumber(UUID vendorId, String invoiceNumber) {
        return invoiceMapper.findHeaderByVendorIdAndInvoiceNumber(vendorId, invoiceNumber).map(this::toDomain);
    }

    @Override
    public List<Invoice> findByFilter(InvoiceFilter filter) {
        List<InvoiceDbEntity> headers = invoiceMapper.findHeadersByFilter(filter);
        if (headers.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<InvoiceLineItemDbEntity>> lineItems = invoiceMapper.findLineItemsByInvoiceIds(
                        headers.stream().map(InvoiceDbEntity::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(InvoiceLineItemDbEntity::getInvoiceId));
        return headers.stream()
                .map(header -> toDomain(header, lineItems.getOrDefault(header.getId(), List.of())))
                .toList();
    }

    @Override
    public long countByFilter(InvoiceFilter filter) {
        return invoiceMapper.countByFilter(filter);
    }

    @Override
    public void insert(Invoice invoice) {
        invoiceMapper.insertHeader(InvoiceDbEntity.from(invoice));
        invoice.lineItems().stream()
                .map(lineItem -> InvoiceLineItemDbEntity.from(invoice.id(), lineItem))
                .forEach(entity -> invoiceMapper.insertLineItem(entity, invoice.createdBy()));
    }

    @Override
    public void updateMatchResult(
            UUID invoiceId,
            InvoiceStatus status,
            MatchStatus poMatchStatus,
            MatchStatus grMatchStatus,
            java.math.BigDecimal qtyVariance,
            Money priceVariance,
            java.time.Instant matchedAt,
            UUID matchedBy,
            UUID idempotencyKey) {
        invoiceMapper.updateMatchResult(
                invoiceId,
                status,
                poMatchStatus,
                grMatchStatus,
                qtyVariance,
                priceVariance == null ? null : priceVariance.amount(),
                matchedAt,
                matchedBy,
                idempotencyKey);
    }

    @Override
    public void markApproved(UUID invoiceId, UUID approvedBy, java.time.Instant approvedAt, UUID idempotencyKey) {
        invoiceMapper.markApproved(invoiceId, approvedBy, approvedAt, idempotencyKey);
    }

    @Override
    public void markDisputed(UUID invoiceId, String reason, UUID disputedBy, java.time.Instant disputedAt, UUID idempotencyKey) {
        invoiceMapper.markDisputed(invoiceId, reason, disputedBy, disputedAt, idempotencyKey);
    }

    @Override
    public boolean markPaid(UUID invoiceId, UUID paidBy, java.time.Instant paidAt) {
        return invoiceMapper.markPaid(invoiceId, paidBy, paidAt) == 1;
    }

    private Invoice toDomain(InvoiceDbEntity header) {
        return toDomain(header, invoiceMapper.findLineItemsByInvoiceId(header.getId()));
    }

    private Invoice toDomain(InvoiceDbEntity header, List<InvoiceLineItemDbEntity> lineItems) {
        return new Invoice(
                header.getId(),
                header.getInvoiceNumber(),
                header.getVendorId(),
                header.getVendorName(),
                header.getPoId(),
                header.getPoNumber(),
                lineItems.stream().map(this::toDomainLineItem).toList(),
                new Money(header.getSubtotal(), header.getCurrency()),
                new Money(header.getTaxAmount(), header.getCurrency()),
                new Money(header.getTotalAmount(), header.getCurrency()),
                header.getInvoiceDate(),
                header.getDueDate(),
                header.getStatus(),
                header.getPoMatchStatus(),
                header.getGrMatchStatus(),
                header.getQtyVariance(),
                header.getPriceVariance() == null ? null : new Money(header.getPriceVariance(), header.getCurrency()),
                header.getMatchedAt(),
                header.getMatchedBy(),
                header.getApprovedBy(),
                header.getApprovedAt(),
                header.getCreatedAt(),
                header.getCreatedBy(),
                header.getIdempotencyKey(),
                header.getMatchIdempotencyKey());
    }

    private InvoiceLineItem toDomainLineItem(InvoiceLineItemDbEntity entity) {
        return new InvoiceLineItem(
                entity.getId(),
                entity.getLineNumber(),
                entity.getPoLineItemId(),
                entity.getDescription(),
                entity.getQuantity(),
                new Money(entity.getUnitPrice(), entity.getCurrency()),
                entity.getTaxRate(),
                new Money(entity.getTaxAmount(), entity.getCurrency()),
                new Money(entity.getTotalPrice(), entity.getCurrency()));
    }
}
