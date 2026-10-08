package dev.hoyin1600p.vhtoolforge.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

import static dev.hoyin1600p.vhtoolforge.gui.Palette.BLACK;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.BUTTON;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.BUTTON_HOVER;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.BUTTON_SHADE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.BUTTON_SHADE_HOVER;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.CHECK_EDGE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.CHECK_ON;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.DIM;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.EDGE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.PANEL;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.PANEL_HI;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.PANEL_LO;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.SLOT;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.SLOT_DARK;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.SLOT_SELECTED;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.WHITE;
import static net.minecraft.client.gui.GuiComponent.fill;

/** The small drawing primitives of the planner screen: bevels, slots, checkboxes, buttons and scissoring. */
final class GuiDraw {
    static void bevel(PoseStack pose, int x, int y, int w, int h) {
        fill(pose, x, y, x + w, y + h, EDGE);
        fill(pose, x + 1, y + 1, x + w - 1, y + h - 1, PANEL_HI);
        fill(pose, x + 3, y + 3, x + w - 1, y + h - 1, PANEL_LO);
        fill(pose, x + 3, y + 3, x + w - 3, y + h - 3, PANEL);
    }

    static void slot(PoseStack pose, int x, int y, int size, boolean selected) {
        if (selected) fill(pose, x - 2, y - 2, x + size + 2, y + size + 2, WHITE);
        fill(pose, x, y, x + size, y + size, SLOT_DARK);
        fill(pose, x + 1, y + 1, x + size, y + size, PANEL_HI);
        fill(pose, x + 1, y + 1, x + size - 1, y + size - 1, selected ? SLOT_SELECTED : SLOT);
    }

    static void checkbox(PoseStack pose, int x, int y, boolean on) {
        fill(pose, x, y, x + 12, y + 12, CHECK_EDGE);
        fill(pose, x + 1, y + 1, x + 11, y + 11, BLACK);
        if (on) fill(pose, x + 3, y + 3, x + 9, y + 9, CHECK_ON);
    }

    static void outline(PoseStack pose, int x, int y, int w, int h, int color) {
        fill(pose, x, y, x + w, y + 1, color);
        fill(pose, x, y + h - 1, x + w, y + h, color);
        fill(pose, x, y, x + 1, y + h, color);
        fill(pose, x + w - 1, y, x + w, y + h, color);
    }

    static void button(Font font, PoseStack pose, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        fill(pose, x, y, x + w, y + h, EDGE);
        fill(pose, x + 1, y + 1, x + w - 1, y + h - 1, hov ? BUTTON_HOVER : BUTTON);
        fill(pose, x + 1, y + h - 3, x + w - 1, y + h - 1, hov ? BUTTON_SHADE_HOVER : BUTTON_SHADE);
        font.drawShadow(pose, label, x + w / 2f - font.width(label) / 2f, y + (h - 8) / 2f, WHITE);
    }

    /** Section header: a label on the left and one right-aligned to {@code x + width}. */
    static void header(Font font, PoseStack pose, String left, String right, int x, int y, int width) {
        font.draw(pose, left, x + 2, y + 2, DIM);
        font.draw(pose, right, x + width - 2 - font.width(right), y + 2, DIM);
    }

    /** An item drawn at {@code scale} times its normal size. */
    static void bigItem(ItemRenderer items, ItemStack stack, int x, int y, float scale) {
        PoseStack mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.translate(x, y, 0);
        mv.scale(scale, scale, 1);
        RenderSystem.applyModelViewMatrix();
        items.renderAndDecorateFakeItem(stack, 0, 0);
        mv.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    static void enableScissor(int x1, int y1, int x2, int y2) {
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        int wh = mc.getWindow().getHeight();
        RenderSystem.enableScissor((int) (x1 * scale), (int) (wh - y2 * scale), (int) ((x2 - x1) * scale), (int) ((y2 - y1) * scale));
    }

    static void disableScissor() {
        RenderSystem.disableScissor();
    }

    private GuiDraw() {}
}
