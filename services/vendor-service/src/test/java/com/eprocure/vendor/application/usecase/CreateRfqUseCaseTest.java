package com.eprocure.vendor.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.vendor.application.port.in.CreateRfqCommand;
import com.eprocure.vendor.application.port.out.PurchaseRequestRfqSourcePort;
import com.eprocure.vendor.application.service.IdempotencyService;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.domain.model.Rfq;
import com.eprocure.vendor.domain.model.RfqStatus;
import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorStatus;
import com.eprocure.vendor.domain.repository.RfqFilter;
import com.eprocure.vendor.domain.repository.RfqRepository;
import com.eprocure.vendor.domain.repository.VendorFilter;
import com.eprocure.vendor.domain.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateRfqUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID PR_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID VENDOR_ID_1 = UUID.fromString("80000000-0000-0000-0000-000000000101");
    private static final UUID VENDOR_ID_2 = UUID.fromString("80000000-0000-0000-0000-000000000102");
    private static final UUID IDEMPOTENCY_UUID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final String KEY = IDEMPOTENCY_UUID.toString();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final FakeRfqRepository rfqRepository = new FakeRfqRepository();
    private final FakeVendorRepository vendorRepository = new FakeVendorRepository();
    private final FakePrSourcePort prSourcePort = new FakePrSourcePort();
    private final FakeIdempotencyService idempotencyService = new FakeIdempotencyService();
    private final CreateRfqUseCase useCase = new CreateRfqUseCase(
            rfqRepository,
            vendorRepository,
            prSourcePort,
            idempotencyService,
            CLOCK);

    @Test
    void should_create_rfq_when_pr_approved_and_vendors_in_avl() {
        vendorRepository.put(approvedVendor(VENDOR_ID_1, "Vendor One"));
        vendorRepository.put(approvedVendor(VENDOR_ID_2, "Vendor Two"));

        var result = useCase.execute(validCommand(), KEY);

        assertThat(rfqRepository.saved).isNotNull();
        assertThat(rfqRepository.saved.status()).isEqualTo(RfqStatus.PUBLISHED);
        assertThat(rfqRepository.saved.invitations()).hasSize(2);
        assertThat(rfqRepository.saved.lineItems()).hasSize(1);
        assertThat(result.replayed()).isFalse();
        assertThat(idempotencyService.savedOperation).isEqualTo("rfq-create");
    }

    @Test
    void should_throw_vnd_009_when_pr_not_approved() {
        prSourcePort.status = "PENDING_APPROVAL";
        vendorRepository.put(approvedVendor(VENDOR_ID_1, "Vendor One"));
        vendorRepository.put(approvedVendor(VENDOR_ID_2, "Vendor Two"));

        assertThatThrownBy(() -> useCase.execute(validCommand(), KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VND_009);
    }

    @Test
    void should_throw_vnd_008_when_vendor_not_in_avl() {
        vendorRepository.put(pendingVendor(VENDOR_ID_1, "Vendor One"));
        vendorRepository.put(approvedVendor(VENDOR_ID_2, "Vendor Two"));

        assertThatThrownBy(() -> useCase.execute(validCommand(), KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VND_008);
    }

    private CreateRfqCommand validCommand() {
        return new CreateRfqCommand(
                ACTOR_ID,
                PR_ID,
                "Laptop RFQ",
                Instant.parse("2026-01-05T00:00:00Z"),
                List.of(VENDOR_ID_1, VENDOR_ID_2),
                "Need competitive quotes");
    }

    private Vendor approvedVendor(UUID id, String name) {
        return pendingVendor(id, name).approve(ACTOR_ID, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private Vendor pendingVendor(UUID id, String name) {
        return Vendor.create(
                id,
                "VND-" + id.toString().substring(id.toString().length() - 3),
                name,
                "TAX" + id.toString().substring(id.toString().length() - 3),
                "sales@example.com",
                "0900000000",
                null,
                null,
                "Ha Noi",
                "Vietnam",
                List.of("IT_EQUIPMENT"),
                List.of(),
                null,
                ACTOR_ID,
                IDEMPOTENCY_UUID,
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
        private String savedOperation;

        private FakeIdempotencyService() {
            super(null, null);
        }

        @Override
        public void verify(String idempotencyKey) {
        }

        @Override
        public <T> Optional<T> find(String operation, UUID actorId, String idempotencyKey, Class<T> type) {
            return Optional.empty();
        }

        @Override
        public void save(String operation, UUID actorId, String idempotencyKey, Object response) {
            this.savedOperation = operation;
        }
    }

    private static final class FakePrSourcePort implements PurchaseRequestRfqSourcePort {
        private String status = "APPROVED";

        @Override
        public PurchaseRequestRfqSource getSource(UUID purchaseRequestId) {
            return new PurchaseRequestRfqSource(
                    purchaseRequestId,
                    "PR-2026-00001",
                    status,
                    List.of(new PurchaseRequestRfqLineItem(
                            UUID.randomUUID(),
                            "Laptop Dell",
                            "IT_EQUIPMENT",
                            new BigDecimal("2.0000"),
                            "cai",
                            "16GB RAM")));
        }
    }

    private static final class FakeRfqRepository implements RfqRepository {
        private Rfq saved;

        @Override
        public String nextRfqNumber() {
            return "RFQ-2026-01-00001";
        }

        @Override
        public Optional<Rfq> findById(UUID id) {
            return Optional.ofNullable(saved).filter(rfq -> rfq.id().equals(id));
        }

        @Override
        public Optional<Rfq> findByIdempotencyKey(UUID idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public List<Rfq> findByFilter(RfqFilter filter) {
            return List.of();
        }

        @Override
        public long countByFilter(RfqFilter filter) {
            return 0;
        }

        @Override
        public void save(Rfq rfq) {
            this.saved = rfq;
        }

        @Override
        public void updateStatus(Rfq rfq) {
            this.saved = rfq;
        }
    }

    private static final class FakeVendorRepository implements VendorRepository {
        private final Map<UUID, Vendor> vendors = new HashMap<>();

        void put(Vendor vendor) {
            vendors.put(vendor.id(), vendor);
        }

        @Override
        public String nextVendorCode() {
            return "VND-100";
        }

        @Override
        public boolean existsByTaxCode(String taxCode) {
            return false;
        }

        @Override
        public Optional<Vendor> findById(UUID id) {
            return Optional.ofNullable(vendors.get(id));
        }

        @Override
        public Optional<Vendor> findByIdempotencyKey(UUID idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public List<Vendor> findByFilter(VendorFilter filter) {
            return List.of();
        }

        @Override
        public long countByFilter(VendorFilter filter) {
            return 0;
        }

        @Override
        public void save(Vendor vendor) {
            vendors.put(vendor.id(), vendor);
        }

        @Override
        public void updateApproval(Vendor vendor) {
            vendors.put(vendor.id(), vendor);
        }
    }
}
