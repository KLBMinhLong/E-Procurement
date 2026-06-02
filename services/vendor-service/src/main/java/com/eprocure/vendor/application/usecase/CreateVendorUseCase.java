package com.eprocure.vendor.application.usecase;

import com.eprocure.vendor.application.port.in.CreateVendorCommand;
import com.eprocure.vendor.application.port.in.VendorContactCommand;
import com.eprocure.vendor.application.service.IdempotencyService;
import com.eprocure.vendor.application.service.VendorDetailView;
import com.eprocure.vendor.application.service.VendorMutationResult;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorContact;
import com.eprocure.vendor.domain.repository.VendorRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateVendorUseCase {
    private static final Logger log = LogManager.getLogger(CreateVendorUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "vendor-create";

    private final VendorRepository vendorRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public CreateVendorUseCase(
            VendorRepository vendorRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.vendorRepository = vendorRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public VendorMutationResult execute(CreateVendorCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                VendorDetailView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit CreateVendor | userId={} | key={}",
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return VendorMutationResult.replayed(cached.get());
        }

        UUID key = UUID.fromString(idempotencyKey);
        var existing = vendorRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            VendorDetailView view = VendorDetailView.from(existing.get());
            idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
            return VendorMutationResult.replayed(view);
        }

        if (vendorRepository.existsByTaxCode(command.taxCode())) {
            throw new BusinessException(ErrorCode.VND_002);
        }

        log.info("[ACTION] Start CreateVendor | userId={}", LogMaskingUtil.maskId(command.actorId()));
        Vendor vendor = Vendor.create(
                UUID.randomUUID(),
                vendorRepository.nextVendorCode(),
                command.name(),
                command.taxCode(),
                command.email(),
                command.phone(),
                command.addressStreet(),
                command.addressDistrict(),
                command.addressCity(),
                command.addressCountry(),
                command.categories(),
                toContacts(command.contacts()),
                command.notes(),
                command.actorId(),
                key,
                Instant.now(clock));
        vendorRepository.save(vendor);

        VendorDetailView view = VendorDetailView.from(vendorRepository.findById(vendor.id()).orElse(vendor));
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete CreateVendor | vendorId={}", LogMaskingUtil.maskId(vendor.id()));
        return VendorMutationResult.fresh(view);
    }

    private List<VendorContact> toContacts(List<VendorContactCommand> contacts) {
        return contacts.stream()
                .map(contact -> VendorContact.create(
                        contact.name(),
                        contact.role(),
                        contact.email(),
                        contact.phone(),
                        contact.primary()))
                .toList();
    }
}
