package com.kissanmitra.util;

public class CommonUtils {
    public static String generateRequestId() {
        return java.util.UUID.randomUUID().toString();
    }
}
