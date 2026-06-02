package com.eprocure.vendor.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.vendor.application.port.in.ApproveVendorCommand;
import com.eprocure.vendor.application.service.IdempotencyService;
import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorStatus;
import com.eprocure.vendor.domain.repository.VendorFilter;
import com.eprocure.vendor.domain.repository.VendorRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApproveVendorUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID VENDOR_ID = UUID.fromString("80000000-0000-0000-0000-000000000999");
    private static final UUID IDEMPOTENCY_UUID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final String KEY = IDEMPOTENCY_UUID.toString();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final FakeVendorRepository vendorRepository = new FakeVendorRepository(samplePendingVendor());
    private final FakeIdempotencyService idempotencyService = new FakeIdempotencyService();
    private final ApproveVendorUseCase useCase = new ApproveVendorUseCase(vendorRepository, idempotencyService, CLOCK);

    @Test
    void should_approve_vendor_when_pending() {
        var result = useCase.execute(new ApproveVendorCommand(ACTOR_ID, VENDOR_ID, null), KEY);

        assertThat(vendorRepository.updated).isNotNull();
        assertThat(vendorRepository.updated.status()).isEqualTo(VendorStatus.APPROVED);
        assertThat(vendorRepository.updated.onApprovedVendorList()).isTrue();
        assertThat(result.replayed()).isFalse();
        assertThat(idempotencyService.savedOperation).isEqualTo("vendor-approve");
    }

    private static Vendor samplePendingVendor() {
        return Vendor.create(
                VENDOR_ID,
                "VND-999",
                "IT Vendor",
                "0100999999",
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

    private static final class FakeVendorRepository implements VendorRepository {
        private Vendor current;
        private Vendor updated;

        private FakeVendorRepository(Vendor current) {
            this.current = current;
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
            return Optional.ofNullable(current).filter(vendor -> vendor.id().equals(id));
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
            this.current = vendor;
        }

        @Override
        public void updateApproval(Vendor vendor) {
            this.updated = vendor;
            this.current = vendor;
        }
    }
}
