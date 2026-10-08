package dev.hoyin1600p.vhtoolforge.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vhtoolforge.collect.Backpacks;
import dev.hoyin1600p.vhtoolforge.model.Jewel;
import dev.hoyin1600p.vhtoolforge.model.Source;
import dev.hoyin1600p.vhtoolforge.solve.Solver;
import dev.hoyin1600p.vhtoolforge.util.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.button;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.outline;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.slot;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.BACKPACK_SELECTED;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.BAD;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.CARD_EDGE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.CRAFT;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.DIM;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.GOOD;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.NOTE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.ROW;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.SHADE_COLLECTED;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.TEXT;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.TILE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.WHITE;
import static net.minecraft.client.gui.GuiComponent.fill;

/** The Plan tab: the chosen jewels grouped by where they are, and the target backpack column. */
final class PlanTab {
    private final UiContext ctx;

    PlanTab(UiContext ctx) {
        this.ctx = ctx;
    }

    private List<Jewel> planList() {
        if (ctx.store.plan != null) return ctx.store.plan;
        Solver.Result result = ctx.solve.result();
        return result == null ? List.of() : result.selected;
    }

    // ---------------------------------------------------------------- groups

    void render(PoseStack pose) {
        int top = ctx.contentTop - ctx.scrollOf(Tab.PLAN);
        int x = ctx.leftX;
        int y = top;
        List<Jewel> plan = planList();
        if (plan.isEmpty()) {
            ctx.font.draw(pose, I18n.get("gui.vhtoolforge.plan.empty"), x + 6, y + 8, DIM);
            ctx.contentHeight = 30;
            return;
        }
        for (Map.Entry<String, List<Jewel>> e : groupByOrigin(plan).entrySet()) {
            List<Jewel> js = e.getValue();
            if (js.isEmpty()) continue;
            y += drawGroup(pose, e.getKey(), js, y) + 6;
        }
        ctx.contentHeight = y - top;
    }

    /** Jewels by origin: each scanned storage in order, then "unique", "craft" and "gone" (storage forgotten). */
    private Map<String, List<Jewel>> groupByOrigin(List<Jewel> plan) {
        Map<String, List<Jewel>> groups = new LinkedHashMap<>();
        for (Source src : ctx.store.sources) groups.put(src.key, new ArrayList<>());
        groups.put("unique", new ArrayList<>());
        groups.put("craft", new ArrayList<>());
        groups.put("gone", new ArrayList<>());
        for (Jewel j : plan) {
            String g = j.kind == Jewel.Kind.CRAFT ? "craft" : j.kind == Jewel.Kind.UNIQUE_TICKED ? "unique"
                    : groups.containsKey(j.sourceKey) ? j.sourceKey : "gone";
            groups.get(g).add(j);
        }
        return groups;
    }

    private record GroupTitle(String title, String sub) {}

    private GroupTitle groupTitle(String key) {
        return switch (key) {
            case "unique" -> new GroupTitle(I18n.get("gui.vhtoolforge.plan.group_unique"), I18n.get("gui.vhtoolforge.plan.group_unique_sub"));
            case "craft" -> new GroupTitle(I18n.get("gui.vhtoolforge.plan.group_craft"), I18n.get("gui.vhtoolforge.plan.group_craft_sub"));
            case "gone" -> new GroupTitle(I18n.get("gui.vhtoolforge.plan.group_gone"), "");
            default -> {
                Source src = ctx.store.source(key);
                yield new GroupTitle(src.label, I18n.get("gui.vhtoolforge.plan.group_source_sub", src.kindName(), src.where()));
            }
        };
    }

    /** Draws one group's panel at {@code y} and returns the panel height. */
    private int drawGroup(PoseStack pose, String key, List<Jewel> js, int y) {
        int x = ctx.leftX;
        int leftW = ctx.leftW;
        int perRow = Math.max(1, (leftW - 20) / 20);
        GroupTitle title = groupTitle(key);
        long done = js.stream().filter(j -> ctx.store.collected.contains(j.id)).count();
        int rows = (js.size() + perRow - 1) / perRow;
        int gh = 22 + rows * 20 + 6;
        fill(pose, x, y, x + leftW, y + gh, TILE);
        outline(pose, x, y, leftW, gh, CARD_EDGE);
        ctx.font.draw(pose, title.title(), x + 8, y + 7, WHITE);
        ctx.font.draw(pose, title.sub(), x + 14 + ctx.font.width(title.title()), y + 7, NOTE);
        String cnt = key.equals("craft") || key.equals("unique") ? js.size() + " " + Text.jewels(js.size())
                : I18n.get("gui.vhtoolforge.plan.in_backpack", done, js.size());
        ctx.font.draw(pose, cnt, x + leftW - 8 - ctx.font.width(cnt), y + 7, done == js.size() && done > 0 ? GOOD : DIM);
        int gy = y + 22;
        for (int k = 0; k < js.size(); k++) {
            drawJewelSlot(pose, js.get(k), x + 8 + (k % perRow) * 20, gy + (k / perRow) * 20);
        }
        return gh;
    }

    private void drawJewelSlot(PoseStack pose, Jewel j, int jx, int jy) {
        slot(pose, jx, jy, 18, false);
        ItemStack st = ctx.stacks.jewel(j);
        ctx.items.renderAndDecorateFakeItem(st, jx + 1, jy + 1);
        pose.pushPose();
        pose.translate(0, 0, 300);
        pose.scale(0.5f, 0.5f, 1);
        ctx.font.drawShadow(pose, String.valueOf(j.size), (jx + 2) * 2, (jy + 1) * 2, WHITE);
        pose.popPose();
        if (ctx.store.collected.contains(j.id)) {
            fill(pose, jx + 1, jy + 1, jx + 17, jy + 17, SHADE_COLLECTED);
            pose.pushPose();
            pose.translate(0, 0, 300);
            ctx.font.drawShadow(pose, "✔", jx + 10, jy + 9, GOOD);
            pose.popPose();
        }
        ctx.hit(jx, jy, 18, 18, null, () -> jewelTooltip(j, st));
    }

    private List<Component> jewelTooltip(Jewel j, ItemStack st) {
        List<Component> tip = new ArrayList<>(st.getTooltipLines(ctx.minecraft.player, TooltipFlag.Default.NORMAL));
        if (j.kind == Jewel.Kind.CRAFT) {
            tip.add(new TranslatableComponent("tooltip.vhtoolforge.jewel_craft").withStyle(ChatFormatting.GRAY));
            tip.add(new TextComponent(j.craftCost == null ? "?" : j.craftCost).withStyle(ChatFormatting.YELLOW));
        } else if (j.kind == Jewel.Kind.UNIQUE_TICKED) {
            tip.add(new TranslatableComponent("tooltip.vhtoolforge.jewel_unique").withStyle(ChatFormatting.GRAY));
        } else {
            Source src = ctx.store.source(j.sourceKey);
            if (src != null) tip.add(new TranslatableComponent("tooltip.vhtoolforge.jewel_in", src.label, src.where()).withStyle(ChatFormatting.GRAY));
            if (ctx.store.collected.contains(j.id)) tip.add(new TranslatableComponent("tooltip.vhtoolforge.jewel_in_backpack").withStyle(ChatFormatting.GREEN));
        }
        return tip;
    }

    // ---------------------------------------------------------------- backpack column

    void renderBackpackColumn(PoseStack pose, int mx, int my) {
        int x = ctx.rightX;
        int y = ctx.contentTop;
        int h = 200;
        fill(pose, x, y, x + ctx.rightW, y + h, TILE);
        outline(pose, x, y, ctx.rightW, h, CARD_EDGE);
        ctx.font.draw(pose, I18n.get("gui.vhtoolforge.plan.backpack_title"), x + 8, y + 8, DIM);
        int by = drawBackpackChoices(pose, x, y + 22);
        by += 6;
        by = drawSteps(pose, x, by);
        by += 8;
        drawLockButton(pose, mx, my, x, by);
    }

    /** The backpacks the player can pick, or a note that there are none; returns the y below them. */
    private int drawBackpackChoices(PoseStack pose, int x, int startY) {
        int rightW = ctx.rightW;
        int by = startY;
        List<Backpacks.Found> all = Backpacks.all();
        Backpacks.Found chosen = Backpacks.chosen(ctx.s);
        if (all.isEmpty()) {
            for (FormattedCharSequence l : ctx.font.split(new TranslatableComponent("gui.vhtoolforge.plan.no_backpack"), rightW - 16)) {
                ctx.font.draw(pose, l, x + 8, by, BAD);
                by += 10;
            }
            return by;
        }
        for (Backpacks.Found f : all) {
            boolean sel = chosen != null && chosen.slot() == f.slot();
            fill(pose, x + 6, by, x + rightW - 6, by + 20, sel ? BACKPACK_SELECTED : ROW);
            outline(pose, x + 6, by, rightW - 12, 20, sel ? GOOD : CARD_EDGE);
            ctx.items.renderAndDecorateFakeItem(f.stack(), x + 9, by + 2);
            String label = I18n.get("gui.vhtoolforge.plan.backpack_label", f.stack().getHoverName().getString(), Backpacks.slotName(f.slot()));
            ctx.font.draw(pose, ctx.font.plainSubstrByWidth(label, rightW - 40), x + 28, by + 6, sel ? WHITE : TEXT);
            ctx.hit(x + 6, by, rightW - 12, 20, () -> {
                Backpacks.choose(ctx.s, f);
                ctx.changed();
            }, () -> List.of(new TranslatableComponent("tooltip.vhtoolforge.choose_backpack")));
            by += 22;
        }
        return by;
    }

    private int drawSteps(PoseStack pose, int x, int startY) {
        int by = startY;
        String[] steps = {
                "gui.vhtoolforge.plan.step_1",
                "gui.vhtoolforge.plan.step_2",
                "gui.vhtoolforge.plan.step_3",
                "gui.vhtoolforge.plan.step_4"};
        for (String st : steps) {
            for (FormattedCharSequence l : ctx.font.split(new TranslatableComponent(st), ctx.rightW - 16)) {
                ctx.font.draw(pose, l, x + 8, by, DIM);
                by += 10;
            }
        }
        return by;
    }

    private void drawLockButton(PoseStack pose, int mx, int my, int x, int by) {
        String lockLabel = I18n.get(ctx.store.plan == null ? "gui.vhtoolforge.plan.lock" : "gui.vhtoolforge.plan.unlock");
        button(ctx.font, pose, x + 8, by, ctx.rightW - 16, 20, lockLabel, mx, my);
        ctx.hit(x + 8, by, ctx.rightW - 16, 20, () -> {
            if (ctx.store.plan == null) {
                Solver.Result result = ctx.solve.result();
                if (result != null) ctx.store.lockPlan(result.selected);
            } else {
                ctx.store.unlockPlan();
                ctx.changed();
            }
        }, () -> List.of(new TranslatableComponent(ctx.store.plan == null
                ? "tooltip.vhtoolforge.lock_plan" : "tooltip.vhtoolforge.unlock_plan")));
    }
}
