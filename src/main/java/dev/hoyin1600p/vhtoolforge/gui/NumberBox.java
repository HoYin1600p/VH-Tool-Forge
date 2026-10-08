package dev.hoyin1600p.vhtoolforge.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;

import java.util.function.Consumer;

/** Builds the small numeric text boxes of the planner. */
final class NumberBox {
    static EditBox create(Font font, int x, int y, int w, String value, Consumer<String> onChange) {
        EditBox box = new EditBox(font, x, y, w, 14, TextComponent.EMPTY);
        box.setMaxLength(8);
        box.setValue(value);
        box.setFilter(str -> str.matches("[0-9]*\\.?[0-9]*"));
        box.setResponder(onChange);
        return box;
    }

    private NumberBox() {}
}
