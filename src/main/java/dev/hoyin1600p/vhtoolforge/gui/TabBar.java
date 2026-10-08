package dev.hoyin1600p.vhtoolforge.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.language.I18n;

import java.util.function.Consumer;

import static dev.hoyin1600p.vhtoolforge.gui.Palette.DIM;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.EDGE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.INSET;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.TAB_IDLE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.WHITE;
import static net.minecraft.client.gui.GuiComponent.fill;

/** The row of tab buttons above the content area. */
final class TabBar {
    private final UiContext ctx;
    private final Consumer<Tab> onSelect;

    TabBar(UiContext ctx, Consumer<Tab> onSelect) {
        this.ctx = ctx;
        this.onSelect = onSelect;
    }

    void render(PoseStack pose, Tab current) {
        int x = 16;
        for (Tab t : Tab.values()) {
            String label = switch (t) {
                case TARGETS -> I18n.get("gui.vhtoolforge.tabs.targets");
                case SOURCES -> I18n.get("gui.vhtoolforge.tabs.sources", ctx.store.sources.size());
                case PLAN -> I18n.get("gui.vhtoolforge.tabs.plan");
            };
            int w = ctx.font.width(label) + 20;
            boolean sel = t == current;
            fill(pose, x, 70, x + w, ctx.contentTop - 6, EDGE);
            fill(pose, x + 1, 71, x + w - 1, ctx.contentTop - 6, sel ? INSET : TAB_IDLE);
            ctx.font.draw(pose, label, x + 10, 77, sel ? WHITE : DIM);
            ctx.hit(x, 70, w, ctx.contentTop - 76, () -> onSelect.accept(t), null);
            x += w + 3;
        }
    }
}
