package com.jackson.migration.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies stable human-readable execution timer formatting. */
class ElapsedTimeTest {
    @Test
    void formatsHoursMinutesSecondsAndMilliseconds() {
        assertEquals("01:02:03.456", ElapsedTime.format(Duration.ofHours(1)
                .plusMinutes(2).plusSeconds(3).plusMillis(456)));
    }

    @Test
    void normalizesNegativeDurationsToZero() {
        assertEquals("00:00:00.000", ElapsedTime.format(Duration.ofMillis(-1)));
    }
}
