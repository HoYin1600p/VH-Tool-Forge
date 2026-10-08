package dev.hoyin1600p.vhtoolforge.solve;

import dev.hoyin1600p.vhtoolforge.model.Craft;
import dev.hoyin1600p.vhtoolforge.model.Jewel;
import dev.hoyin1600p.vhtoolforge.stat.StatIds;
import dev.hoyin1600p.vhtoolforge.stat.Stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Picks jewels for one tool.
 * <ol>
 *   <li>Ticked unique jewels go in first.</li>
 *   <li>Every must-have yes/no stat is covered next: from storage when that costs no more space than
 *       crafting it, otherwise a craft placeholder reserves the recipe's size.</li>
 *   <li>Remaining capacity goes to the number targets (and nice-to-have flags) by weighted score:
 *       greedy by gain per size, then swap/removal local search, from a few starting orders.</li>
 * </ol>
 * Pure Java so it can be unit-tested without Minecraft.
 */
public final class Solver {
    /** Smallest score gain that counts as an improvement. */
    private static final double TOLERANCE = 1e-12;
    /** Weight of the preference for using less capacity, relative to one unit of score. */
    private static final double CAPACITY_TIEBREAK = 1e-4;
    /** Cap on swap/removal rounds per starting order. */
    private static final int LOCAL_SEARCH_PASSES = 40;
    /** Number of greedy starting orders tried (see {@code optimize}). */
    private static final int GREEDY_ORDERS = 3;

    /** A number target: the wanted total, the tool's base value, the priority weight and an optional cap on what jewels may add. */
    public record NumGoal(String stat, double target, double base, double weight, double jewelCap) {}

    /** Everything the solver needs for one tool: capacity, goals, flags and the jewels to pick from. */
    public static final class Input {
        public int capacity;
        public String baseFlag;
        public boolean hammer;
        public List<NumGoal> goals = new ArrayList<>();
        public Set<String> requiredFlags = new LinkedHashSet<>();
        public Set<String> niceFlags = new LinkedHashSet<>();
        public List<Jewel> pool = new ArrayList<>();
        public List<Jewel> forced = new ArrayList<>();
        public Map<String, Craft> crafts = new HashMap<>();
    }

    /** The solver's pick and how well it meets the targets. */
    public static final class Result {
        public List<Jewel> selected = new ArrayList<>();
        public int used;
        public int capacity;
        public boolean over;
        public List<String> missing = new ArrayList<>();
        public List<Jewel> crafts = new ArrayList<>();
        public double score;
        public double maxScore;

        /** The score as a fraction of the best possible score; 1 when there is nothing to score. */
        public double percent() {
            return maxScore <= 0 ? 1 : score / maxScore;
        }
    }

    /** Stat totals of a selection (jewel contributions only; bases are added by the caller). */
    public static final class Totals {
        public final Map<String, Double> nums = new HashMap<>();
        public final Set<String> flags = new HashSet<>();
    }

    /** Adds up the stats of a selection, applying the per-stat caps on what jewels may add. */
    public static Totals totals(Input in, List<Jewel> sel) {
        Totals t = new Totals();
        if (in.baseFlag != null) t.flags.add(in.baseFlag);
        for (Jewel j : sel) {
            for (String f : j.flags) if (!StatIds.HAMMERING.equals(f)) t.flags.add(f);
            for (Map.Entry<String, Double> e : j.nums.entrySet()) {
                if (StatIds.HAMMER_SIZE.equals(e.getKey()) && !in.hammer) continue;
                t.nums.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }
        for (NumGoal g : in.goals) {
            if (g.jewelCap() > 0 && t.nums.containsKey(g.stat())) t.nums.put(g.stat(), Math.min(g.jewelCap(), t.nums.get(g.stat())));
        }
        return t;
    }

    static double score(Input in, List<Jewel> sel) {
        Totals t = totals(in, sel);
        double s = 0;
        for (NumGoal g : in.goals) {
            double total = g.base() + t.nums.getOrDefault(g.stat(), 0.0);
            s += g.weight() * Math.min(1.0, total / g.target());
        }
        for (String f : in.niceFlags) if (t.flags.contains(f)) s += 1;
        return s;
    }

    static double maxScore(Input in) {
        double m = in.niceFlags.size();
        for (NumGoal g : in.goals) m += g.weight();
        return m;
    }

    /** Picks the jewels for one tool; the class description lists the steps. */
    public static Result solve(Input in) {
        Result r = new Result();
        r.capacity = in.capacity;
        r.maxScore = maxScore(in);

        // 1-2: forced uniques, then must-have flags.
        List<Jewel> locked = new ArrayList<>(in.forced);
        coverRequiredFlags(in, locked, r);
        int lockedUsed = locked.stream().mapToInt(j -> j.size).sum();
        if (lockedUsed > in.capacity || (in.goals.isEmpty() && in.niceFlags.isEmpty())) {
            r.selected = locked;
            r.used = lockedUsed;
            r.over = lockedUsed > in.capacity;
            r.score = score(in, locked);
            return r;
        }

        // 3: score the rest, from a few greedy starting orders.
        List<Jewel> candidates = candidates(in, locked);
        List<Jewel> best = null;
        double bestObj = -Double.MAX_VALUE;
        for (int order = 0; order < GREEDY_ORDERS; order++) {
            List<Jewel> sel = optimize(in, locked, candidates, order);
            double o = objective(in, sel);
            if (o > bestObj) {
                bestObj = o;
                best = sel;
            }
        }
        r.selected = best;
        r.used = best.stream().mapToInt(j -> j.size).sum();
        r.score = score(in, best);
        return r;
    }

    /**
     * Covers every must-have flag not already provided by {@code locked}: a stored jewel when it costs no more
     * space than crafting, else a craft placeholder, else the flag is reported missing in {@code r}.
     */
    private static void coverRequiredFlags(Input in, List<Jewel> locked, Result r) {
        Set<String> need = new LinkedHashSet<>(in.requiredFlags);
        need.removeAll(totals(in, locked).flags);
        while (!need.isEmpty()) {
            String flag = need.iterator().next();
            Jewel best = null;
            double bestCost = Double.MAX_VALUE;
            for (Jewel j : in.pool) {
                if (locked.contains(j) || !j.flags.contains(flag) || j.size <= 0) continue;
                int covered = 0;
                for (String f : j.flags) if (need.contains(f)) covered++;
                double cost = (double) j.size / covered;
                if (cost < bestCost) {
                    bestCost = cost;
                    best = j;
                }
            }
            Craft craft = in.crafts.get(flag);
            double craftCost = craft == null ? Double.MAX_VALUE : craft.size();
            if (best != null && bestCost <= craftCost) {
                locked.add(best);
                need.removeAll(best.flags);
            } else if (craft != null) {
                Jewel c = craftPlaceholder(flag, craft);
                locked.add(c);
                r.crafts.add(c);
                need.remove(flag);
            } else {
                r.missing.add(flag);
                need.remove(flag);
            }
        }
    }

    private static Jewel craftPlaceholder(String flag, Craft craft) {
        Jewel c = new Jewel();
        c.id = "craft:" + flag;
        c.kind = Jewel.Kind.CRAFT;
        c.name = Stats.name(flag) + " Jewel";
        c.size = craft.size();
        c.flags.add(flag);
        c.craftCost = craft.cost();
        return c;
    }

    /** Pool jewels worth scoring: not locked, not a duplicate of a locked unique, and touching a goal or nice flag. */
    private static List<Jewel> candidates(Input in, List<Jewel> locked) {
        Set<String> lockedUniques = new HashSet<>();
        for (Jewel j : locked) if (j.uniqueKey != null) lockedUniques.add(j.uniqueKey);
        List<Jewel> candidates = new ArrayList<>();
        for (Jewel j : in.pool) {
            if (locked.contains(j) || j.size <= 0) continue;
            if (j.uniqueKey != null && lockedUniques.contains(j.uniqueKey)) continue;
            if (!contributes(in, j)) continue;
            candidates.add(j);
        }
        return candidates;
    }

    private static boolean contributes(Input in, Jewel j) {
        for (NumGoal g : in.goals) if (j.nums.containsKey(g.stat())) return true;
        for (String f : in.niceFlags) if (j.flags.contains(f)) return true;
        return false;
    }

    /** Score, with a tiny preference for using less capacity. */
    private static double objective(Input in, List<Jewel> sel) {
        int used = 0;
        for (Jewel j : sel) used += j.size;
        return score(in, sel) - CAPACITY_TIEBREAK * used / Math.max(1, in.capacity);
    }

    /** A working selection: its jewels, their total size and its objective value. */
    private static final class Selection {
        List<Jewel> jewels;
        int used;
        double objective;

        void set(List<Jewel> jewels, int used, double objective) {
            this.jewels = jewels;
            this.used = used;
            this.objective = objective;
        }
    }

    /** Whether {@code j} still fits in the capacity (given {@code used}) and does not repeat a unique already in {@code sel}. */
    private static boolean fits(Input in, List<Jewel> sel, int used, Jewel j) {
        return used + j.size <= in.capacity && !clashesUnique(sel, j);
    }

    private static List<Jewel> optimize(Input in, List<Jewel> locked, List<Jewel> candidates, int order) {
        Selection st = new Selection();
        List<Jewel> start = new ArrayList<>(locked);
        st.set(start, locked.stream().mapToInt(j -> j.size).sum(), objective(in, start));
        Set<Jewel> lockedSet = new HashSet<>(locked);

        greedy(in, st, candidates, order);

        // Local search: drop useless jewels, swap one for one, then fill gaps.
        for (int pass = 0; pass < LOCAL_SEARCH_PASSES; pass++) {
            boolean improved = removeOrSwap(in, st, lockedSet, candidates);
            if (!improved) improved = fillGaps(in, st, candidates);
            if (!improved) break;
        }
        return st.jewels;
    }

    /** Greedy: order 0 = best gain per size, 1 = best absolute gain, 2 = gain per size favouring bigger jewels. */
    private static void greedy(Input in, Selection st, List<Jewel> candidates, int order) {
        List<Jewel> sel = st.jewels;
        while (true) {
            Jewel pick = null;
            double pickKey = TOLERANCE;
            for (Jewel j : candidates) {
                if (sel.contains(j) || !fits(in, sel, st.used, j)) continue;
                sel.add(j);
                double d = objective(in, sel) - st.objective;
                sel.remove(sel.size() - 1);
                if (d <= TOLERANCE) continue;
                double key = switch (order) {
                    case 1 -> d;
                    case 2 -> d / Math.sqrt(j.size);
                    default -> d / j.size;
                };
                if (key > pickKey) {
                    pickKey = key;
                    pick = j;
                }
            }
            if (pick == null) break;
            sel.add(pick);
            st.used += pick.size;
            st.objective = objective(in, sel);
        }
    }

    /** Applies the first improving removal or one-for-one swap of an unlocked jewel; false if there is none. */
    private static boolean removeOrSwap(Input in, Selection st, Set<Jewel> lockedSet, List<Jewel> candidates) {
        for (int i = 0; i < st.jewels.size(); i++) {
            Jewel out = st.jewels.get(i);
            if (lockedSet.contains(out)) continue;
            List<Jewel> without = new ArrayList<>(st.jewels);
            without.remove(i);
            int usedWithout = st.used - out.size;
            double ow = objective(in, without);
            if (ow > st.objective + TOLERANCE) {
                st.set(without, usedWithout, ow);
                return true;
            }
            for (Jewel j : candidates) {
                if (st.jewels.contains(j) || !fits(in, without, usedWithout, j)) continue;
                without.add(j);
                double v = objective(in, without);
                if (v > st.objective + TOLERANCE) {
                    st.set(without, usedWithout + j.size, v);
                    return true;
                }
                without.remove(without.size() - 1);
            }
        }
        return false;
    }

    /** Adds every candidate that still fits and improves the objective; true if any was added. */
    private static boolean fillGaps(Input in, Selection st, List<Jewel> candidates) {
        boolean improved = false;
        for (Jewel j : candidates) {
            if (st.jewels.contains(j) || !fits(in, st.jewels, st.used, j)) continue;
            st.jewels.add(j);
            double v = objective(in, st.jewels);
            if (v > st.objective + TOLERANCE) {
                st.used += j.size;
                st.objective = v;
                improved = true;
            } else {
                st.jewels.remove(st.jewels.size() - 1);
            }
        }
        return improved;
    }

    private static boolean clashesUnique(List<Jewel> sel, Jewel j) {
        if (j.uniqueKey == null) return false;
        for (Jewel s : sel) if (j.uniqueKey.equals(s.uniqueKey)) return true;
        return false;
    }

    private Solver() {}
}
