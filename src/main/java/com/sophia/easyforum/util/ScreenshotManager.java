package com.sophia.easyforum.util;

import java.util.concurrent.atomic.AtomicReference;

public final class ScreenshotManager {
    private static final AtomicReference < String > LAST_SCREENSHOT_BASE64 = new AtomicReference < > ();

    private ScreenshotManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String getLastScreenshotBase64() {
        return LAST_SCREENSHOT_BASE64.get();
    }

    public static void setLastScreenshotBase64(String base64) {
        LAST_SCREENSHOT_BASE64.set(base64);
    }

    public static void clearLastScreenshot() {
        LAST_SCREENSHOT_BASE64.set(null);
    }

    public static boolean hasScreenshot() {
        return LAST_SCREENSHOT_BASE64.get() != null;
    }
}