package com.eprocure.approval.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SlaDeadlineCalculatorTest {
    private static final BusinessHoursCalendar CALENDAR = BusinessHoursCalendar.from(
            "Asia/Ho_Chi_Minh",
            "08:00",
            "17:30",
            "MON,TUE,WED,THU,FRI");

    @Test
    void should_add_business_hours_across_weekend_when_normal_priority() {
        SlaDeadlineCalculator calculator = calculator();
        Instant friday1630Vn = Instant.parse("2026-05-29T09:30:00Z");

        Instant deadline = calculator.calculateDeadline(friday1630Vn, 2, PurchaseRequestPriority.NORMAL);

        assertThat(deadline).isEqualTo(Instant.parse("2026-06-01T02:00:00Z"));
    }

    @Test
    void should_start_from_next_business_day_when_assigned_on_weekend() {
        SlaDeadlineCalculator calculator = calculator();
        Instant saturday1000Vn = Instant.parse("2026-05-30T03:00:00Z");

        Instant deadline = calculator.calculateDeadline(saturday1000Vn, 1, PurchaseRequestPriority.URGENT);

        assertThat(deadline).isEqualTo(Instant.parse("2026-06-01T02:00:00Z"));
    }

    @Test
    void should_start_from_business_open_when_assigned_before_hours() {
        SlaDeadlineCalculator calculator = calculator();
        Instant monday0700Vn = Instant.parse("2026-06-01T00:00:00Z");

        Instant deadline = calculator.calculateDeadline(monday0700Vn, 2, PurchaseRequestPriority.NORMAL);

        assertThat(deadline).isEqualTo(Instant.parse("2026-06-01T03:00:00Z"));
    }

    @Test
    void should_use_continuous_hours_when_emergency_priority() {
        SlaDeadlineCalculator calculator = calculator();
        Instant saturday1000Vn = Instant.parse("2026-05-30T03:00:00Z");

        Instant deadline = calculator.calculateDeadline(saturday1000Vn, 2, PurchaseRequestPriority.EMERGENCY);

        assertThat(deadline).isEqualTo(Instant.parse("2026-05-30T05:00:00Z"));
    }

    private SlaDeadlineCalculator calculator() {
        return new SlaDeadlineCalculator(CALENDAR, Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC));
    }
}
