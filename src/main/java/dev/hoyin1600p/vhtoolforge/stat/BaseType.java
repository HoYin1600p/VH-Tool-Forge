package dev.hoyin1600p.vhtoolforge.stat;

/**
 * The tool base types. {@link #name()} is the id saved in the settings and used in the pack's tool recipes;
 * each type carries the yes/no stat that marks a tool of that type.
 */
public enum BaseType {
    PICK(StatIds.PICKING),
    AXE(StatIds.AXING),
    SHOVEL(StatIds.SHOVELLING),
    HAMMER(StatIds.HAMMERING),
    SICKLE(StatIds.REAPING);

    private final String flagId;

    BaseType(String flagId) {
        this.flagId = flagId;
    }

    /** The yes/no stat id of this base type's own tool flag. */
    public String flagId() {
        return flagId;
    }

    /** True for the hammer base type. */
    public boolean isHammer() {
        return this == HAMMER;
    }

    /** The base type with this saved id; anything unknown (or null) is a pick. */
    public static BaseType fromId(String id) {
        for (BaseType t : values()) if (t.name().equals(id)) return t;
        return PICK;
    }
}
