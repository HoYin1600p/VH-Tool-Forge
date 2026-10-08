package dev.hoyin1600p.vhtoolforge.stat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The tool stats a jewel can carry, with VH's own display names and reader colours. */
public final class Stats {
    /** How a number stat is written. */
    public enum Format { DEC1, DEC2, INT, PERCENT }

    /** A stat whose values add up across jewels. */
    public record Num(String id, String name, int color, Format format, double base, boolean hammerOnly, double jewelCap) {
        /** The value as text, with a leading + for non-negative values when {@code sign} is set. */
        public String format(double value, boolean sign) {
            String prefix = sign && value >= 0 ? "+" : "";
            return switch (format) {
                case PERCENT -> prefix + String.format(Locale.ROOT, "%.1f%%", value * 100);
                case DEC1 -> prefix + String.format(Locale.ROOT, "%.1f", value);
                case DEC2 -> prefix + String.format(Locale.ROOT, "%.2f", value);
                case INT -> prefix + Math.round(value);
            };
        }

        /** What the player types: percentages as whole percent. */
        public double toTyped(double value) {
            return format == Format.PERCENT ? value * 100 : value;
        }

        /** Converts what the player typed back to internal units. */
        public double fromTyped(double typed) {
            return format == Format.PERCENT ? typed / 100 : typed;
        }
    }

    /** A yes/no stat; duplicates add nothing. */
    public record Flag(String id, String name, int color, boolean toolType, boolean baseOnly, boolean niceToHave) {}

    /** All number stats, in display order. */
    public static final List<Num> NUMS = List.of(
            new Num(StatIds.MINING_SPEED, "Mining Speed", 0x48BAF8, Format.DEC1, 9, false, 0),
            new Num(StatIds.ITEM_QUANTITY, "Item Quantity", 0xE88A12, Format.PERCENT, 0, false, 0),
            new Num(StatIds.ITEM_RARITY, "Item Rarity", 0xEAFF00, Format.PERCENT, 0, false, 0),
            new Num(StatIds.TRAP_DISARMING, "Trap Disarm Chance", 0x8143FF, Format.PERCENT, 0, false, 0),
            new Num(StatIds.COPIOUSLY, "Copiously", 0xF74780, Format.PERCENT, 0, false, 0),
            new Num(StatIds.IMMORTALITY, "Vanilla Immortality", 0xCDF392, Format.PERCENT, 0, false, 0),
            new Num(StatIds.DURABILITY, "Durability", 0xDFD0FE, Format.INT, 4096, false, 0),
            new Num(StatIds.REACH, "Reach", 0x84D7FF, Format.DEC2, 0, false, 0),
            new Num(StatIds.HAMMER_SIZE, "Hammer Size", 0x25D4A5, Format.INT, 1, true, 7));

    /** All yes/no stats, in display order. */
    public static final List<Flag> FLAGS = List.of(
            new Flag(StatIds.SOULBOUND, "Soulbound", 0x9664FD, false, false, true),
            new Flag(StatIds.PICKING, "Picking", 0xEAEAEA, true, false, false),
            new Flag(StatIds.AXING, "Axing", 0x90693A, true, false, false),
            new Flag(StatIds.SHOVELLING, "Shovelling", 0xE2E69F, true, false, false),
            new Flag(StatIds.REAPING, "Reaping", 0x3B8A40, true, false, false),
            new Flag(StatIds.HAMMERING, "Hammering", 0x25D4A5, true, true, false),
            new Flag(StatIds.WOODEN_AFFINITY, "Wooden Affinity", 0xAB6A2E, false, false, false),
            new Flag(StatIds.ORNATE_AFFINITY, "Ornate Affinity", 0xEC2525, false, false, false),
            new Flag(StatIds.GILDED_AFFINITY, "Gilded Affinity", 0xFFE42C, false, false, false),
            new Flag(StatIds.LIVING_AFFINITY, "Living Affinity", 0x72FF41, false, false, false),
            new Flag(StatIds.COIN_AFFINITY, "Coin Affinity", 0xEFA016, false, false, false),
            new Flag(StatIds.SMELTING, "Smelting", 0xFF7200, false, false, false),
            new Flag(StatIds.PULVERIZING, "Pulverizing", 0x73B373, false, false, false),
            new Flag(StatIds.HYDROVOID, "Hydrovoid", 0x00FCFF, false, false, false));

    private static final Map<String, Num> NUM_BY_ID = new LinkedHashMap<>();
    private static final Map<String, Flag> FLAG_BY_ID = new LinkedHashMap<>();

    static {
        NUMS.forEach(n -> NUM_BY_ID.put(n.id(), n));
        FLAGS.forEach(f -> FLAG_BY_ID.put(f.id(), f));
    }

    /** The number stat with this id, or null. */
    public static Num num(String id) {
        return NUM_BY_ID.get(id);
    }

    /** The yes/no stat with this id, or null. */
    public static Flag flag(String id) {
        return FLAG_BY_ID.get(id);
    }

    /** Display name for any stat id, falling back to a title-cased id. */
    public static String name(String id) {
        if (NUM_BY_ID.containsKey(id)) return NUM_BY_ID.get(id).name();
        if (FLAG_BY_ID.containsKey(id)) return FLAG_BY_ID.get(id).name();
        StringBuilder sb = new StringBuilder();
        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private Stats() {}
}
