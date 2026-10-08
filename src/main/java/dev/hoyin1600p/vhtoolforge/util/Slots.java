package dev.hoyin1600p.vhtoolforge.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;

/** Menu slot helpers. */
public final class Slots {
    /** True if the slot belongs to the player's own inventory (hotbar and main), not the opened storage. */
    public static boolean isPlayerInventory(Slot slot) {
        return slot.container == Minecraft.getInstance().player.getInventory();
    }

    private Slots() {}
}
