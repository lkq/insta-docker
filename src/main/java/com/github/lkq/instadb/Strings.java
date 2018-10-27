package com.github.lkq.instadb;

public class Strings {
    public static void requiresNotBlank(String value, String message) {
        if (value == null || value.trim().equals("")) {
            throw new IllegalArgumentException(message);
        }
    }
    public static boolean isNotBlank(String value) {
        return value != null && !"".equals(value.trim());
    }
}
