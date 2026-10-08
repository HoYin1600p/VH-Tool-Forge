package dev.hoyin1600p.vhtoolforge.client;

import dev.hoyin1600p.vhtoolforge.collect.Backpacks;
import dev.hoyin1600p.vhtoolforge.collect.Collector;
import dev.hoyin1600p.vhtoolforge.data.Store;
import dev.hoyin1600p.vhtoolforge.plan.Planner;
import dev.hoyin1600p.vhtoolforge.scan.Scanner;
import dev.hoyin1600p.vhtoolforge.util.Text;
import dev.hoyin1600p.vhtoolforge.util.Toast;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ScreenEvent;

/**
 * The Scan and Collect buttons, stacked beside a storage screen (right side, or left when there is no room),
 * and the status line drawn under them.
 */
public final class StorageButtons {
    private static final int WIDTH = 90;
    private static final int HEIGHT = 20;
    private static final int GAP = 2;
    private static final int MARGIN = 4;
    /** Widgets and slots this far past the panel's own edge still count as part of it (side tabs, cell columns). */
    private static final int ATTACHED_REACH = 80;

    /** Where the buttons went on each open screen, for the status line and for JEI's exclusion areas. */
    private static final Map<Screen, Rect2i> PLACED = new WeakHashMap<>();

    /** Adds the Scan and Collect buttons beside a storage screen, if the player has them switched on. */
    static void add(ScreenEvent.InitScreenEvent.Post e, AbstractContainerScreen<?> acs) {
        Screen screen = e.getScreen();
        Rect2i area = place(acs, e.getListenersList());
        PLACED.put(screen, area);
        e.addListener(scanButton(screen, area.getX(), area.getY()));
        e.addListener(collectButton(screen, area.getX(), area.getY() + HEIGHT + GAP));
    }

    /** The column the two buttons occupy: right of the panel and anything attached to it, else left, else above. */
    private static Rect2i place(AbstractContainerScreen<?> acs, List<GuiEventListener> listeners) {
        int left = acs.getGuiLeft();
        int right = left + acs.getXSize();
        int panelLeft = left, panelRight = right;
        for (Slot slot : acs.getMenu().slots) {
            int sx = left + slot.x;
            if (sx + 18 > panelRight && sx < right + ATTACHED_REACH) panelRight = sx + 18;
            if (sx - 1 < panelLeft && sx > left - ATTACHED_REACH) panelLeft = sx - 1;
        }
        for (GuiEventListener l : listeners) {
            if (!(l instanceof AbstractWidget w)) continue;
            int wr = w.x + w.getWidth();
            if (wr > panelRight && w.x < right + ATTACHED_REACH) panelRight = wr;
            if (w.x < panelLeft && wr > left - ATTACHED_REACH) panelLeft = w.x;
        }
        int columnHeight = HEIGHT * 2 + GAP;
        // Sophisticated screens grow upgrade tabs out of their right edge when clicked: sit above them instead.
        int above = acs.getGuiTop() - columnHeight - GAP;
        if (isSophisticated(acs) && above >= 2 && right + MARGIN + WIDTH <= acs.width - 2) {
            return new Rect2i(right + MARGIN, above, WIDTH, columnHeight);
        }
        int y = Math.max(2, acs.getGuiTop());
        if (panelRight + MARGIN + WIDTH <= acs.width - 2) return new Rect2i(panelRight + MARGIN, y, WIDTH, columnHeight);
        if (panelLeft - MARGIN - WIDTH >= 2) return new Rect2i(panelLeft - MARGIN - WIDTH, y, WIDTH, columnHeight);
        return new Rect2i(left, Math.max(2, acs.getGuiTop() - columnHeight - GAP), WIDTH, columnHeight);
    }

    /** Sophisticated Backpacks / Storage screens (all built on Sophisticated Core's storage screen). */
    private static boolean isSophisticated(Screen screen) {
        for (Class<?> c = screen.getClass(); c != null; c = c.getSuperclass()) {
            if (c.getName().startsWith("net.p3pp3rf1y.sophisticatedcore.")) return true;
        }
        return false;
    }

    /** Screen areas the buttons cover on the given screen, so item-list overlays can keep clear of them. */
    public static List<Rect2i> areas(Screen screen) {
        Rect2i r = PLACED.get(screen);
        if (r == null || !Store.get().settings.showStorageButtons) return List.of();
        return List.of(r);
    }

    private static Button scanButton(Screen screen, int x, int y) {
        return new Button(x, y, WIDTH, HEIGHT, new TextComponent(I18n.get("gui.vhtoolforge.storage.scan")), b -> {
            int n = Scanner.scan(screen);
            Toast.show(n < 0 ? I18n.get("message.vhtoolforge.nothing_to_scan")
                    : I18n.get("message.vhtoolforge.scanned", n, Text.jewels(n)));
        }, (b, pose, mx, my) -> screen.renderTooltip(pose, new TranslatableComponent("tooltip.vhtoolforge.scan"), mx, my));
    }

    private static Button collectButton(Screen screen, int x, int y) {
        return new Button(x, y, WIDTH, HEIGHT, new TextComponent(I18n.get("gui.vhtoolforge.storage.collect")), b -> {
            Store store = Store.get();
            if (store.plan == null) {
                Planner.lockCurrentPlan();
            }
            Collector.start(screen);
            Toast.show(Collector.status());
        }, (b, pose, mx, my) -> screen.renderTooltip(pose, new TextComponent(collectHint()), mx, my)) {
            @Override
            public Component getMessage() {
                int n = Collector.pending(screen);
                return new TextComponent(Collector.running() ? I18n.get("gui.vhtoolforge.storage.collecting")
                        : I18n.get("gui.vhtoolforge.storage.collect_count", n));
            }
        };
    }

    private static String collectHint() {
        Store store = Store.get();
        Backpacks.Found bp = Backpacks.chosen(store.settings);
        if (bp == null) return I18n.get("tooltip.vhtoolforge.collect_no_backpack");
        return I18n.get("tooltip.vhtoolforge.collect", bp.stack().getHoverName().getString(), Backpacks.slotName(bp.slot()));
    }

    /** Draws the collector status, or the latest toast, under the buttons, wrapped to the space beside the panel. */
    static void drawStatus(ScreenEvent.DrawScreenEvent.Post e, AbstractContainerScreen<?> acs) {
        String msg = Collector.running() ? Collector.status() : Toast.message();
        if (msg.isEmpty()) return;
        Rect2i area = PLACED.get(e.getScreen());
        if (area == null) return;
        Minecraft mc = Minecraft.getInstance();
        int x = area.getX();
        int y = area.getY() + area.getHeight() + MARGIN;
        int maxWidth = Math.max(WIDTH, acs.width - x - 2);
        for (FormattedCharSequence line : mc.font.split(new TextComponent(msg), maxWidth)) {
            mc.font.drawShadow(e.getPoseStack(), line, x, y, ChatFormatting.YELLOW.getColor());
            y += mc.font.lineHeight + 1;
        }
    }

    private StorageButtons() {}
}
