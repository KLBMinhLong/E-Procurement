package com.eprocure.approval.application.service;

import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SlaDeadlineCalculator {
    private final BusinessHoursCalendar calendar;
    private final Clock clock;

    @Autowired
    public SlaDeadlineCalculator(
            @Value("${eprocure.approval.sla.zone-id:Asia/Ho_Chi_Minh}") String zoneId,
            @Value("${eprocure.approval.sla.business-hours-start:08:00}") String businessHoursStart,
            @Value("${eprocure.approval.sla.business-hours-end:17:30}") String businessHoursEnd,
            @Value("${eprocure.approval.sla.business-days:MON,TUE,WED,THU,FRI}") String businessDays) {
        this(BusinessHoursCalendar.from(zoneId, businessHoursStart, businessHoursEnd, businessDays), Clock.systemUTC());
    }

    public SlaDeadlineCalculator(BusinessHoursCalendar calendar, Clock clock) {
        this.calendar = Objects.requireNonNull(calendar, "calendar must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Instant now() {
        return clock.instant();
    }

    public Instant calculateDeadline(Instant assignedAt, int slaHours, PurchaseRequestPriority priority) {
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        if (slaHours < 1) {
            throw new IllegalArgumentException("slaHours must be positive");
        }
        if (priority == PurchaseRequestPriority.EMERGENCY) {
            return assignedAt.plus(Duration.ofHours(slaHours));
        }
        return addBusinessMinutes(assignedAt, slaHours * 60L);
    }

    private Instant addBusinessMinutes(Instant assignedAt, long minutesToAdd) {
        ZonedDateTime cursor = moveToBusinessTime(assignedAt.atZone(calendar.zoneId()));
        long remainingMinutes = minutesToAdd;

        while (remainingMinutes > 0) {
            ZonedDateTime businessEnd = cursor.with(calendar.endTime());
            long availableMinutes = Duration.between(cursor, businessEnd).toMinutes();
            if (remainingMinutes <= availableMinutes) {
                return cursor.plusMinutes(remainingMinutes).toInstant();
            }
            remainingMinutes -= availableMinutes;
            cursor = nextBusinessStart(cursor.plusDays(1));
        }

        return cursor.toInstant();
    }

    private ZonedDateTime moveToBusinessTime(ZonedDateTime dateTime) {
        if (!calendar.isBusinessDay(dateTime.getDayOfWeek())) {
            return nextBusinessStart(dateTime);
        }
        if (dateTime.toLocalTime().isBefore(calendar.startTime())) {
            return dateTime.with(calendar.startTime());
        }
        if (!dateTime.toLocalTime().isBefore(calendar.endTime())) {
            return nextBusinessStart(dateTime.plusDays(1));
        }
        return dateTime;
    }

    private ZonedDateTime nextBusinessStart(ZonedDateTime from) {
        LocalDate candidateDate = from.toLocalDate();
        while (!calendar.isBusinessDay(candidateDate.getDayOfWeek())) {
            candidateDate = candidateDate.plusDays(1);
        }
        return candidateDate.atTime(calendar.startTime()).atZone(calendar.zoneId());
    }
}
