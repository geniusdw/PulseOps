package com.pulseops.common;

import com.pulseops.common.error.ValidationException;

/** Helpers for parsing the {@code PREFIX-<id>} public identifiers. */
public final class PublicIds {

    private PublicIds() {
    }

    /**
     * Accepts either {@code "EVT-123"} / {@code "INC-9"} style ids or a bare
     * number, and returns the numeric id.
     */
    public static long parse(String prefix, String raw) {
        String value = raw == null ? "" : raw.trim();
        String withoutPrefix = value.toUpperCase().startsWith(prefix.toUpperCase() + "-")
                ? value.substring(prefix.length() + 1)
                : value;
        try {
            return Long.parseLong(withoutPrefix);
        } catch (NumberFormatException ex) {
            throw new ValidationException("Malformed identifier '" + raw + "' (expected " + prefix + "-<number>)");
        }
    }
}
