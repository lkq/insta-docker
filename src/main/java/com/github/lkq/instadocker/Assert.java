package com.github.lkq.instadocker;

public class Assert {
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

    public static void evenNumber(int number, String message) {
        if (number / 2 != 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
