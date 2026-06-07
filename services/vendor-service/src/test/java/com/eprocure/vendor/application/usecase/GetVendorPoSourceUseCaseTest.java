package com.eprocure.vendor.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorContact;
import com.eprocure.vendor.domain.repository.VendorFilter;
import com.eprocure.vendor.domain.repository.VendorRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetVendorPoSourceUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID VENDOR_ID = UUID.fromString("80000000-0000-0000-0000-000000000999");
    private static final UUID IDEMPOTENCY_UUID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    private final FakeVendorRepository vendorRepository = new FakeVendorRepository();
    private final GetVendorPoSourceUseCase useCase = new GetVendorPoSourceUseCase(vendorRepository);

    @Test
    void should_return_vendor_po_source_when_vendor_approved_and_on_avl() {
        Vendor vendor = pendingVendor().approve(ACTOR_ID, Instant.parse("2026-01-02T00:00:00Z"));
        vendorRepository.current = vendor;

        var result = useCase.execute(VENDOR_ID);

        assertThat(result.id()).isEqualTo(VENDOR_ID);
        assertThat(result.vendorCode()).isEqualTo("VND-999");
        assertThat(result.name()).isEqualTo("IT Vendor");
        assertThat(result.email()).isEqualTo("sales@example.com");
        assertThat(result.onApprovedVendorList()).isTrue();
        assertThat(result.categories()).containsExactly("IT_EQUIPMENT");
        assertThat(result.primaryContact()).isNotNull();
        assertThat(result.primaryContact().name()).isEqualTo("Primary Sales");
        assertThat(result.primaryContact().email()).isEqualTo("primary@example.com");
    }

    @Test
    void should_throw_vnd_008_when_vendor_not_approved_or_not_on_avl() {
        vendorRepository.current = pendingVendor();

        assertThatThrownBy(() -> useCase.execute(VENDOR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VND_008);
    }

    @Test
    void should_throw_vnd_001_when_vendor_missing() {
        assertThatThrownBy(() -> useCase.execute(VENDOR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VND_001);
    }

    private static Vendor pendingVendor() {
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
                List.of(
                        VendorContact.create("Secondary Sales", null, "secondary@example.com", "0900000002", false),
                        VendorContact.create("Primary Sales", "Sales Manager", "primary@example.com", "0900000001", true)),
                null,
                ACTOR_ID,
                IDEMPOTENCY_UUID,
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static final class FakeVendorRepository implements VendorRepository {
        private Vendor current;

        @Override public String nextVendorCode() { return "VND-100"; }
        @Override public boolean existsByTaxCode(String taxCode) { return false; }

        @Override
        public Optional<Vendor> findById(UUID id) {
            return Optional.ofNullable(current).filter(vendor -> vendor.id().equals(id));
        }

        @Override public Optional<Vendor> findByIdempotencyKey(UUID idempotencyKey) { return Optional.empty(); }
        @Override public List<Vendor> findByFilter(VendorFilter filter) { return List.of(); }
        @Override public long countByFilter(VendorFilter filter) { return 0; }
        @Override public void save(Vendor vendor) { this.current = vendor; }
        @Override public void updateApproval(Vendor vendor) { this.current = vendor; }
    }
}
