package dev.hoyin1600p.vhtoolforge.gui;

import dev.hoyin1600p.vhtoolforge.data.Store;
import dev.hoyin1600p.vhtoolforge.model.Settings;
import dev.hoyin1600p.vhtoolforge.pack.PackConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** What the planner screen and its panels share: the data, the solver, the layout and the per-frame hit areas. */
final class UiContext {
    final Store store;
    final Settings s;
    final PackConfig pack;
    final SolveService solve;
    final StackCache stacks = new StackCache();

    /** Clickable areas drawn by hand, rebuilt every frame. */
    final List<HitArea> hits = new ArrayList<>();
    final Map<Tab, Integer> scroll = new LinkedHashMap<>();

    // Set by the screen when it is (re)initialised.
    Minecraft minecraft;
    Font font;
    ItemRenderer items;
    int width, height;
    /** Re-lays out the screen from scratch. */
    Runnable relayout = () -> {};

    // Layout
    int contentTop, contentBottom, leftX, leftW, rightX, rightW;
    int contentHeight;

    UiContext(Store store, PackConfig pack, SolveService solve) {
        this.store = store;
        this.s = store.settings;
        this.pack = pack;
        this.solve = solve;
    }

    /** Something the plan depends on changed: save it and solve again. */
    void changed() {
        solve.markDirty();
        store.save();
    }

    int scrollOf(Tab tab) {
        return scroll.getOrDefault(tab, 0);
    }

    /** Adds a clickable and/or hoverable area with a text tooltip (or none). */
    void hit(int x, int y, int w, int h, Runnable action, Supplier<List<Component>> tooltip) {
        hits.add(new HitArea(x, y, w, h, action, tooltip, null));
    }
}
