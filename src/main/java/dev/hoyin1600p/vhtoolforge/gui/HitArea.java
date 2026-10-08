package dev.hoyin1600p.vhtoolforge.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * A clickable or hoverable area drawn by hand. The screen rebuilds the list every frame; later entries sit on
 * top of earlier ones. An area can run an action when clicked and show a text tooltip or an item tooltip.
 */
record HitArea(int x, int y, int w, int h, Runnable action, Supplier<List<Component>> tooltip, ItemStack tooltipStack) {
    boolean in(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Areas in the scrolling list only count while the mouse is inside the visible part of it. */
    private boolean hiddenByScroll(UiContext ctx, double my) {
        boolean inList = y >= ctx.contentTop - 1 && x < ctx.leftX + ctx.leftW && x >= ctx.leftX;
        return inList && (my < ctx.contentTop || my >= ctx.contentBottom);
    }

    /** The topmost area under the mouse that has an action, or null. */
    static HitArea clickable(UiContext ctx, double mx, double my) {
        for (int i = ctx.hits.size() - 1; i >= 0; i--) {
            HitArea h = ctx.hits.get(i);
            if (h.action() == null || !h.in(mx, my)) continue;
            if (h.hiddenByScroll(ctx, my)) continue;
            return h;
        }
        return null;
    }

    /** The topmost area under the mouse that has a tooltip, or null. */
    static HitArea tooltipped(UiContext ctx, double mx, double my) {
        for (int i = ctx.hits.size() - 1; i >= 0; i--) {
            HitArea h = ctx.hits.get(i);
            if (!h.in(mx, my)) continue;
            if (h.hiddenByScroll(ctx, my)) continue;
            if (h.tooltipStack() != null || h.tooltip() != null) return h;
        }
        return null;
    }
}
