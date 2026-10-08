package dev.hoyin1600p.vhtoolforge.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vhtoolforge.model.Craft;
import dev.hoyin1600p.vhtoolforge.model.Jewel;
import dev.hoyin1600p.vhtoolforge.model.Settings;
import dev.hoyin1600p.vhtoolforge.pack.PackConfig;
import dev.hoyin1600p.vhtoolforge.plan.Planner;
import dev.hoyin1600p.vhtoolforge.solve.Solver;
import dev.hoyin1600p.vhtoolforge.stat.Stats;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.hoyin1600p.vhtoolforge.gui.Format.parseDouble;
import static dev.hoyin1600p.vhtoolforge.gui.Format.parseInt;
import static dev.hoyin1600p.vhtoolforge.gui.Format.trim;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.bigItem;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.checkbox;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.header;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.outline;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.slot;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.BAD;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.BLACK;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.CARD_EDGE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.CRAFT;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.DIM;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.DISABLED;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.FLAG_OFF;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.FOOTNOTE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.GOLD;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.GOOD;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.HOVER_HINT;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.NOTE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.PART;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.RANK_EDGE_SET;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.RESULT_IDLE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.ROW;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.ROW_HOVER;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.SHADE_HEAVY;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.SIZE_FIXED;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.TILE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.TILE_ON;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.TRACK;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.UNIQUE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.UNIQUE_OFF;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.WHITE;
import static net.minecraft.client.gui.GuiComponent.fill;

/**
 * The Targets tab: number targets with priorities and results, yes/no stat tiles, unique jewels,
 * and on the right the two tool previews with the plain-language summary.
 */
final class TargetsTab {
    private final UiContext ctx;
    private final PriorityDropdown dropdown;

    TargetsTab(UiContext ctx, PriorityDropdown dropdown) {
        this.ctx = ctx;
        this.dropdown = dropdown;
    }

    // ---------------------------------------------------------------- widgets

    /** The text boxes inside the scrolling list, in list order. */
    List<Placed> buildWidgets() {
        Settings s = ctx.s;
        List<Placed> out = new ArrayList<>();
        int y = 14;
        int inputX = ctx.leftX + 8 + 150;
        for (Stats.Num n : Stats.NUMS) {
            boolean off = n.hammerOnly() && !Planner.hammer(s);
            Settings.Target t = s.targets.get(n.id());
            String val = t == null || t.value == null ? "" : trim(n.toTyped(t.value));
            EditBox box = NumberBox.create(ctx.font, inputX, 0, 46, val, v -> {
                Double d = parseDouble(v);
                s.target(n.id()).value = d == null || d <= 0 ? null : n.fromTyped(d);
                ctx.changed();
            });
            box.setEditable(!off);
            out.add(new Placed(box, y + 2));
            y += 20;
        }
        y += 18;
        y += flagGridHeight();
        y += 18;
        for (PackConfig.UniqueJewel u : ctx.pack.uniques) {
            if (!u.size().fixed()) {
                Settings.UniqueChoice c = s.uniques.get(u.key());
                int size = c != null && c.size != null ? u.size().clamp(c.size) : u.size().min();
                EditBox box = NumberBox.create(ctx.font, sizeBoxX(u), 0, 30, String.valueOf(size), v -> {
                    Integer n = parseInt(v);
                    s.unique(u.key()).size = n;
                    ctx.changed();
                });
                out.add(new Placed(box, y + 3));
            }
            y += 22;
        }
        return out;
    }

    /** Unique row, right side: [Size][box][min-max]. */
    private int sizeBoxX(PackConfig.UniqueJewel u) {
        return ctx.leftX + ctx.leftW - 8 - ctx.font.width(u.size().min() + "-" + u.size().max()) - 6 - 30;
    }

    private int flagCols() {
        return Math.max(1, (ctx.leftW - 16) / 124);
    }

    private int flagGridHeight() {
        int n = 0;
        for (Stats.Flag f : Stats.FLAGS) if (!f.baseOnly() || Planner.hammer(ctx.s)) n++;
        return ((n + flagCols() - 1) / flagCols()) * 22;
    }

    // ---------------------------------------------------------------- list

    void render(PoseStack pose, int mx, int my) {
        int top = ctx.contentTop - ctx.scrollOf(Tab.TARGETS);
        int x = ctx.leftX;
        int y = top;
        header(ctx.font, pose, I18n.get("gui.vhtoolforge.targets.header_stats_left"), I18n.get("gui.vhtoolforge.targets.header_stats_right"), x, y, ctx.leftW);
        y += 14;
        Solver.Result result = ctx.solve.result();
        Solver.Input solvedInput = ctx.solve.solvedInput();
        Solver.Totals totals = result == null || solvedInput == null ? null : Solver.totals(solvedInput, result.selected);
        for (Stats.Num n : Stats.NUMS) {
            drawNumRow(pose, mx, my, n, y, totals);
            y += 20;
        }

        y += 4;
        header(ctx.font, pose, I18n.get("gui.vhtoolforge.targets.header_flags_left"), I18n.get("gui.vhtoolforge.targets.header_flags_right"), x, y, ctx.leftW);
        y += 14;
        drawFlagGrid(pose, y, totals);
        y += flagGridHeight();

        y += 4;
        header(ctx.font, pose, I18n.get("gui.vhtoolforge.targets.header_unique_left"), I18n.get("gui.vhtoolforge.targets.header_unique_right"), x, y, ctx.leftW);
        y += 14;
        for (PackConfig.UniqueJewel u : ctx.pack.uniques) {
            drawUniqueRow(pose, u, y);
            y += 22;
        }
        y += 6;
        ctx.font.draw(pose, I18n.get("gui.vhtoolforge.targets.footnote"), x + 4, y, FOOTNOTE);
        y += 14;
        ctx.contentHeight = y - top;
    }

    private void drawNumRow(PoseStack pose, int mx, int my, Stats.Num n, int y, Solver.Totals totals) {
        Settings s = ctx.s;
        int x = ctx.leftX;
        boolean off = n.hammerOnly() && !Planner.hammer(s);
        boolean hover = mx >= x && mx < x + ctx.leftW && my >= y && my < y + 19 && my >= ctx.contentTop && my < ctx.contentBottom;
        fill(pose, x, y, x + ctx.leftW, y + 19, hover ? ROW_HOVER : ROW);
        Icons.draw(pose, n.id(), x + 4, y + 2);
        int color = off ? DISABLED : BLACK | n.color();
        String name = n.name() + (n.format() == Stats.Format.PERCENT ? " %" : "");
        ctx.font.drawShadow(pose, name, x + 24, y + 6, color);
        String note = off ? I18n.get("gui.vhtoolforge.targets.hammer_only")
                : n.base() > 0 ? I18n.get("gui.vhtoolforge.targets.base_value", trim(Planner.base(s, n))) : "";
        if (!note.isEmpty()) ctx.font.draw(pose, note, x + 26 + ctx.font.width(name) + 2, y + 6, NOTE);

        int px = ctx.leftX + 8 + 150 + 52;
        Settings.Target t = s.targets.get(n.id());
        drawRankButton(pose, n, t, px, y, off);
        if (totals != null && !off) drawNumResult(pose, n, t, px + 38, y, totals);
    }

    /** The priority button: opens the dropdown unless the stat is switched off. */
    private void drawRankButton(PoseStack pose, Stats.Num n, Settings.Target t, int px, int y, boolean off) {
        Integer rank = t == null ? null : t.rank;
        fill(pose, px, y + 2, px + 30, y + 17, off ? TILE_ON : TILE);
        outline(pose, px, y + 2, 30, 15, rank != null ? RANK_EDGE_SET : DISABLED);
        String rl = rank == null ? "-" : String.valueOf(rank);
        ctx.font.draw(pose, rl + " v", px + 15 - ctx.font.width(rl + " v") / 2f, y + 6, rank != null ? GOLD : DIM);
        if (!off) {
            final int ry = y;
            ctx.hit(px, y + 2, 30, 15, () -> dropdown.open(n.id(), px, ry + 17),
                    () -> List.of(new TranslatableComponent("tooltip.vhtoolforge.priority"),
                            new TranslatableComponent("tooltip.vhtoolforge.priority_note").withStyle(ChatFormatting.GRAY)));
        }
    }

    /** The tool's total for this stat, with a progress bar toward the target when there is one. */
    private void drawNumResult(PoseStack pose, Stats.Num n, Settings.Target t, int rx, int y, Solver.Totals totals) {
        int x = ctx.leftX;
        int rw = x + ctx.leftW - 8 - rx;
        double total = Planner.base(ctx.s, n) + totals.nums.getOrDefault(n.id(), 0.0);
        Double target = t == null ? null : t.value;
        String txt = n.format(total, false);
        int barX = rx + 54;
        fill(pose, barX, y + 7, barX + Math.max(10, rw - 54), y + 12, TRACK);
        if (target != null && target > 0) {
            double f = Math.min(1, total / target);
            boolean met = f >= 0.999;
            ctx.font.draw(pose, txt, rx + 50 - ctx.font.width(txt), y + 6, met ? GOOD : PART);
            fill(pose, barX, y + 7, barX + (int) (Math.max(10, rw - 54) * f), y + 12, met ? GOOD : PART);
        } else {
            ctx.font.draw(pose, txt, rx + 50 - ctx.font.width(txt), y + 6, RESULT_IDLE);
        }
    }

    private void drawFlagGrid(PoseStack pose, int y, Solver.Totals totals) {
        int cols = flagCols();
        int tileW = (ctx.leftW - (cols - 1) * 4) / cols;
        int i = 0;
        String base = Planner.baseFlag(ctx.s);
        for (Stats.Flag f : Stats.FLAGS) {
            if (f.baseOnly() && !Planner.hammer(ctx.s)) continue;
            int tx = ctx.leftX + (i % cols) * (tileW + 4);
            int ty = y + (i / cols) * 22;
            i++;
            drawFlagTile(pose, f, tx, ty, tileW, f.id().equals(base) || f.baseOnly(), totals);
        }
    }

    private record FlagStatus(String text, int color) {}

    private FlagStatus flagStatus(Stats.Flag f, boolean locked, boolean on, Solver.Totals totals) {
        Solver.Result result = ctx.solve.result();
        if (locked) return new FlagStatus(I18n.get("gui.vhtoolforge.targets.flag_base"), DIM);
        if (!on || result == null) return new FlagStatus("", DIM);
        boolean crafted = result.crafts.stream().anyMatch(j -> j.flags.contains(f.id()));
        boolean have = totals != null && totals.flags.contains(f.id());
        if (crafted) return new FlagStatus(I18n.get("gui.vhtoolforge.targets.flag_craft"), CRAFT);
        if (have) return new FlagStatus("✔", GOOD);
        if (f.niceToHave()) return new FlagStatus(I18n.get("gui.vhtoolforge.targets.flag_skipped"), DIM);
        return new FlagStatus("✘", BAD);
    }

    private void drawFlagTile(PoseStack pose, Stats.Flag f, int tx, int ty, int tileW, boolean locked, Solver.Totals totals) {
        Settings s = ctx.s;
        boolean on = locked || s.flags.contains(f.id());
        int fc = BLACK | f.color();
        fill(pose, tx, ty, tx + tileW, ty + 20, on ? TILE_ON : TILE);
        outline(pose, tx, ty, tileW, 20, on ? fc : CARD_EDGE);
        Icons.draw(pose, f.id(), tx + 3, ty + 2);
        if (!on) fill(pose, tx + 3, ty + 2, tx + 19, ty + 18, SHADE_HEAVY);
        ctx.font.drawShadow(pose, f.name(), tx + 22, ty + 6, on ? fc : FLAG_OFF);
        FlagStatus st = flagStatus(f, locked, on, totals);
        if (!st.text().isEmpty()) ctx.font.draw(pose, st.text(), tx + tileW - 4 - ctx.font.width(st.text()), ty + 6, st.color());
        if (!locked) {
            List<Component> tip = new ArrayList<>();
            tip.add(new TextComponent(f.name()).withStyle(st2 -> st2.withColor(TextColor.fromRgb(f.color()))));
            tip.add(new TranslatableComponent(f.niceToHave() ? "tooltip.vhtoolforge.flag_nice" : "tooltip.vhtoolforge.flag_must").withStyle(ChatFormatting.GRAY));
            Craft c = ctx.pack.crafts.get(f.id());
            if (c != null) tip.add(new TranslatableComponent("tooltip.vhtoolforge.flag_craft", c.size()).withStyle(ChatFormatting.DARK_GRAY));
            ctx.hit(tx, ty, tileW, 20, () -> {
                if (!s.flags.remove(f.id())) s.flags.add(f.id());
                ctx.changed();
            }, () -> tip);
        }
    }

    private void drawUniqueRow(PoseStack pose, PackConfig.UniqueJewel u, int y) {
        Settings s = ctx.s;
        int x = ctx.leftX;
        int leftW = ctx.leftW;
        Settings.UniqueChoice c = s.uniques.get(u.key());
        boolean on = c != null && c.on;
        fill(pose, x, y, x + leftW, y + 21, ROW);
        checkbox(pose, x + 4, y + 4, on);
        Jewel stand = uniqueStandIn(u, c);
        ctx.items.renderAndDecorateFakeItem(ctx.stacks.jewel(stand), x + 20, y + 2);
        ctx.font.drawShadow(pose, u.name(), x + 40, y + 7, on ? UNIQUE : UNIQUE_OFF);
        String effText = String.join(", ", effectNames(u));
        int effX = x + 140;
        String sizeWord = I18n.get("gui.vhtoolforge.targets.size_label");
        String sizeTxt = u.size().fixed() ? I18n.get("gui.vhtoolforge.targets.size_fixed", u.size().min()) : u.size().min() + "-" + u.size().max();
        int sizeLeft = u.size().fixed() ? x + leftW - 8 - ctx.font.width(sizeTxt) : sizeBoxX(u) - 6 - ctx.font.width(sizeWord);
        int effW = Math.max(20, sizeLeft - 10 - effX);
        String shown = ctx.font.plainSubstrByWidth(effText, effW);
        if (shown.length() < effText.length()) shown = ctx.font.plainSubstrByWidth(effText, effW - ctx.font.width("…")) + "…";
        ctx.font.draw(pose, shown, effX, y + 7, DIM);
        ctx.font.draw(pose, sizeTxt, x + leftW - 8 - ctx.font.width(sizeTxt), y + 7, u.size().fixed() ? SIZE_FIXED : DIM);
        if (!u.size().fixed()) ctx.font.draw(pose, sizeWord, sizeLeft, y + 7, DIM);
        ctx.hit(x, y, 140 + effW, 21, () -> {
            s.unique(u.key()).on = !on;
            ctx.changed();
        }, () -> {
            List<Component> tip = new ArrayList<>(ToolStacks.jewelStack(stand).getTooltipLines(ctx.minecraft.player, TooltipFlag.Default.NORMAL));
            if (u.lore() != null) tip.add(new TextComponent(u.lore()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return tip;
        });
    }

    private static List<String> effectNames(PackConfig.UniqueJewel u) {
        List<String> eff = new ArrayList<>();
        for (String f : u.flags()) eff.add(Stats.name(f));
        for (Map.Entry<String, Double> e : u.nums().entrySet()) eff.add(Stats.name(e.getKey()));
        eff.addAll(u.extras());
        return eff;
    }

    private Jewel uniqueStandIn(PackConfig.UniqueJewel u, Settings.UniqueChoice c) {
        Jewel j = new Jewel();
        j.name = u.name();
        j.size = u.size().fixed() ? u.size().min() : u.size().clamp(c != null && c.size != null ? c.size : u.size().min());
        j.nums.putAll(u.nums());
        j.flags.addAll(u.flags());
        j.uniqueKey = u.key();
        return j;
    }

    // ---------------------------------------------------------------- preview column

    void renderPreviewColumn(PoseStack pose) {
        Solver.Result result = ctx.solve.result();
        int x = ctx.rightX;
        int y = ctx.contentTop;
        int half = (ctx.rightW - 8) / 2;
        toolCard(pose, x, y, half, I18n.get("gui.vhtoolforge.preview.as_entered"), ctx.solve.enteredTool());
        toolCard(pose, x + half + 8, y, half, I18n.get("gui.vhtoolforge.preview.as_built"), ctx.solve.builtTool());
        y += 96;

        int bw = (ctx.rightW - 16) / 3;
        String pct = result == null ? "…" : result.maxScore <= 0 ? "-" : Math.round(result.percent() * 100) + "%";
        statBox(pose, x, y, bw, pct, I18n.get("gui.vhtoolforge.preview.of_targets"));
        statBox(pose, x + bw + 8, y, bw, result == null ? "…" : String.valueOf(result.selected.size()), I18n.get("gui.vhtoolforge.preview.jewels_used"));
        statBox(pose, x + 2 * (bw + 8), y, bw, result == null ? "…" : String.valueOf(result.capacity - result.used), I18n.get("gui.vhtoolforge.preview.capacity_left"));
        y += 36;
        drawWhyBox(pose, x, y);
    }

    /** The plain-language summary of the current result, one line per fact. */
    private List<String> whyLines() {
        Solver.Result result = ctx.solve.result();
        Solver.Input in = ctx.solve.solvedInput();
        List<String> why = new ArrayList<>();
        if (ctx.solve.isPending() || result == null || in == null) {
            why.add(I18n.get("gui.vhtoolforge.why.working"));
            return why;
        }
        if (result.over) why.add(I18n.get("gui.vhtoolforge.why.over"));
        Solver.Totals t = Solver.totals(in, result.selected);
        List<String> short_ = new ArrayList<>();
        for (Solver.NumGoal g : in.goals) if (g.base() + t.nums.getOrDefault(g.stat(), 0.0) < g.target() * 0.999) short_.add(Stats.name(g.stat()));
        if (in.goals.isEmpty()) why.add(I18n.get("gui.vhtoolforge.why.enter_target"));
        else why.add(short_.isEmpty() ? I18n.get("gui.vhtoolforge.why.all_met") : I18n.get("gui.vhtoolforge.why.short_on", String.join(", ", short_)));
        if (!result.crafts.isEmpty()) {
            int c = result.crafts.stream().mapToInt(j -> j.size).sum();
            List<String> names = new ArrayList<>();
            for (Jewel j : result.crafts) names.add(Stats.name(j.flags.iterator().next()));
            why.add(I18n.get("gui.vhtoolforge.why.reserved", c, String.join(", ", names)));
        }
        if (!result.missing.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (String f : result.missing) names.add(Stats.name(f));
            why.add(I18n.get("gui.vhtoolforge.why.no_recipe", String.join(", ", names)));
        }
        int lvl = Math.max(Planner.material(ctx.s).getLevel(), Planner.maxLevel(result.selected));
        why.add(lvl > Planner.material(ctx.s).getLevel() ? I18n.get("gui.vhtoolforge.why.level_rises", lvl) : I18n.get("gui.vhtoolforge.why.level_stays", lvl));
        if (ctx.store.jewels.isEmpty()) why.add(I18n.get("gui.vhtoolforge.why.none_scanned"));
        return why;
    }

    private void drawWhyBox(PoseStack pose, int x, int y) {
        int rightW = ctx.rightW;
        int boxH = 10;
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String w : whyLines()) lines.addAll(ctx.font.split(new TextComponent(w), rightW - 16));
        boxH += lines.size() * 10;
        fill(pose, x, y, x + rightW, y + boxH, TILE);
        outline(pose, x, y, rightW, boxH, CARD_EDGE);
        int ly = y + 6;
        for (FormattedCharSequence l : lines) {
            ctx.font.draw(pose, l, x + 8, ly, DIM);
            ly += 10;
        }
    }

    private void toolCard(PoseStack pose, int x, int y, int w, String label, ItemStack stack) {
        fill(pose, x, y, x + w, y + 90, TILE);
        outline(pose, x, y, w, 90, CARD_EDGE);
        ctx.font.draw(pose, label, x + w / 2f - ctx.font.width(label) / 2f, y + 6, DIM);
        int sx = x + w / 2 - 22;
        slot(pose, sx, y + 18, 44, false);
        if (!stack.isEmpty()) {
            bigItem(ctx.items, stack, sx + 6, y + 24, 2f);
            String name = stack.getHoverName().getString();
            String shown = ctx.font.plainSubstrByWidth(name, w - 8);
            ctx.font.draw(pose, shown, x + w / 2f - ctx.font.width(shown) / 2f, y + 68, WHITE);
            ctx.hits.add(new HitArea(sx, y + 18, 44, 44, null, null, stack));
        }
        String hover = I18n.get("gui.vhtoolforge.preview.hover");
        ctx.font.draw(pose, hover, x + w / 2f - ctx.font.width(hover) / 2f, y + 79, HOVER_HINT);
    }

    private void statBox(PoseStack pose, int x, int y, int w, String big, String small) {
        fill(pose, x, y, x + w, y + 30, TILE);
        outline(pose, x, y, w, 30, CARD_EDGE);
        pose.pushPose();
        pose.translate(x + 6, y + 4, 0);
        pose.scale(1.5f, 1.5f, 1);
        ctx.font.draw(pose, big, 0, 0, WHITE);
        pose.popPose();
        ctx.font.draw(pose, small, x + 6, y + 19, DIM);
    }
}
