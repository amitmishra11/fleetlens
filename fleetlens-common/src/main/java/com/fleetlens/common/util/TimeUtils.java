package com.fleetlens.common.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class TimeUtils {

    private TimeUtils() {}

    public static Instant parseIsoOrNow(String iso) {
        if (iso == null || iso.isBlank()) {
            return Instant.now();
        }
        try {
            return DateTimeFormatter.ISO_DATE_TIME.parse(iso, Instant::from);
        } catch (DateTimeParseException e) {
            return Instant.parse(iso);
        }
    }

    public static String toIso(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
