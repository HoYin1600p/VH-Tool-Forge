package dev.hoyin1600p.vhtoolforge.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoyin1600p.vhtoolforge.stat.StatIds;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackConfigTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    private static final String MODIFIERS = """
            {"modifierGroup": {
              "size": [
                {"identifier": "size_fixed", "attribute": "the_vault:jewel_size", "tiers": [{"value": {"min": 5, "max": 5}}]},
                {"identifier": "size_range", "attribute": "the_vault:jewel_size", "tiers": [{"value": {"min": 10, "max": 20, "step": 5}}]}
              ],
              "effects": [
                {"identifier": "mining", "attribute": "the_vault:mining_speed", "tiers": [{"value": {"min": 0.5, "max": 2.0}}]},
                {"identifier": "smelt", "attribute": "the_vault:smelting", "tiers": [{"value": {"flag": true}}]},
                {"identifier": "odd", "attribute": "the_vault:mystery", "tiers": [{"value": {"displayName": "Where"}}]}
              ]
            }}
            """;

    private static final String GEAR = """
            {"registry": {
              "fixed_one": {"item": "the_vault:jewel", "name": "Fixed One",
                "modifierIdentifiers": {"a": ["size_fixed"], "b": ["mining", "smelt"]}, "lore": "Old lore"},
              "ranged_one": {"item": "the_vault:jewel", "modifierIdentifiers": {"a": ["size_range", "odd", "gone"]}},
              "a_sword": {"item": "the_vault:sword", "name": "Not A Jewel"}
            }}
            """;

    @Test
    void parsesUniqueJewelsWithFixedAndRangedSizes() {
        PackConfig pc = new PackConfig();
        pc.parseUniques(json(GEAR), json(MODIFIERS));

        assertEquals(2, pc.uniques.size());
        PackConfig.UniqueJewel fixed = pc.uniques.get(0);
        assertEquals("fixed_one", fixed.key());
        assertEquals("Fixed One", fixed.name());
        assertTrue(fixed.size().fixed());
        assertEquals(5, fixed.size().min());
        assertEquals(0.5, fixed.nums().get(StatIds.MINING_SPEED));
        assertTrue(fixed.flags().contains(StatIds.SMELTING));
        assertEquals("Old lore", fixed.lore());

        PackConfig.UniqueJewel ranged = pc.uniques.get(1);
        assertEquals("ranged_one", ranged.name()); // no name: falls back to the registry key
        assertFalse(ranged.size().fixed());
        assertEquals(10, ranged.size().min());
        assertEquals(20, ranged.size().max());
        assertEquals(5, ranged.size().step());
        assertEquals(15, ranged.size().clamp(14));
        assertEquals(20, ranged.size().clamp(99));
        assertNull(ranged.lore());
        assertEquals(List.of("Mystery (Where)"), ranged.extras());
        assertEquals(List.of("ranged_one: no modifier gone"), pc.problems);
    }

    @Test
    void missingUniqueFilesLeaveTheListEmpty() {
        PackConfig pc = new PackConfig();
        pc.parseUniques(null, json(MODIFIERS));
        pc.parseUniques(json(GEAR), null);
        pc.parseUniques(json("{}"), json(MODIFIERS));
        assertTrue(pc.uniques.isEmpty());
    }

    @Test
    void parsesFlagRecipesAndIgnoresOthers() {
        PackConfig pc = new PackConfig();
        JsonObject recipes = json("""
                {"jewelCraftingRecipes": [
                  {"jewelAttribute": "the_vault:smelting", "size": 7,
                   "inputs": [{"ITEM": "the_vault:gem", "AMOUNT": 2}, {"ITEM": "the_vault:ingot", "AMOUNT": 1}]},
                  {"jewelAttribute": "the_vault:mining_speed", "size": 4, "inputs": []},
                  {"jewelAttribute": "the_vault:smelting", "size": 99, "inputs": []}
                ]}
                """);
        pc.parseCrafts(recipes, id -> id.substring(id.indexOf(':') + 1).toUpperCase());

        assertEquals(1, pc.crafts.size()); // the non-flag recipe is ignored, the first flag recipe wins
        assertEquals(7, pc.crafts.get(StatIds.SMELTING).size());
        assertEquals("2 GEM, 1 INGOT", pc.crafts.get(StatIds.SMELTING).cost());
    }

    @Test
    void missingRecipeFileAddsNothing() {
        PackConfig pc = new PackConfig();
        pc.parseCrafts(null, id -> id);
        pc.parseCrafts(json("{}"), id -> id);
        assertTrue(pc.crafts.isEmpty());
    }
}
