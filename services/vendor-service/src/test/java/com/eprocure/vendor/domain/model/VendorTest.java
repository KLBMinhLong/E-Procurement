package com.eprocure.vendor.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VendorTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void should_create_pending_vendor_when_valid_data() {
        Vendor vendor = samplePendingVendor();

        assertThat(vendor.status()).isEqualTo(VendorStatus.PENDING);
        assertThat(vendor.onApprovedVendorList()).isFalse();
        assertThat(vendor.categories()).containsExactly("IT_EQUIPMENT");
    }

    @Test
    void should_approve_vendor_when_pending() {
        Vendor approved = samplePendingVendor().approve(ACTOR_ID, NOW.plusSeconds(60));

        assertThat(approved.status()).isEqualTo(VendorStatus.APPROVED);
        assertThat(approved.onApprovedVendorList()).isTrue();
        assertThat(approved.approvedBy()).isEqualTo(ACTOR_ID);
    }

    @Test
    void should_throw_when_categories_empty() {
        assertThatThrownBy(() -> Vendor.create(
                UUID.randomUUID(),
                "VND-999",
                "No Category Vendor",
                "0100999999",
                "sales@example.com",
                "0900000000",
                null,
                null,
                null,
                "Vietnam",
                List.of(),
                List.of(),
                null,
                ACTOR_ID,
                IDEMPOTENCY_KEY,
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("categories");
    }

    private Vendor samplePendingVendor() {
        return Vendor.create(
                UUID.fromString("80000000-0000-0000-0000-000000000999"),
                "VND-999",
                "IT Vendor",
                "0100999999",
                "sales@example.com",
                "0900000000",
                null,
                null,
                "Ha Noi",
                "Vietnam",
                List.of("it_equipment"),
                List.of(VendorContact.create("Sales", "Manager", "sales@example.com", "0900000000", true)),
                null,
                ACTOR_ID,
                IDEMPOTENCY_KEY,
                NOW);
    }
}
