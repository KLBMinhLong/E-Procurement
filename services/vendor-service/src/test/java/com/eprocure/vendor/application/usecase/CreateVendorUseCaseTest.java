package com.eprocure.vendor.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.vendor.application.port.in.CreateVendorCommand;
import com.eprocure.vendor.application.port.in.VendorContactCommand;
import com.eprocure.vendor.application.service.IdempotencyService;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
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

class CreateVendorUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final String KEY = "11111111-1111-4111-8111-111111111111";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final FakeVendorRepository vendorRepository = new FakeVendorRepository();
    private final FakeIdempotencyService idempotencyService = new FakeIdempotencyService();
    private final CreateVendorUseCase useCase = new CreateVendorUseCase(vendorRepository, idempotencyService, CLOCK);

    @Test
    void should_create_pending_vendor_when_valid_command() {
        var result = useCase.execute(validCommand(), KEY);

        assertThat(vendorRepository.saved).isNotNull();
        assertThat(vendorRepository.saved.status()).isEqualTo(VendorStatus.PENDING);
        assertThat(vendorRepository.saved.onApprovedVendorList()).isFalse();
        assertThat(vendorRepository.saved.vendorCode()).isEqualTo("VND-100");
        assertThat(result.replayed()).isFalse();
        assertThat(idempotencyService.savedOperation).isEqualTo("vendor-create");
    }

    @Test
    void should_throw_vnd_002_when_tax_code_exists() {
        vendorRepository.taxCodeExists = true;

        assertThatThrownBy(() -> useCase.execute(validCommand(), KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VND_002);
    }

    private CreateVendorCommand validCommand() {
        return new CreateVendorCommand(
                ACTOR_ID,
                "IT Vendor",
                "0100999999",
                "sales@example.com",
                "0900000000",
                null,
                null,
                "Ha Noi",
                "Vietnam",
                List.of("IT_EQUIPMENT"),
                List.of(new VendorContactCommand("Sales", "Manager", "sales@example.com", "0900000000", true)),
                null);
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
        private boolean taxCodeExists;
        private Vendor saved;

        @Override
        public String nextVendorCode() {
            return "VND-100";
        }

        @Override
        public boolean existsByTaxCode(String taxCode) {
            return taxCodeExists;
        }

        @Override
        public Optional<Vendor> findById(UUID id) {
            return Optional.ofNullable(saved).filter(vendor -> vendor.id().equals(id));
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
            this.saved = vendor;
        }

        @Override
        public void updateApproval(Vendor vendor) {
        }
    }
}
