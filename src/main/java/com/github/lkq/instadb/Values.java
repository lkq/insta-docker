package com.github.lkq.instadb;

public class Values {
    public static void requiresNotBlank(String value, String message) {
        if (value == null || value.trim().equals("")) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void requiresTrue(boolean value, String message) {
        if (!value) {
            throw new IllegalArgumentException(message);
        }
    }

    public static boolean isNotBlank(String value) {
        return value != null && !"".equals(value.trim());
    }
}
