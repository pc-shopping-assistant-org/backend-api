package com.ecm.server.mapper;

import java.util.Locale;

public final class UserMappingSupport {
    private UserMappingSupport() {
    }

    public static String firstName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim();
        int split = normalized.lastIndexOf(' ');
        return split <= 0 ? normalized : normalized.substring(0, split).trim();
    }

    public static String lastName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim();
        int split = normalized.lastIndexOf(' ');
        return split <= 0 ? "" : normalized.substring(split + 1).trim();
    }

    public static String fullName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

    /**
     * Persist enum-like profile values in the exact uppercase form required by
     * the PostgreSQL CHECK constraints. Blank optional values remain null.
     */
    public static String normalizeCustomerGender(String gender) {
        return normalizeGender(gender, null);
    }

    public static String normalizeEmployeeGender(String gender) {
        return normalizeGender(gender, null);
    }

    private static String normalizeGender(String gender, String fallback) {
        if (gender == null || gender.isBlank()) {
            return fallback;
        }
        return gender.trim().toUpperCase(Locale.ROOT);
    }
}
