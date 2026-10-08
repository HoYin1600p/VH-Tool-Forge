package dev.hoyin1600p.vhtoolforge.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * One jewel as the planner sees it: where it is, its size and level, and its stats.
 * Real jewels keep their item NBT (as SNBT) so they can be shown and matched again in storage.
 */
public final class Jewel {
    /** Where a jewel comes from: scanned storage, a ticked unique jewel that was not found, or one the plan says to craft. */
    public enum Kind { STORED, UNIQUE_TICKED, CRAFT }

    /** Unique id within the store; stored jewels use their source, a hash and a count, placeholders use "unique:" or "craft:" ids. */
    public String id;
    /** Key of the {@link Source} this jewel was scanned from; null for placeholders. */
    public String sourceKey;
    public Kind kind = Kind.STORED;
    /** Display name. */
    public String name;
    /** Tool capacity this jewel takes. */
    public int size;
    /** The jewel level; a tool built with it is at least this level. */
    public int level;
    public Map<String, Double> nums = new LinkedHashMap<>();
    public Set<String> flags = new LinkedHashSet<>();
    /** Other modifiers the planner does not score (mana on loot, ability casts), as display text. */
    public Set<String> extras = new LinkedHashSet<>();
    /** Key of the unique jewel this is, or null for an ordinary jewel. */
    public String uniqueKey;
    /** Item NBT of the stored jewel, as SNBT. Null for crafted or ticked-unique placeholders. */
    public String snbt;
    /** Crafting cost text, for jewels the plan says to craft. */
    public String craftCost;

    /** True for jewels that are not real stored items (crafts and ticked uniques). */
    public boolean isPlaceholder() {
        return kind != Kind.STORED;
    }
}
