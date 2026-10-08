package dev.hoyin1600p.vhtoolforge.solve;

import dev.hoyin1600p.vhtoolforge.model.Craft;
import dev.hoyin1600p.vhtoolforge.model.Jewel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SolverTest {
    private static int ids;

    private static Jewel jewel(int size, Map<String, Double> nums, String... flags) {
        Jewel j = new Jewel();
        j.id = "j" + (++ids);
        j.sourceKey = "src";
        j.size = size;
        j.nums.putAll(nums);
        j.flags.addAll(List.of(flags));
        return j;
    }

    private static Solver.Input input(int cap) {
        Solver.Input in = new Solver.Input();
        in.capacity = cap;
        in.baseFlag = "picking";
        in.crafts.put("wooden_affinity", new Craft(5, "1 Gemstone"));
        in.crafts.put("gilded_affinity", new Craft(5, "1 Gemstone"));
        return in;
    }

    @Test
    void staysWithinCapacityAndMeetsTarget() {
        Solver.Input in = input(30);
        in.goals.add(new Solver.NumGoal("item_quantity", 0.10, 0, 1, 0));
        for (int i = 0; i < 10; i++) in.pool.add(jewel(10, Map.of("item_quantity", 0.04)));
        Solver.Result r = Solver.solve(in);
        assertTrue(r.used <= 30);
        assertEquals(3, r.selected.size());
        assertEquals(1.0, r.percent(), 1e-9);
    }

    @Test
    void stopsAddingOnceTargetIsMet() {
        Solver.Input in = input(100);
        in.goals.add(new Solver.NumGoal("item_quantity", 0.08, 0, 1, 0));
        for (int i = 0; i < 10; i++) in.pool.add(jewel(10, Map.of("item_quantity", 0.04)));
        Solver.Result r = Solver.solve(in);
        assertEquals(2, r.selected.size(), "overshooting the target wastes capacity");
    }

    @Test
    void mustHaveFlagUsesStorageOrReservesCraft() {
        Solver.Input in = input(50);
        in.requiredFlags.addAll(Set.of("wooden_affinity", "gilded_affinity"));
        Jewel wooden = jewel(5, Map.of(), "wooden_affinity");
        in.pool.add(wooden);
        Solver.Result r = Solver.solve(in);
        assertTrue(r.selected.contains(wooden));
        assertEquals(1, r.crafts.size());
        assertTrue(r.crafts.get(0).flags.contains("gilded_affinity"));
        assertEquals(10, r.used);
    }

    @Test
    void mustHaveWithoutRecipeIsReportedMissing() {
        Solver.Input in = input(50);
        in.requiredFlags.add("hydrovoid");
        Solver.Result r = Solver.solve(in);
        assertEquals(List.of("hydrovoid"), r.missing);
    }

    @Test
    void mustHavesComeBeforeNumbers() {
        Solver.Input in = input(15);
        in.requiredFlags.add("wooden_affinity");
        in.goals.add(new Solver.NumGoal("mining_speed", 100, 9, 1, 0));
        in.pool.add(jewel(10, Map.of("mining_speed", 8.0)));
        in.pool.add(jewel(10, Map.of("mining_speed", 7.0)));
        Solver.Result r = Solver.solve(in);
        assertEquals(1, r.crafts.size());
        assertEquals(15, r.used);
    }

    @Test
    void soulboundIsOnlyTakenWhenItFits() {
        Solver.Input in = input(10);
        in.niceFlags.add("soulbound");
        in.goals.add(new Solver.NumGoal("trap_disarming", 0.10, 0, 5, 0));
        Jewel trap = jewel(10, Map.of("trap_disarming", 0.10));
        Jewel soul = jewel(10, Map.of(), "soulbound");
        in.pool.add(soul);
        in.pool.add(trap);
        Solver.Result r = Solver.solve(in);
        assertEquals(List.of(trap), r.selected);
    }

    @Test
    void prefersJewelWithoutWastedModifiers() {
        Solver.Input in = input(10);
        in.goals.add(new Solver.NumGoal("item_quantity", 0.20, 0, 1, 0));
        in.goals.add(new Solver.NumGoal("trap_disarming", 0.50, 0, 1, 0));
        Jewel wasted = jewel(10, Map.of("item_quantity", 0.03), "soulbound");
        Jewel both = jewel(10, Map.of("item_quantity", 0.03, "trap_disarming", 0.08));
        in.pool.add(wasted);
        in.pool.add(both);
        assertEquals(List.of(both), Solver.solve(in).selected);
    }

    @Test
    void hammerSizeCappedAndOnlyOnHammer() {
        Solver.Input in = input(200);
        in.goals.add(new Solver.NumGoal("hammer_size", 20, 1, 1, 7));
        in.hammer = true;
        for (int i = 0; i < 12; i++) in.pool.add(jewel(10, Map.of("hammer_size", 1.0)));
        Solver.Result r = Solver.solve(in);
        assertEquals(7, r.selected.size(), "no point adding hammer size past the cap of 7");
    }
}
