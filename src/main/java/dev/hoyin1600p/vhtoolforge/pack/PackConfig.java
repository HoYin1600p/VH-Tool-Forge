package dev.hoyin1600p.vhtoolforge.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.hoyin1600p.vhtoolforge.VHToolForge;
import dev.hoyin1600p.vhtoolforge.model.Craft;
import dev.hoyin1600p.vhtoolforge.stat.BaseType;
import dev.hoyin1600p.vhtoolforge.stat.StatIds;
import dev.hoyin1600p.vhtoolforge.stat.Stats;
import dev.hoyin1600p.vhtoolforge.util.ModPaths;
import iskallia.vault.item.tool.ToolMaterial;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Reads the active pack's Vault Hunters config files, so another pack's tools, unique jewels
 * and crafting recipes show up without code changes:
 * <ul>
 *   <li>{@code recipes/tool_recipes.json}: which tool materials and base types are craftable</li>
 *   <li>{@code unique_gear.json} + {@code gear_modifiers/unique.json}: unique jewels, their sizes and effects</li>
 *   <li>{@code recipes/jewel_crafting_recipes.json}: craftable jewels, their sizes and ingredients</li>
 * </ul>
 */
public final class PackConfig {
    /** The sizes a jewel can have: from min to max in steps. */
    public record SizeRange(int min, int max, int step) {
        /** True if only one size is possible. */
        public boolean fixed() {
            return min == max;
        }

        /** Snaps a size to the nearest allowed step and into the range. */
        public int clamp(int v) {
            int stepped = step <= 0 ? v : Math.round((float) (v - min) / step) * step + min;
            return Math.max(min, Math.min(max, stepped));
        }
    }

    /** A unique jewel defined by the pack: its size range, stats, other effects and lore. */
    public record UniqueJewel(String key, String name, SizeRange size, Map<String, Double> nums, Set<String> flags,
                              List<String> extras, String lore) {}

    public final List<ToolMaterial> materials = new ArrayList<>();
    public final List<String> baseTypes = new ArrayList<>();
    public final List<UniqueJewel> uniques = new ArrayList<>();
    public final Map<String, Craft> crafts = new LinkedHashMap<>();
    public final List<String> problems = new ArrayList<>();

    private static PackConfig cached;

    /** The pack configuration, read from the config/the_vault files the first time it is needed. */
    public static PackConfig get() {
        if (cached == null) cached = load();
        return cached;
    }

    private static PackConfig load() {
        PackConfig pc = new PackConfig();
        Path dir = ModPaths.vaultConfigDir();
        pc.loadTools(dir.resolve("recipes/tool_recipes.json"));
        pc.loadUniques(dir.resolve("unique_gear.json"), dir.resolve("gear_modifiers/unique.json"));
        pc.loadCrafts(dir.resolve("recipes/jewel_crafting_recipes.json"));
        if (!pc.problems.isEmpty()) VHToolForge.LOG.warn("Pack config problems: {}", pc.problems);
        return pc;
    }

    private JsonObject read(Path p) {
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        } catch (IOException | JsonParseException | IllegalStateException e) {
            problems.add(p.getFileName() + ": " + e.getMessage());
            return null;
        }
    }

    private void loadTools(Path file) {
        JsonObject root = read(file);
        Set<String> mats = new LinkedHashSet<>();
        Set<String> types = new LinkedHashSet<>();
        if (root != null && root.has("toolRecipes")) {
            for (JsonElement e : root.getAsJsonArray("toolRecipes")) {
                JsonObject o = e.getAsJsonObject();
                if (o.has("toolMaterial")) mats.add(o.get("toolMaterial").getAsString());
                if (o.has("toolType")) types.add(o.get("toolType").getAsString());
            }
        }
        for (String m : mats) {
            try {
                materials.add(ToolMaterial.valueOf(m));
            } catch (IllegalArgumentException ex) {
                problems.add("unknown tool material " + m);
            }
        }
        if (materials.isEmpty()) {
            for (ToolMaterial m : ToolMaterial.values()) if (m.getCapacity() > 0) materials.add(m);
        }
        materials.sort(Comparator.comparingInt(ToolMaterial::getCapacity).thenComparingInt(ToolMaterial::getLevel));
        baseTypes.addAll(types.isEmpty() ? Arrays.stream(BaseType.values()).map(BaseType::name).toList() : types);
    }

    private void loadUniques(Path gearFile, Path modifiersFile) {
        parseUniques(read(gearFile), read(modifiersFile));
    }

    /** Builds the unique jewel list from the parsed unique_gear.json and gear_modifiers/unique.json (either may be null). */
    void parseUniques(JsonObject gear, JsonObject mods) {
        if (gear == null || mods == null || !gear.has("registry")) return;

        // identifier -> modifier definition (attribute + first tier's value)
        Map<String, JsonObject> defs = new HashMap<>();
        JsonObject groups = mods.has("modifierGroup") ? mods.getAsJsonObject("modifierGroup") : mods;
        for (Map.Entry<String, JsonElement> g : groups.entrySet()) {
            if (!g.getValue().isJsonArray()) continue;
            for (JsonElement e : g.getValue().getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                if (o.has("identifier")) defs.put(o.get("identifier").getAsString(), o);
            }
        }

        for (Map.Entry<String, JsonElement> entry : gear.getAsJsonObject("registry").entrySet()) {
            JsonObject u = entry.getValue().getAsJsonObject();
            if (!u.has("item") || !"the_vault:jewel".equals(u.get("item").getAsString())) continue;
            String name = u.has("name") ? u.get("name").getAsString() : entry.getKey();
            SizeRange size = new SizeRange(0, 0, 1);
            Map<String, Double> nums = new LinkedHashMap<>();
            Set<String> flags = new LinkedHashSet<>();
            List<String> extras = new ArrayList<>();
            JsonObject ids = u.has("modifierIdentifiers") ? u.getAsJsonObject("modifierIdentifiers") : new JsonObject();
            for (Map.Entry<String, JsonElement> affix : ids.entrySet()) {
                for (JsonElement idEl : affix.getValue().getAsJsonArray()) {
                    JsonObject def = defs.get(idEl.getAsString());
                    if (def == null) {
                        problems.add(name + ": no modifier " + idEl.getAsString());
                        continue;
                    }
                    String attr = stripNs(def.get("attribute").getAsString());
                    JsonObject value = firstTierValue(def);
                    if (value == null) continue;
                    if (StatIds.JEWEL_SIZE.equals(attr)) {
                        size = new SizeRange(value.get("min").getAsInt(), value.get("max").getAsInt(),
                                value.has("step") ? value.get("step").getAsInt() : 1);
                    } else if (value.has("flag")) {
                        if (value.get("flag").getAsBoolean()) flags.add(attr);
                    } else if (value.has("min") && value.has("max") && Stats.num(attr) != null) {
                        nums.put(attr, value.get("min").getAsDouble());
                    } else {
                        extras.add(describe(attr, value));
                    }
                }
            }
            String lore = u.has("lore") ? u.get("lore").getAsString() : null;
            uniques.add(new UniqueJewel(entry.getKey(), name, size, nums, flags, extras, lore));
        }
    }

    private static JsonObject firstTierValue(JsonObject def) {
        if (!def.has("tiers")) return null;
        JsonArray tiers = def.getAsJsonArray("tiers");
        if (tiers.isEmpty()) return null;
        JsonElement v = tiers.get(0).getAsJsonObject().get("value");
        return v != null && v.isJsonObject() ? v.getAsJsonObject() : null;
    }

    private static String describe(String attr, JsonObject value) {
        String where = value.has("displayName") ? value.get("displayName").getAsString() : null;
        String base = Stats.name(attr);
        if (value.has("abilityId")) base = "Cast " + value.get("abilityId").getAsString().replace('_', ' ') + " on loot";
        return where == null ? base : base + " (" + where + ")";
    }

    private void loadCrafts(Path file) {
        parseCrafts(read(file), PackConfig::itemName);
    }

    /** Collects the flag-jewel recipes from the parsed jewel_crafting_recipes.json; {@code itemName} turns an item id into its display name. */
    void parseCrafts(JsonObject root, Function<String, String> itemName) {
        if (root == null || !root.has("jewelCraftingRecipes")) return;
        for (JsonElement e : root.getAsJsonArray("jewelCraftingRecipes")) {
            JsonObject o = e.getAsJsonObject();
            String attr = stripNs(o.get("jewelAttribute").getAsString());
            if (Stats.flag(attr) == null) continue; // random jewels and anything not a yes/no stat
            List<String> parts = new ArrayList<>();
            if (o.has("inputs")) {
                for (JsonElement in : o.getAsJsonArray("inputs")) {
                    JsonObject io = in.getAsJsonObject();
                    parts.add(io.get("AMOUNT").getAsInt() + " " + itemName.apply(io.get("ITEM").getAsString()));
                }
            }
            crafts.putIfAbsent(attr, new Craft(o.get("size").getAsInt(), String.join(", ", parts)));
        }
    }

    private static String itemName(String id) {
        try {
            Item item = Registry.ITEM.get(new ResourceLocation(id));
            return item.getDescription().getString();
        } catch (ResourceLocationException e) {
            return id;
        }
    }

    private static String stripNs(String id) {
        int i = id.indexOf(':');
        return i < 0 ? id : id.substring(i + 1);
    }
}
