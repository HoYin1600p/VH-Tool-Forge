package dev.hoyin1600p.vhtoolforge.scan;

import java.util.function.Predicate;

/** The shared "exact NBT first, else same fingerprint" choice used when pulling a jewel out of storage. */
public final class JewelMatch {
    /**
     * The first item that is an exact match; if none is, the first item that is only a similar match; else null.
     * Stops at the first exact match, so later items are not examined.
     */
    public static <T> T pick(Iterable<T> items, Predicate<T> exact, Predicate<T> similar) {
        T fallback = null;
        for (T item : items) {
            if (exact.test(item)) return item;
            if (fallback == null && similar.test(item)) fallback = item;
        }
        return fallback;
    }

    private JewelMatch() {}
}
