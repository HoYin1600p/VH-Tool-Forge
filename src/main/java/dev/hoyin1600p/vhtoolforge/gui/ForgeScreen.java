package dev.hoyin1600p.vhtoolforge.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vhtoolforge.data.Store;
import dev.hoyin1600p.vhtoolforge.input.Keys;
import dev.hoyin1600p.vhtoolforge.model.Settings;
import dev.hoyin1600p.vhtoolforge.pack.PackConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.bevel;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.button;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.disableScissor;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.enableScissor;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.HINT_LIVE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.HINT_LOCKED;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.INSET;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.SCROLL_THUMB;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.SHADE_LIGHT;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.TITLE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.TRACK;

/**
 * The planner: tool tier and base type on top, then three tabs.
 * Targets: number targets with priorities, yes/no stats, unique jewels, and the two tool previews.
 * Sources: scanned storages. Plan: the chosen jewels grouped by where they are, and the target backpack.
 * This class lays the screen out, routes input and orders the drawing; the panels draw themselves.
 */
public final class ForgeScreen extends Screen {
    private static Tab lastTab = Tab.TARGETS;

    private final UiContext ctx;
    private final PriorityDropdown dropdown;
    private final TabBar tabBar;
    private final TargetsTab targetsTab;
    private final SourcesTab sourcesTab;
    private final PlanTab planTab;
    private ToolPickerPanel picker;
    private Tab tab = lastTab;

    /** Widgets that live inside the scrolling list. */
    private final List<Placed> placed = new ArrayList<>();

    /** Creates the planner on the current world's saved settings, at the tab it was last left on. */
    public ForgeScreen() {
        super(new TranslatableComponent("gui.vhtoolforge.title"));
        Store store = Store.get();
        ctx = new UiContext(store, PackConfig.get(), new SolveService(store));
        ctx.relayout = () -> init(minecraft, width, height);
        dropdown = new PriorityDropdown(ctx);
        tabBar = new TabBar(ctx, this::setTab);
        targetsTab = new TargetsTab(ctx, dropdown);
        sourcesTab = new SourcesTab(ctx);
        planTab = new PlanTab(ctx);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- layout and widgets

    @Override
    protected void init() {
        clearWidgets();
        placed.clear();
        ctx.minecraft = minecraft;
        ctx.font = font;
        ctx.items = itemRenderer;
        ctx.width = width;
        ctx.height = height;
        ctx.contentTop = 92;
        ctx.contentBottom = height - 10;
        ctx.leftX = 16;
        int inner = width - 32;
        if (tab == Tab.SOURCES) {
            ctx.leftW = inner;
        } else {
            ctx.leftW = Math.max(220, (int) (inner * 0.60));
        }
        ctx.rightX = ctx.leftX + ctx.leftW + 10;
        ctx.rightW = width - 16 - ctx.rightX;

        picker = new ToolPickerPanel(ctx);
        addRenderableWidget(picker.maxLevelBox());
        addRenderableWidget(picker.extraCapBox());

        if (tab == Tab.TARGETS) {
            for (Placed p : targetsTab.buildWidgets()) {
                placed.add(p);
                addRenderableWidget((EditBox) p.w());
            }
        }
    }

    private void positionPlaced() {
        int off = ctx.scrollOf(tab);
        for (Placed p : placed) {
            p.w().y = ctx.contentTop + p.baseY() - off;
            p.w().visible = p.w().y >= ctx.contentTop && p.w().y + p.w().getHeight() <= ctx.contentBottom;
        }
    }

    private void setTab(Tab t) {
        tab = t;
        lastTab = t;
        dropdown.close();
        init(minecraft, width, height);
    }

    @Override
    public void tick() {
        picker.tick();
        for (Placed p : placed) if (p.w() instanceof EditBox eb) eb.tick();
        ctx.solve.tick();
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void render(PoseStack pose, int mx, int my, float pt) {
        ctx.hits.clear();
        renderBackground(pose);
        bevel(pose, 6, 6, width - 12, height - 12);
        drawTopBar(pose, mx, my);

        picker.render(pose);
        tabBar.render(pose, tab);

        // tab body
        fill(pose, 10, ctx.contentTop - 6, width - 10, height - 8, INSET);
        positionPlaced();
        enableScissor(ctx.leftX, ctx.contentTop, ctx.leftX + ctx.leftW, ctx.contentBottom);
        switch (tab) {
            case TARGETS -> targetsTab.render(pose, mx, my);
            case SOURCES -> sourcesTab.render(pose, mx, my);
            case PLAN -> planTab.render(pose);
        }
        // widgets inside the list are drawn under the scissor
        for (Placed p : placed) p.w().render(pose, mx, my, pt);
        disableScissor();
        if (tab == Tab.TARGETS) targetsTab.renderPreviewColumn(pose);
        if (tab == Tab.PLAN) planTab.renderBackpackColumn(pose, mx, my);

        picker.renderBoxes(pose, mx, my, pt);
        drawScrollbar(pose);

        if (dropdown.isOpen()) dropdown.render(pose, mx, my);
        else drawHoverTooltip(pose, mx, my);
    }

    /** Title, the Scan and Collect buttons switch, and the plan-locked hint. */
    private void drawTopBar(PoseStack pose, int mx, int my) {
        Settings s = ctx.s;
        font.draw(pose, I18n.get("gui.vhtoolforge.title"), 16, 14, TITLE);
        String toggle = I18n.get("gui.vhtoolforge.topbar.buttons_toggle",
                I18n.get(s.showStorageButtons ? "gui.vhtoolforge.topbar.on" : "gui.vhtoolforge.topbar.off"));
        int tw = font.width(toggle) + 16;
        int tx = width - 16 - tw;
        button(font, pose, tx, 9, tw, 16, toggle, mx, my);
        if (!s.showStorageButtons) fill(pose, tx + 1, 10, tx + tw - 1, 24, SHADE_LIGHT);
        ctx.hit(tx, 9, tw, 16, () -> {
            s.showStorageButtons = !s.showStorageButtons;
            ctx.changed();
        }, () -> List.of(new TranslatableComponent("tooltip.vhtoolforge.storage_buttons"),
                new TranslatableComponent("tooltip.vhtoolforge.storage_buttons_note").withStyle(ChatFormatting.GRAY)));
        boolean locked = ctx.store.plan != null;
        String hint = I18n.get(locked ? "gui.vhtoolforge.topbar.plan_locked" : "gui.vhtoolforge.topbar.plan_live");
        font.draw(pose, hint, tx - 10 - font.width(hint), 14, locked ? HINT_LOCKED : HINT_LIVE);
    }

    private void drawScrollbar(PoseStack pose) {
        int view = ctx.contentBottom - ctx.contentTop;
        if (ctx.contentHeight <= view) {
            ctx.scroll.put(tab, 0);
            return;
        }
        int cur = Math.min(ctx.scrollOf(tab), ctx.contentHeight - view);
        ctx.scroll.put(tab, cur);
        int x = ctx.leftX + ctx.leftW + 2;
        int barH = Math.max(16, view * view / ctx.contentHeight);
        int barY = ctx.contentTop + (int) ((long) (view - barH) * cur / (ctx.contentHeight - view));
        fill(pose, x, ctx.contentTop, x + 4, ctx.contentBottom, TRACK);
        fill(pose, x, barY, x + 4, barY + barH, SCROLL_THUMB);
    }

    private void drawHoverTooltip(PoseStack pose, int mx, int my) {
        HitArea h = HitArea.tooltipped(ctx, mx, my);
        if (h == null) return;
        if (h.tooltipStack() != null) renderTooltip(pose, h.tooltipStack(), mx, my);
        else renderComponentTooltip(pose, h.tooltip().get(), mx, my);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (dropdown.isOpen()) {
            dropdown.click(mx, my);
            return true;
        }
        if (super.mouseClicked(mx, my, button)) return true;
        HitArea h = HitArea.clickable(ctx, mx, my);
        if (h != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.UI_BUTTON_CLICK, 1.0f));
            h.action().run();
            return true;
        }
        // clicking empty space drops text focus
        setFocused(null);
        picker.unfocus();
        for (Placed p : placed) if (p.w() instanceof EditBox eb) eb.setFocus(false);
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (dropdown.isOpen()) return true;
        int view = ctx.contentBottom - ctx.contentTop;
        int max = Math.max(0, ctx.contentHeight - view);
        int cur = ctx.scrollOf(tab);
        int next = (int) Math.max(0, Math.min(max, cur - delta * 20));
        ctx.scroll.put(tab, next);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (dropdown.isOpen() && key == 256) {
            dropdown.close();
            return true;
        }
        if (getFocused() instanceof EditBox eb && eb.isFocused()) {
            if (key == 256) {
                eb.setFocus(false);
                setFocused(null);
                return true;
            }
            return super.keyPressed(key, scan, mods);
        }
        if (Keys.OPEN.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void removed() {
        ctx.store.save();
    }
}
