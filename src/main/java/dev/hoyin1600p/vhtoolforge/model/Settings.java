package dev.hoyin1600p.vhtoolforge.model;

import dev.hoyin1600p.vhtoolforge.stat.BaseType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Everything the player entered on the planner screen. */
public final class Settings {
    /** A number target: the wanted total and its priority. */
    public static final class Target {
        /** Target as a tool total, in internal units (fractions for percentages). Null = no target. */
        public Double value;
        /** Priority rank, 1 = most important. Null = unranked (lowest, equal weight). */
        public Integer rank;
    }

    /** Whether a unique jewel is ticked, and the size chosen for it. */
    public static final class UniqueChoice {
        /** Whether the unique jewel is ticked. */
        public boolean on;
        /** The size chosen for the unique jewel; null for the smallest. */
        public Integer size;
    }

    public String material = "ECHOING_INGOT";
    public String baseType = BaseType.PICK.name();
    /** Highest jewel level the plan may use; null for no limit. */
    public Integer maxLevel;
    /** Whether the prestige tool capacity bonus applies. */
    public boolean extraCapacity;
    /** Show the Scan / Collect buttons beside storage screens. */
    public boolean showStorageButtons = true;
    public int extraCapacityAmount = 100;
    public Map<String, Target> targets = new LinkedHashMap<>();
    public Set<String> flags = new LinkedHashSet<>();
    public Map<String, UniqueChoice> uniques = new LinkedHashMap<>();

    /** Target backpack: Sophisticated Backpacks' contents UUID, and the inventory slot it was last seen in. */
    public String backpackUuid;
    public int backpackSlot = -1;
    /** Display name of the target backpack when it was chosen. */
    public String backpackName;

    /** The target of a stat, created on first use. */
    public Target target(String stat) {
        return targets.computeIfAbsent(stat, k -> new Target());
    }

    /** The choice for a unique jewel, created on first use. */
    public UniqueChoice unique(String key) {
        return uniques.computeIfAbsent(key, k -> new UniqueChoice());
    }
}
