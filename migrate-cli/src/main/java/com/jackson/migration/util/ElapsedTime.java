package com.jackson.migration.util;

import java.time.Duration;
import java.time.Instant;

/**
 * Formats elapsed execution time consistently across Workbench commands and external-tool runs.
 */
public final class ElapsedTime {
    private ElapsedTime() {}

    /** Returns the elapsed duration from {@code start} to now. */
    public static Duration since(Instant start) {
        return Duration.between(start, Instant.now());
    }

    /**
     * Formats a duration as {@code HH:MM:SS.mmm}. Negative durations are normalized to zero.
     */
    public static String format(Duration duration) {
        if (duration == null || duration.isNegative()) duration = Duration.ZERO;
        long millis = duration.toMillis();
        long hours = millis / 3_600_000;
        millis %= 3_600_000;
        long minutes = millis / 60_000;
        millis %= 60_000;
        long seconds = millis / 1_000;
        long remainderMillis = millis % 1_000;
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, remainderMillis);
    }
}
