package dev.hoyin1600p.vhtoolforge.util;

/** The short status message shown beside storage screens (Scan / Collect results). */
public final class Toast {
    /** How long a toast message stays above a storage screen. */
    private static final long TOAST_MS = 5000;

    private static String message = "";
    private static long until;

    /** Shows a message above storage screens for a few seconds. */
    public static void show(String msg) {
        message = msg;
        until = System.currentTimeMillis() + TOAST_MS;
    }

    /** The latest toast message, which may already have expired. */
    public static String message() {
        return message;
    }

    /** True once the latest toast has been shown long enough. */
    public static boolean expired() {
        return System.currentTimeMillis() > until;
    }

    private Toast() {}
}
