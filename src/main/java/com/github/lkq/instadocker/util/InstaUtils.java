package com.github.lkq.instadocker.util;

public class InstaUtils {
    public static boolean isNotBlank(String value) {
        return value != null && !"".equals(value.trim());
    }
}
