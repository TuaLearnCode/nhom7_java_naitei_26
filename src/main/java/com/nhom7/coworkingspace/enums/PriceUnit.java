package com.nhom7.coworkingspace.enums;

import java.util.Locale;

public enum PriceUnit {
    HOUR,
    DAY,
    MONTH;

    public static PriceUnit fromString(String unit) {
        try {
            return fromStringStrict(unit);
        } catch (IllegalArgumentException ex) {
            return HOUR;
        }
    }

    public static PriceUnit fromStringStrict(String unit) {
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Price unit must not be blank");
        }
        String normalized = unit.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("PER_")) {
            normalized = normalized.substring(4);
        }
        return PriceUnit.valueOf(normalized);
    }
}
