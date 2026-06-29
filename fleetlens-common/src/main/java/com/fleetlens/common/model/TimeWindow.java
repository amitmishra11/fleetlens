package com.fleetlens.common.model;

import java.time.Instant;

public record TimeWindow(Instant from, Instant to) {

    public boolean contains(Instant instant) {
        return !instant.isBefore(from) && !instant.isAfter(to);
    }

    public static TimeWindow lastMinutes(Instant reference, long minutes) {
        return new TimeWindow(reference.minusSeconds(minutes * 60), reference);
    }
}
