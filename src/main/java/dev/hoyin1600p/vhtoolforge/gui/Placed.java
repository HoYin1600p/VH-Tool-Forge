package dev.hoyin1600p.vhtoolforge.gui;

import net.minecraft.client.gui.components.AbstractWidget;

/** A widget that lives inside a scrolling list, with its unscrolled y. */
record Placed(AbstractWidget w, int baseY) {}
