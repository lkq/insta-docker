package com.github.lkq.instadocker.util;

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

    public static void requiresEvenNumber(int number, String message) {
        if (number / 2 != 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
