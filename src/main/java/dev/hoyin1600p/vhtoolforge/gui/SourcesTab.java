package dev.hoyin1600p.vhtoolforge.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vhtoolforge.model.Source;
import dev.hoyin1600p.vhtoolforge.util.Text;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

import static dev.hoyin1600p.vhtoolforge.gui.Format.ago;
import static dev.hoyin1600p.vhtoolforge.gui.Format.shortDim;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.button;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.checkbox;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.header;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.DIM;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.NOTE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.ROW;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.TEXT;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.WHITE;
import static net.minecraft.client.gui.GuiComponent.fill;

/** The Sources tab: every scanned storage, with a tick to include it in the plan and a button to forget it. */
final class SourcesTab {
    private final UiContext ctx;

    SourcesTab(UiContext ctx) {
        this.ctx = ctx;
    }

    void render(PoseStack pose, int mx, int my) {
        int top = ctx.contentTop - ctx.scrollOf(Tab.SOURCES);
        int x = ctx.leftX;
        int y = top;
        header(ctx.font, pose, I18n.get("gui.vhtoolforge.sources.header_left"), I18n.get("gui.vhtoolforge.sources.header_right"), x, y, ctx.leftW);
        y += 14;
        if (ctx.store.sources.isEmpty()) {
            ctx.font.draw(pose, I18n.get("gui.vhtoolforge.sources.empty"), x + 6, y + 6, DIM);
            y += 24;
        }
        for (Source src : new ArrayList<>(ctx.store.sources)) {
            drawSourceRow(pose, mx, my, src, y);
            y += 26;
        }
        y += 8;
        for (FormattedCharSequence l : ctx.font.split(new TranslatableComponent("gui.vhtoolforge.sources.help"), ctx.leftW - 12)) {
            ctx.font.draw(pose, l, x + 6, y, NOTE);
            y += 10;
        }
        ctx.contentHeight = y - top + 6;
    }

    private void drawSourceRow(PoseStack pose, int mx, int my, Source src, int y) {
        int x = ctx.leftX;
        long count = ctx.store.jewels.stream().filter(j -> src.key.equals(j.sourceKey)).count();
        fill(pose, x, y, x + ctx.leftW, y + 24, ROW);
        checkbox(pose, x + 6, y + 5, src.enabled);
        ctx.hit(x + 4, y + 2, 18, 20, () -> {
            src.enabled = !src.enabled;
            ctx.changed();
        }, () -> List.of(new TranslatableComponent(src.enabled ? "tooltip.vhtoolforge.source_included" : "tooltip.vhtoolforge.source_excluded")));
        ctx.font.draw(pose, src.label, x + 28, y + 4, WHITE);
        ctx.font.draw(pose, src.kindName(), x + 28, y + 14, NOTE);
        ctx.font.draw(pose, src.where() + "  " + shortDim(src.dimension), x + 200, y + 8, DIM);
        ctx.font.draw(pose, count + " " + Text.jewels(count), x + 380, y + 8, TEXT);
        ctx.font.draw(pose, I18n.get("gui.vhtoolforge.sources.scanned", ago(src.scannedAt)), x + 470, y + 8, DIM);
        int bx = x + ctx.leftW - 60;
        button(ctx.font, pose, bx, y + 3, 52, 18, I18n.get("gui.vhtoolforge.sources.forget"), mx, my);
        ctx.hit(bx, y + 3, 52, 18, () -> {
            ctx.store.forgetSource(src.key);
            ctx.changed();
        }, () -> List.of(new TranslatableComponent("tooltip.vhtoolforge.source_forget")));
    }
}
