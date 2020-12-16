package com.dedicatedcode.paperspace;

import java.util.UUID;

public final class TestHelper {
    private TestHelper() {
    }

    public static String rand(String prefix) {
        return prefix + UUID.randomUUID();
    }

    public static String rand() {
        return rand("");
    }
}
