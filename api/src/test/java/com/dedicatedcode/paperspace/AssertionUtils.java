package com.dedicatedcode.paperspace;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class AssertionUtils {
    private AssertionUtils() {
    }

    public enum DateTimeComparePrecision {
        SECONDS
    }

    public static void assertDateTimeEquals(LocalDateTime expected, LocalDateTime actual, DateTimeComparePrecision precision) {
        switch (precision) {
            case SECONDS: {
                assertEquals(expected.withNano(0), actual.withNano(0));
                break;
            }
            default:
                fail("Unhandled precision");
        }
    }
}
