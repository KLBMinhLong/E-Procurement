package com.eprocure.vendor.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RfqTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID PR_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void should_create_published_rfq_when_valid_snapshot() {
        Rfq rfq = sampleRfq();

        assertThat(rfq.status()).isEqualTo(RfqStatus.PUBLISHED);
        assertThat(rfq.lineItems()).hasSize(1);
        assertThat(rfq.invitations()).hasSize(2);
    }

    @Test
    void should_close_rfq_when_published() {
        Rfq closed = sampleRfq().close(ACTOR_ID, NOW.plusSeconds(3600));

        assertThat(closed.status()).isEqualTo(RfqStatus.CLOSED);
        assertThat(closed.closedAt()).isEqualTo(NOW.plusSeconds(3600));
    }

    @Test
    void should_throw_when_less_than_two_invitations() {
        assertThatThrownBy(() -> Rfq.create(
                UUID.randomUUID(),
                "RFQ-2026-01-00001",
                PR_ID,
                "PR-2026-00001",
                "Laptop RFQ",
                NOW.plusSeconds(86400),
                List.of(sampleLineItem()),
                List.of(sampleInvitation("80000000-0000-0000-0000-000000000101")),
                null,
                ACTOR_ID,
                IDEMPOTENCY_KEY,
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 vendor");
    }

    private Rfq sampleRfq() {
        return Rfq.create(
                UUID.fromString("81000000-0000-0000-0000-000000000001"),
                "RFQ-2026-01-00001",
                PR_ID,
                "PR-2026-00001",
                "Laptop RFQ",
                NOW.plusSeconds(86400),
                List.of(sampleLineItem()),
                List.of(
                        sampleInvitation("80000000-0000-0000-0000-000000000101"),
                        sampleInvitation("80000000-0000-0000-0000-000000000102")),
                null,
                ACTOR_ID,
                IDEMPOTENCY_KEY,
                NOW);
    }

    private RfqLineItem sampleLineItem() {
        return RfqLineItem.create(
                UUID.randomUUID(),
                "Laptop Dell",
                "IT_EQUIPMENT",
                new BigDecimal("2.0000"),
                "cai",
                "16GB RAM");
    }

    private RfqInvitation sampleInvitation(String vendorId) {
        return RfqInvitation.create(UUID.fromString(vendorId), "Vendor " + vendorId.substring(vendorId.length() - 3), NOW);
    }
}
