package dev.hoyin1600p.vhtoolforge.gui;

import dev.hoyin1600p.vhtoolforge.stat.Stats;
import iskallia.vault.item.tool.ToolMaterial;
import net.minecraft.client.resources.language.I18n;

/** Number and text formatting and parsing for the planner screen. */
final class Format {
    static String materialName(ToolMaterial m) {
        return I18n.get(m.getDescription());
    }

    /** A base type id such as "PICK" as its stat name. */
    static String title(String id) {
        return Stats.name(id.toLowerCase());
    }

    /** A number with at most two decimals and no trailing zeros. */
    static String trim(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        String s = String.format("%.2f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    /** The whole number in a text box, or null when it is empty or not a number. */
    static Integer parseInt(String v) {
        try {
            return v == null || v.isBlank() ? null : (int) Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The decimal in a text box, or null when it is empty or not a number. */
    static Double parseDouble(String v) {
        try {
            return v == null || v.isBlank() || v.equals(".") ? null : Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** A dimension id without its namespace. */
    static String shortDim(String dim) {
        if (dim == null) return "";
        int i = dim.indexOf(':');
        return i < 0 ? dim : dim.substring(i + 1);
    }

    /** How long ago a timestamp was, in the coarsest whole unit. */
    static String ago(long t) {
        long s = (System.currentTimeMillis() - t) / 1000;
        if (s < 60) return I18n.get("gui.vhtoolforge.time.just_now");
        if (s < 3600) return I18n.get("gui.vhtoolforge.time.min_ago", s / 60);
        if (s < 86400) return I18n.get("gui.vhtoolforge.time.hours_ago", s / 3600);
        return I18n.get("gui.vhtoolforge.time.days_ago", s / 86400);
    }

    private Format() {}
}
