package dev.hoyin1600p.vhtoolforge.util;

import net.minecraft.client.resources.language.I18n;

/** Small text helpers shared by the screens and messages. */
public final class Text {
    /** The translated noun "jewel" or "jewels" for a count: singular only when it is exactly one. */
    public static String jewels(long count) {
        return I18n.get(count == 1 ? "message.vhtoolforge.jewel" : "message.vhtoolforge.jewels");
    }

    private Text() {}
}
