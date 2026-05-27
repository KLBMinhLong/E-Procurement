package com.eprocure.approval.application.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record BusinessHoursCalendar(
        ZoneId zoneId,
        LocalTime startTime,
        LocalTime endTime,
        Set<DayOfWeek> businessDays) {

    public BusinessHoursCalendar {
        zoneId = Objects.requireNonNull(zoneId, "zoneId must not be null");
        startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        if (businessDays == null || businessDays.isEmpty()) {
            throw new IllegalArgumentException("businessDays must not be empty");
        }
        businessDays = Set.copyOf(businessDays);
    }

    public static BusinessHoursCalendar from(String zoneId, String startTime, String endTime, String businessDays) {
        return new BusinessHoursCalendar(
                ZoneId.of(zoneId),
                LocalTime.parse(startTime),
                LocalTime.parse(endTime),
                parseBusinessDays(businessDays));
    }

    public boolean isBusinessDay(DayOfWeek dayOfWeek) {
        return businessDays.contains(dayOfWeek);
    }

    private static Set<DayOfWeek> parseBusinessDays(String rawBusinessDays) {
        if (rawBusinessDays == null || rawBusinessDays.isBlank()) {
            throw new IllegalArgumentException("businessDays must not be blank");
        }
        return Arrays.stream(rawBusinessDays.split(","))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .map(BusinessHoursCalendar::parseBusinessDay)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static DayOfWeek parseBusinessDay(String value) {
        return switch (value) {
            case "MON", "MONDAY" -> DayOfWeek.MONDAY;
            case "TUE", "TUESDAY" -> DayOfWeek.TUESDAY;
            case "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
            case "THU", "THURSDAY" -> DayOfWeek.THURSDAY;
            case "FRI", "FRIDAY" -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY" -> DayOfWeek.SATURDAY;
            case "SUN", "SUNDAY" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Unsupported business day: " + value);
        };
    }
}
