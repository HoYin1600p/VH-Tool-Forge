package dev.hoyin1600p.vhtoolforge.plan;

import dev.hoyin1600p.vhtoolforge.model.Settings;
import dev.hoyin1600p.vhtoolforge.stat.BaseType;
import dev.hoyin1600p.vhtoolforge.stat.StatIds;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Covers the Planner helpers that run without Minecraft (nothing here touches ToolMaterial or the pack config). */
class PlannerTest {
    private static Settings settings(BaseType type) {
        Settings s = new Settings();
        s.baseType = type.name();
        return s;
    }

    @Test
    void rankCountSkipsHammerSizeOnNonHammers() {
        assertEquals(8, Planner.rankCount(settings(BaseType.PICK)));
        assertEquals(9, Planner.rankCount(settings(BaseType.HAMMER)));
    }

    @Test
    void unknownBaseTypeCountsAsPick() {
        Settings s = new Settings();
        s.baseType = "NOT_A_TYPE";
        assertEquals(8, Planner.rankCount(s));
    }

    @Test
    void weightFallsWithRankAndUnrankedIsOne() {
        Settings s = settings(BaseType.PICK);
        assertEquals(1, Planner.weight(s, null));
        assertEquals(9, Planner.weight(s, 1));
        assertEquals(2, Planner.weight(s, 8));
        assertEquals(1, Planner.weight(s, 20));
        assertEquals(10, Planner.weight(settings(BaseType.HAMMER), 1));
    }

    @Test
    void requiredFlagsAreTickedMustHavesOnly() {
        Settings s = settings(BaseType.PICK);
        s.flags.addAll(List.of(StatIds.PICKING, StatIds.AXING, StatIds.HAMMERING, StatIds.SOULBOUND,
                StatIds.SMELTING, StatIds.HYDROVOID));
        // Own base flag (picking), base-only (hammering) and nice-to-have (soulbound) are left out.
        assertEquals(Set.of(StatIds.AXING, StatIds.SMELTING, StatIds.HYDROVOID), Planner.requiredFlags(s));
    }

    @Test
    void requiredFlagsEmptyWhenNothingTicked() {
        assertTrue(Planner.requiredFlags(settings(BaseType.AXE)).isEmpty());
    }

    @Test
    void baseHelpersFollowBaseType() {
        assertEquals(StatIds.HAMMERING, Planner.baseFlag(settings(BaseType.HAMMER)));
        assertTrue(Planner.hammer(settings(BaseType.HAMMER)));
        assertEquals(StatIds.AXING, Planner.baseFlag(settings(BaseType.AXE)));
    }
}
