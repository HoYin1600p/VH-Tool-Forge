package dev.hoyin1600p.vhtoolforge.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vhtoolforge.model.Settings;
import dev.hoyin1600p.vhtoolforge.plan.Planner;

import java.util.Objects;

import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.outline;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.DROPDOWN_BG;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.DROPDOWN_EDGE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.DROPDOWN_HOVER;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.GOLD;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.WHITE;
import static net.minecraft.client.gui.GuiComponent.fill;

/** The small rank picker that opens under a stat's priority button: "-" for unranked, then 1 up to the rank count. */
final class PriorityDropdown {
    private final UiContext ctx;
    private String stat;
    private int x, y;
    private int shownHeight, shownTop;

    PriorityDropdown(UiContext ctx) {
        this.ctx = ctx;
    }

    boolean isOpen() {
        return stat != null;
    }

    void open(String stat, int x, int y) {
        this.stat = stat;
        this.x = x;
        this.y = y;
    }

    void close() {
        stat = null;
    }

    void render(PoseStack pose, int mx, int my) {
        int n = Planner.rankCount(ctx.s);
        int h = (n + 1) * 12 + 4;
        int top = y;
        if (top + h > ctx.height - 4) top = Math.max(4, y - 17 - h);
        pose.pushPose();
        pose.translate(0, 0, 400);
        fill(pose, x, top, x + 30, top + h, DROPDOWN_BG);
        outline(pose, x, top, 30, h, DROPDOWN_EDGE);
        Settings.Target t = ctx.s.targets.get(stat);
        for (int i = 0; i <= n; i++) {
            int oy = top + 2 + i * 12;
            boolean hov = mx >= x && mx < x + 30 && my >= oy && my < oy + 12;
            Integer val = i == 0 ? null : i;
            boolean cur = t != null && Objects.equals(t.rank, val);
            if (hov) fill(pose, x + 1, oy, x + 29, oy + 12, DROPDOWN_HOVER);
            String label = val == null ? "-" : String.valueOf(val);
            ctx.font.draw(pose, label, x + 15 - ctx.font.width(label) / 2f, oy + 2, cur ? GOLD : WHITE);
        }
        pose.popPose();
        shownHeight = h;
        shownTop = top;
    }

    /** Picks the rank under the mouse, if any, and closes the dropdown. */
    void click(double mx, double my) {
        if (mx >= x && mx < x + 30 && my >= shownTop && my < shownTop + shownHeight) {
            int i = (int) ((my - shownTop - 2) / 12);
            int n = Planner.rankCount(ctx.s);
            if (i >= 0 && i <= n) {
                ctx.s.target(stat).rank = i == 0 ? null : i;
                ctx.changed();
            }
        }
        stat = null;
    }
}
