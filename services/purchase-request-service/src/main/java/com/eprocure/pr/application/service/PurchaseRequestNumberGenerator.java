package com.eprocure.pr.application.service;

import com.eprocure.pr.application.port.out.PrNumberSequencePort;
import java.time.Clock;
import java.time.YearMonth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PurchaseRequestNumberGenerator {
    private final PrNumberSequencePort sequencePort;
    private final Clock clock;
    private final String prefix;

    public PurchaseRequestNumberGenerator(
            PrNumberSequencePort sequencePort,
            Clock clock,
            @Value("${eprocure.pr.number-prefix:PR}") String prefix) {
        this.sequencePort = sequencePort;
        this.clock = clock;
        this.prefix = prefix == null || prefix.isBlank() ? "PR" : prefix.trim().toUpperCase();
    }

    public String next() {
        YearMonth yearMonth = YearMonth.now(clock);
        long sequence = sequencePort.next();
        if (sequence > 99999) {
            throw new IllegalStateException("PR number sequence exceeded 99999");
        }
        return "%s-%04d-%02d-%05d".formatted(prefix, yearMonth.getYear(), yearMonth.getMonthValue(), sequence);
    }
}
