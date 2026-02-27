package com.example.util;

/**
 * Simple utility class with static methods.
 */
public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String capitalize(String str) {
        if (isBlank(str)) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

}
