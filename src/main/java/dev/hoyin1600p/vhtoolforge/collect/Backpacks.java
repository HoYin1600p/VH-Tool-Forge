package dev.hoyin1600p.vhtoolforge.collect;

import dev.hoyin1600p.vhtoolforge.model.Settings;
import dev.hoyin1600p.vhtoolforge.util.Slots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Finds Sophisticated Backpacks in the player's main inventory and hotbar. */
public final class Backpacks {
    /** Player inventory slots searched for backpacks: hotbar plus main inventory. */
    private static final int INVENTORY_SLOTS = 36;

    /** A backpack found in the player inventory: its slot, its stack and its contents UUID (null if it has none). */
    public record Found(int slot, ItemStack stack, String uuid) {}

    /** Whether the stack is a Sophisticated Backpacks backpack. */
    public static boolean isBackpack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = stack.getItem().getRegistryName();
        return id != null && "sophisticatedbackpacks".equals(id.getNamespace()) && id.getPath().contains("backpack");
    }

    /** The backpack's contents UUID, or null if it has none. */
    public static String uuidOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID("contentsUuid")) return tag.getUUID("contentsUuid").toString();
        return null;
    }

    /** Backpacks in inventory slots 0-35 (hotbar + main), single-stacked only. */
    public static List<Found> all() {
        List<Found> out = new ArrayList<>();
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < INVENTORY_SLOTS; i++) {
            ItemStack st = inv.getItem(i);
            if (isBackpack(st) && st.getCount() == 1) out.add(new Found(i, st, uuidOf(st)));
        }
        return out;
    }

    /** The backpack the player chose: same contents UUID if it has one, else the same slot. */
    public static Found chosen(Settings s) {
        List<Found> all = all();
        if (s.backpackUuid != null) {
            for (Found f : all) if (s.backpackUuid.equals(f.uuid())) return f;
        }
        for (Found f : all) if (f.slot() == s.backpackSlot && (s.backpackUuid == null || f.uuid() == null)) return f;
        return null;
    }

    /** Saves this backpack as the target backpack in the settings. */
    public static void choose(Settings s, Found f) {
        s.backpackUuid = f.uuid();
        s.backpackSlot = f.slot();
        s.backpackName = f.stack().getHoverName().getString();
    }

    /** The open menu's slot index for a player inventory slot, or -1. */
    public static int menuSlot(AbstractContainerMenu menu, int inventorySlot) {
        for (Slot sl : menu.slots) {
            if (Slots.isPlayerInventory(sl) && sl.getContainerSlot() == inventorySlot) return sl.index;
        }
        return -1;
    }

    /** A readable name for a player inventory slot, such as its hotbar number or inventory row and column. */
    public static String slotName(int slot) {
        return slot < 9 ? I18n.get("gui.vhtoolforge.backpack.slot_hotbar", slot + 1)
                : I18n.get("gui.vhtoolforge.backpack.slot_inventory", (slot - 9) / 9 + 1, (slot - 9) % 9 + 1);
    }

    private Backpacks() {}
}
