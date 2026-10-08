package dev.hoyin1600p.vhtoolforge.model;

import net.minecraft.client.resources.language.I18n;

/** A scanned storage: an AE2 network terminal, an RS grid, or a plain container. */
public final class Source {
    /** What kind of storage was scanned. */
    public enum Kind { AE2, RS, CONTAINER }

    /** Stable id built from the storage kind, dimension and position. */
    public String key;
    /** What kind of storage this is. */
    public Kind kind;
    /** What the player sees, e.g. "ME Terminal" or "Chest". */
    public String label;
    /** Dimension id the storage was scanned in. */
    public String dimension;
    /** Block position, or null for a handheld/wireless terminal. */
    public Integer x, y, z;
    /** When the storage was scanned, in epoch milliseconds. */
    public long scannedAt;
    public boolean enabled = true;

    /** The translated name of the storage kind. */
    public String kindName() {
        return switch (kind) {
            case AE2 -> I18n.get("gui.vhtoolforge.source.kind_ae2");
            case RS -> I18n.get("gui.vhtoolforge.source.kind_rs");
            case CONTAINER -> I18n.get("gui.vhtoolforge.source.kind_container");
        };
    }

    /** The block position, or a note that the terminal is wireless or handheld. */
    public String where() {
        if (x == null) return I18n.get("gui.vhtoolforge.source.wireless");
        return x + ", " + y + ", " + z;
    }
}
