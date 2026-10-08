package dev.hoyin1600p.vhtoolforge.stat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatIdsTest {
    @Test
    void numberStatIdsAreVhAttributePaths() {
        assertEquals("mining_speed", StatIds.MINING_SPEED);
        assertEquals("item_quantity", StatIds.ITEM_QUANTITY);
        assertEquals("item_rarity", StatIds.ITEM_RARITY);
        assertEquals("trap_disarming", StatIds.TRAP_DISARMING);
        assertEquals("copiously", StatIds.COPIOUSLY);
        assertEquals("immortality", StatIds.IMMORTALITY);
        assertEquals("durability", StatIds.DURABILITY);
        assertEquals("reach", StatIds.REACH);
        assertEquals("hammer_size", StatIds.HAMMER_SIZE);
    }

    @Test
    void flagStatIdsAreVhAttributePaths() {
        assertEquals("soulbound", StatIds.SOULBOUND);
        assertEquals("picking", StatIds.PICKING);
        assertEquals("axing", StatIds.AXING);
        assertEquals("shovelling", StatIds.SHOVELLING);
        assertEquals("reaping", StatIds.REAPING);
        assertEquals("hammering", StatIds.HAMMERING);
        assertEquals("wooden_affinity", StatIds.WOODEN_AFFINITY);
        assertEquals("ornate_affinity", StatIds.ORNATE_AFFINITY);
        assertEquals("gilded_affinity", StatIds.GILDED_AFFINITY);
        assertEquals("living_affinity", StatIds.LIVING_AFFINITY);
        assertEquals("coin_affinity", StatIds.COIN_AFFINITY);
        assertEquals("smelting", StatIds.SMELTING);
        assertEquals("pulverizing", StatIds.PULVERIZING);
        assertEquals("hydrovoid", StatIds.HYDROVOID);
        assertEquals("jewel_size", StatIds.JEWEL_SIZE);
    }

    @Test
    void baseTypesKeepSavedIdsAndOrder() {
        assertEquals(List.of("PICK", "AXE", "SHOVEL", "HAMMER", "SICKLE"),
                Arrays.stream(BaseType.values()).map(BaseType::name).toList());
    }

    @Test
    void eachBaseTypeHasItsFlag() {
        assertEquals("picking", BaseType.PICK.flagId());
        assertEquals("axing", BaseType.AXE.flagId());
        assertEquals("shovelling", BaseType.SHOVEL.flagId());
        assertEquals("hammering", BaseType.HAMMER.flagId());
        assertEquals("reaping", BaseType.SICKLE.flagId());
    }

    @Test
    void onlyTheHammerIsAHammer() {
        for (BaseType t : BaseType.values()) assertEquals(t == BaseType.HAMMER, t.isHammer());
    }

    @Test
    void fromIdMapsSavedIdsAndDefaultsToPick() {
        for (BaseType t : BaseType.values()) assertEquals(t, BaseType.fromId(t.name()));
        assertEquals(BaseType.PICK, BaseType.fromId("SPEAR"));
        assertEquals(BaseType.PICK, BaseType.fromId("hammer"));
        assertEquals(BaseType.PICK, BaseType.fromId(""));
        assertEquals(BaseType.PICK, BaseType.fromId(null));
        assertTrue(BaseType.fromId("HAMMER").isHammer());
        assertFalse(BaseType.fromId("SPEAR").isHammer());
    }
}
