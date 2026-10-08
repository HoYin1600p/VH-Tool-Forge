package dev.hoyin1600p.vhtoolforge.plan;

import dev.hoyin1600p.vhtoolforge.data.Store;
import dev.hoyin1600p.vhtoolforge.model.Jewel;
import dev.hoyin1600p.vhtoolforge.model.Settings;
import dev.hoyin1600p.vhtoolforge.model.Source;
import dev.hoyin1600p.vhtoolforge.pack.PackConfig;
import dev.hoyin1600p.vhtoolforge.solve.Solver;
import dev.hoyin1600p.vhtoolforge.stat.BaseType;
import dev.hoyin1600p.vhtoolforge.stat.Stats;
import iskallia.vault.item.tool.ToolMaterial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Turns the player's settings and scanned jewels into a solver run. */
public final class Planner {
    /** The tool material chosen in the settings; chromatic iron if it is unknown. */
    public static ToolMaterial material(Settings s) {
        try {
            return ToolMaterial.valueOf(s.material);
        } catch (IllegalArgumentException | NullPointerException e) {
            return ToolMaterial.CHROMATIC_IRON_INGOT;
        }
    }

    /** The yes/no stat id of the chosen base type. */
    public static String baseFlag(Settings s) {
        return BaseType.fromId(s.baseType).flagId();
    }

    /** True when the chosen base type is a hammer. */
    public static boolean hammer(Settings s) {
        return BaseType.fromId(s.baseType).isHammer();
    }

    /** The chosen tool's capacity, plus the prestige capacity the player entered if ticked. */
    public static int capacity(Settings s) {
        return material(s).getCapacity() + (s.extraCapacity ? Math.max(0, s.extraCapacityAmount) : 0);
    }

    /** Number of stats the priority dropdown offers on this tool (Hammer Size only on hammers). */
    public static int rankCount(Settings s) {
        int n = 0;
        for (Stats.Num num : Stats.NUMS) if (!num.hammerOnly() || hammer(s)) n++;
        return n;
    }

    /** Rank 1 weighs most; unranked stats share the lowest weight (1). */
    public static double weight(Settings s, Integer rank) {
        if (rank == null) return 1;
        return Math.max(1, rankCount(s) + 2 - rank);
    }

    /** The stat's base value on the chosen tool; hammer-only stats have one only on hammers. */
    public static double base(Settings s, Stats.Num n) {
        if (n.hammerOnly()) return hammer(s) ? n.base() : 0;
        return n.base();
    }

    /** Must-have yes/no stats (ticked, not the base type's own, not nice-to-have). */
    public static Set<String> requiredFlags(Settings s) {
        Set<String> out = new LinkedHashSet<>();
        String base = baseFlag(s);
        for (Stats.Flag f : Stats.FLAGS) {
            if (f.baseOnly() || f.niceToHave() || f.id().equals(base)) continue;
            if (s.flags.contains(f.id())) out.add(f.id());
        }
        return out;
    }

    /** Builds the solver input from the settings and the scanned jewels. */
    public static Solver.Input input(Store store) {
        Settings s = store.settings;
        PackConfig pack = PackConfig.get();
        Solver.Input in = new Solver.Input();
        in.capacity = capacity(s);
        in.baseFlag = baseFlag(s);
        in.hammer = hammer(s);

        for (Stats.Num n : Stats.NUMS) {
            if (n.hammerOnly() && !in.hammer) continue;
            Settings.Target t = s.targets.get(n.id());
            if (t == null || t.value == null || t.value <= 0) continue;
            in.goals.add(new Solver.NumGoal(n.id(), t.value, base(s, n), weight(s, t.rank), n.jewelCap()));
        }
        in.requiredFlags.addAll(requiredFlags(s));
        for (Stats.Flag f : Stats.FLAGS) if (f.niceToHave() && s.flags.contains(f.id())) in.niceFlags.add(f.id());

        Set<String> enabled = new HashSet<>();
        for (Source src : store.sources) if (src.enabled) enabled.add(src.key);
        for (Jewel j : store.jewels) {
            if (!enabled.contains(j.sourceKey)) continue;
            if (s.maxLevel != null && j.level > s.maxLevel) continue;
            in.pool.add(j);
        }

        // Ticked uniques: use a scanned copy when there is one (its real size and location), else a placeholder.
        for (PackConfig.UniqueJewel u : pack.uniques) {
            Settings.UniqueChoice c = s.uniques.get(u.key());
            if (c == null || !c.on) continue;
            int wanted = u.size().fixed() ? u.size().min() : u.size().clamp(c.size == null ? u.size().min() : c.size);
            Jewel stored = null;
            for (Jewel j : in.pool) {
                if (!u.key().equals(j.uniqueKey)) continue;
                if (stored == null || Math.abs(j.size - wanted) < Math.abs(stored.size - wanted)) stored = j;
            }
            if (stored != null) {
                in.forced.add(stored);
            } else {
                Jewel p = new Jewel();
                p.id = "unique:" + u.key();
                p.kind = Jewel.Kind.UNIQUE_TICKED;
                p.name = u.name();
                p.size = wanted;
                p.uniqueKey = u.key();
                p.nums.putAll(u.nums());
                p.flags.addAll(u.flags());
                p.extras.addAll(u.extras());
                in.forced.add(p);
            }
        }
        in.crafts.putAll(pack.crafts);
        return in;
    }

    /** Solves the current settings and scanned jewels. */
    public static Solver.Result plan(Store store) {
        return Solver.solve(input(store));
    }

    /** Locks whatever the planner would pick right now (used when Collect is pressed with no locked plan). */
    public static void lockCurrentPlan() {
        Store st = Store.get();
        Solver.Result r = plan(st);
        List<Jewel> stored = new ArrayList<>();
        for (Jewel j : r.selected) stored.add(j);
        st.lockPlan(stored);
    }

    /** Highest jewel level in a selection (the built tool's level is at least this). */
    public static int maxLevel(List<Jewel> sel) {
        int m = 0;
        for (Jewel j : sel) m = Math.max(m, j.level);
        return m;
    }

    private Planner() {}
}
