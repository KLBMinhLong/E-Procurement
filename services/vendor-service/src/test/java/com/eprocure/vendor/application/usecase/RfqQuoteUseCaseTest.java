package com.eprocure.vendor.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.vendor.application.port.in.AwardRfqCommand;
import com.eprocure.vendor.application.port.in.EvaluateQuoteCommand;
import com.eprocure.vendor.application.port.in.SubmitVendorQuoteCommand;
import com.eprocure.vendor.application.service.IdempotencyService;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.domain.model.Rfq;
import com.eprocure.vendor.domain.model.RfqInvitation;
import com.eprocure.vendor.domain.model.RfqLineItem;
import com.eprocure.vendor.domain.model.RfqStatus;
import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorQuote;
import com.eprocure.vendor.domain.model.VendorQuoteLineItem;
import com.eprocure.vendor.domain.repository.RfqFilter;
import com.eprocure.vendor.domain.repository.RfqRepository;
import com.eprocure.vendor.domain.repository.VendorFilter;
import com.eprocure.vendor.domain.repository.VendorQuoteRepository;
import com.eprocure.vendor.domain.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RfqQuoteUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID RFQ_ID = UUID.fromString("81000000-0000-0000-0000-000000000001");
    private static final UUID RFQ_LINE_ID = UUID.fromString("82000000-0000-0000-0000-000000000001");
    private static final UUID VENDOR_ID = UUID.fromString("80000000-0000-0000-0000-000000000101");
    private static final UUID SECOND_VENDOR_ID = UUID.fromString("80000000-0000-0000-0000-000000000102");
    private static final UUID IDEMPOTENCY_UUID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final String KEY = IDEMPOTENCY_UUID.toString();
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void should_submit_quote_when_vendor_invited_and_rfq_open() {
        FakeRfqRepository rfqRepository = new FakeRfqRepository(sampleRfq(NOW.plusSeconds(86400)));
        FakeVendorRepository vendorRepository = new FakeVendorRepository();
        vendorRepository.put(approvedVendor(VENDOR_ID, "Vendor One"));
        FakeVendorQuoteRepository quoteRepository = new FakeVendorQuoteRepository();
        SubmitVendorQuoteUseCase useCase = new SubmitVendorQuoteUseCase(
                rfqRepository,
                vendorRepository,
                quoteRepository,
                new FakeIdempotencyService(),
                CLOCK);

        var result = useCase.execute(validSubmitCommand(), KEY);

        assertThat(result.view().vendorId()).isEqualTo(VENDOR_ID);
        assertThat(result.view().totalAmount()).isEqualByComparingTo("2000.0000");
        assertThat(rfqRepository.submittedVendorId).isEqualTo(VENDOR_ID);
    }

    @Test
    void should_throw_vnd_007_when_quote_deadline_expired() {
        FakeRfqRepository rfqRepository = new FakeRfqRepository(
                sampleRfq(NOW.minusSeconds(1), NOW.minusSeconds(86400)));
        FakeVendorRepository vendorRepository = new FakeVendorRepository();
        vendorRepository.put(approvedVendor(VENDOR_ID, "Vendor One"));
        SubmitVendorQuoteUseCase useCase = new SubmitVendorQuoteUseCase(
                rfqRepository,
                vendorRepository,
                new FakeVendorQuoteRepository(),
                new FakeIdempotencyService(),
                CLOCK);

        assertThatThrownBy(() -> useCase.execute(validSubmitCommand(), KEY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VND_007);
    }

    @Test
    void should_evaluate_quote_when_quote_belongs_to_rfq() {
        FakeRfqRepository rfqRepository = new FakeRfqRepository(sampleRfq(NOW.plusSeconds(86400)));
        FakeVendorQuoteRepository quoteRepository = new FakeVendorQuoteRepository();
        VendorQuote quote = sampleQuote();
        quoteRepository.save(quote);
        EvaluateQuoteUseCase useCase = new EvaluateQuoteUseCase(
                rfqRepository,
                quoteRepository,
                new FakeIdempotencyService(),
                CLOCK);

        var result = useCase.execute(
                new EvaluateQuoteCommand(ACTOR_ID, RFQ_ID, quote.id(), new BigDecimal("91.50"), "Best value"),
                KEY);

        assertThat(result.view().evaluationScore()).isEqualByComparingTo("91.50");
        assertThat(quoteRepository.quotes.get(quote.id()).evaluationNote()).isEqualTo("Best value");
    }

    @Test
    void should_award_closed_rfq_when_quote_vendor_approved() {
        FakeRfqRepository rfqRepository = new FakeRfqRepository(sampleRfq(NOW.plusSeconds(86400)).close(ACTOR_ID, NOW));
        FakeVendorRepository vendorRepository = new FakeVendorRepository();
        vendorRepository.put(approvedVendor(VENDOR_ID, "Vendor One"));
        FakeVendorQuoteRepository quoteRepository = new FakeVendorQuoteRepository();
        VendorQuote quote = sampleQuote();
        quoteRepository.save(quote);
        AwardRfqUseCase useCase = new AwardRfqUseCase(
                rfqRepository,
                quoteRepository,
                vendorRepository,
                new FakeIdempotencyService(),
                CLOCK);

        var result = useCase.execute(
                new AwardRfqCommand(ACTOR_ID, RFQ_ID, quote.id(), "Gia tot nhat va dap ung nang luc ky thuat"),
                KEY);

        assertThat(result.awardedVendor().id()).isEqualTo(VENDOR_ID);
        assertThat(rfqRepository.current.status()).isEqualTo(RfqStatus.AWARDED);
        assertThat(rfqRepository.current.awardedQuoteId()).isEqualTo(quote.id());
    }

    private SubmitVendorQuoteCommand validSubmitCommand() {
        return new SubmitVendorQuoteCommand(
                ACTOR_ID,
                RFQ_ID,
                VENDOR_ID,
                "VND",
                LocalDate.parse("2026-02-01"),
                List.of(new SubmitVendorQuoteCommand.SubmitVendorQuoteLineItemCommand(
                        RFQ_LINE_ID,
                        new BigDecimal("1000.0000"),
                        7,
                        "12 months")),
                "Net 30",
                "Includes delivery");
    }

    private static Rfq sampleRfq(Instant deadline) {
        return sampleRfq(deadline, NOW);
    }

    private static Rfq sampleRfq(Instant deadline, Instant createdAt) {
        return Rfq.create(
                RFQ_ID,
                "RFQ-2026-01-00001",
                UUID.fromString("50000000-0000-0000-0000-000000000001"),
                "PR-2026-00001",
                "Laptop RFQ",
                deadline,
                List.of(new RfqLineItem(
                        RFQ_LINE_ID,
                        UUID.fromString("51000000-0000-0000-0000-000000000001"),
                        "Laptop Dell",
                        "IT_EQUIPMENT",
                        new BigDecimal("2.0000"),
                        "cai",
                        "16GB RAM")),
                List.of(
                        RfqInvitation.create(VENDOR_ID, "Vendor One", NOW),
                        RfqInvitation.create(SECOND_VENDOR_ID, "Vendor Two", NOW)),
                null,
                ACTOR_ID,
                IDEMPOTENCY_UUID,
                createdAt);
    }

    private static VendorQuote sampleQuote() {
        return VendorQuote.create(
                UUID.fromString("83000000-0000-0000-0000-000000000001"),
                RFQ_ID,
                VENDOR_ID,
                "Vendor One",
                List.of(VendorQuoteLineItem.create(
                        RFQ_LINE_ID,
                        "Laptop Dell",
                        new BigDecimal("2.0000"),
                        new BigDecimal("1000.0000"),
                        "VND",
                        7,
                        "12 months")),
                "VND",
                LocalDate.parse("2026-02-01"),
                "Net 30",
                "Includes delivery",
                ACTOR_ID,
                IDEMPOTENCY_UUID,
                NOW);
    }

    private static Vendor approvedVendor(UUID id, String name) {
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
                        NOW)
                .approve(ACTOR_ID, NOW);
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
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
        }
    }

    private static final class FakeRfqRepository implements RfqRepository {
        private Rfq current;
        private UUID submittedVendorId;

        private FakeRfqRepository(Rfq current) {
            this.current = current;
        }

        @Override
        public String nextRfqNumber() {
            return "RFQ-2026-01-00001";
        }

        @Override
        public Optional<Rfq> findById(UUID id) {
            return Optional.ofNullable(current).filter(rfq -> rfq.id().equals(id));
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
            this.current = rfq;
        }

        @Override
        public void updateStatus(Rfq rfq) {
            this.current = rfq;
        }

        @Override
        public void markInvitationSubmitted(UUID rfqId, UUID vendorId, Instant submittedAt, UUID actorId) {
            this.submittedVendorId = vendorId;
        }
    }

    private static final class FakeVendorQuoteRepository implements VendorQuoteRepository {
        private final Map<UUID, VendorQuote> quotes = new HashMap<>();

        @Override
        public Optional<VendorQuote> findById(UUID id) {
            return Optional.ofNullable(quotes.get(id));
        }

        @Override
        public Optional<VendorQuote> findByIdempotencyKey(UUID idempotencyKey) {
            return quotes.values().stream()
                    .filter(quote -> quote.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public Optional<VendorQuote> findByRfqIdAndVendorId(UUID rfqId, UUID vendorId) {
            return quotes.values().stream()
                    .filter(quote -> quote.rfqId().equals(rfqId) && quote.vendorId().equals(vendorId))
                    .findFirst();
        }

        @Override
        public List<VendorQuote> findByRfqId(UUID rfqId) {
            return quotes.values().stream()
                    .filter(quote -> quote.rfqId().equals(rfqId))
                    .toList();
        }

        @Override
        public void save(VendorQuote quote) {
            quotes.put(quote.id(), quote);
        }

        @Override
        public void updateEvaluation(VendorQuote quote) {
            quotes.put(quote.id(), quote);
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
