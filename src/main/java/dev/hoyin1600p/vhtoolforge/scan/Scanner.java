package dev.hoyin1600p.vhtoolforge.scan;

import dev.hoyin1600p.vhtoolforge.data.Store;
import dev.hoyin1600p.vhtoolforge.model.Jewel;
import dev.hoyin1600p.vhtoolforge.model.Source;
import dev.hoyin1600p.vhtoolforge.util.Slots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Works out what storage a screen shows, reads its jewels, and pulls jewels out of it. */
public final class Scanner {
    private static final boolean AE2 = ModList.get().isLoaded("ae2");
    private static final boolean RS = ModList.get().isLoaded("refinedstorage");

    /** How long after a block right-click an opening screen is still tied to that block. */
    private static final long CLICK_BINDING_MS = 4000;

    /** Block the player last right-clicked, so a screen that opens right after can be tied to it. */
    private static BlockPos lastClicked;
    private static long lastClickedAt;

    /** Remembers the block the player just right-clicked. */
    public static void noteBlockClick(BlockPos pos) {
        lastClicked = pos.immutable();
        lastClickedAt = System.currentTimeMillis();
    }

    /** The kind of storage this screen shows, or null if it is not a scannable storage. */
    public static Source.Kind kindOf(Screen screen) {
        if (AE2 && Ae2Bridge.is(screen)) return Source.Kind.AE2;
        if (RS && RsBridge.is(screen)) return Source.Kind.RS;
        if (screen instanceof AbstractContainerScreen<?> acs && !(screen instanceof InventoryScreen)
                && !(screen instanceof CreativeModeInventoryScreen) && hasStorageSlots(acs)) {
            return Source.Kind.CONTAINER;
        }
        return null;
    }

    private static boolean hasStorageSlots(AbstractContainerScreen<?> acs) {
        for (Slot s : acs.getMenu().slots) if (!Slots.isPlayerInventory(s)) return true;
        return false;
    }

    /** Screens are tied to the block clicked just before they opened; remembered for the screen's life. */
    private static final Map<Screen, Source> BOUND = new WeakHashMap<>();

    /** The storage this screen belongs to (not yet saved). */
    public static Source sourceFor(Screen screen) {
        Source cached = BOUND.get(screen);
        if (cached != null) return cached;
        Source s = identify(screen);
        if (s != null) {
            BOUND.put(screen, s);
            lastClicked = null;
        }
        return s;
    }

    private static Source identify(Screen screen) {
        Source.Kind kind = kindOf(screen);
        if (kind == null) return null;
        Minecraft mc = Minecraft.getInstance();
        Source s = new Source();
        s.kind = kind;
        s.dimension = mc.level.dimension().location().toString();
        boolean recent = lastClicked != null && System.currentTimeMillis() - lastClickedAt < CLICK_BINDING_MS;
        if (recent) {
            s.x = lastClicked.getX();
            s.y = lastClicked.getY();
            s.z = lastClicked.getZ();
            s.label = mc.level.getBlockState(lastClicked).getBlock().getName().getString();
        } else {
            s.label = screen.getTitle().getString();
        }
        if (s.label == null || s.label.isBlank()) s.label = s.kindName();
        s.key = kind + "@" + s.dimension + (recent ? "@" + s.x + "," + s.y + "," + s.z : "@" + (kind == Source.Kind.CONTAINER ? "handheld" : "wireless"));
        return s;
    }

    /** Reads every identified jewel the screen shows and saves them as this storage's contents. */
    public static int scan(Screen screen) {
        Source src = sourceFor(screen);
        if (src == null) return -1;
        List<ItemStack> stacks = switch (src.kind) {
            case AE2 -> Ae2Bridge.jewels(screen);
            case RS -> RsBridge.jewels(screen);
            case CONTAINER -> containerJewels((AbstractContainerScreen<?>) screen);
        };
        List<Jewel> found = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (ItemStack st : stacks) {
            Jewel j = JewelReader.read(st, src.key, seen);
            if (j != null) found.add(j);
        }
        src.scannedAt = System.currentTimeMillis();
        Store.get().replaceSource(src, found);
        return found.size();
    }

    private static List<ItemStack> containerJewels(AbstractContainerScreen<?> acs) {
        List<ItemStack> out = new ArrayList<>();
        for (Slot s : acs.getMenu().slots) {
            if (Slots.isPlayerInventory(s)) continue;
            if (JewelReader.isJewel(s.getItem())) out.add(s.getItem().copy());
        }
        return out;
    }

    /**
     * Starts moving one jewel onto the cursor. Returns false if this screen no longer has it.
     * The cursor fills in when the server answers, so callers wait for it.
     */
    public static boolean pull(Screen screen, Jewel j) {
        Source.Kind kind = kindOf(screen);
        if (kind == null) return false;
        return switch (kind) {
            case AE2 -> Ae2Bridge.pull(screen, j);
            case RS -> RsBridge.pull(screen, j);
            case CONTAINER -> pullFromSlot((AbstractContainerScreen<?>) screen, j);
        };
    }

    /** Returns the cursor item to an AE2/RS network. False for plain containers (nowhere safe to put it). */
    public static boolean putBack(Screen screen) {
        Source.Kind kind = kindOf(screen);
        if (kind == Source.Kind.AE2) {
            Ae2Bridge.putBack(screen);
            return true;
        }
        if (kind == Source.Kind.RS) {
            RsBridge.putBack();
            return true;
        }
        return false;
    }

    private static boolean pullFromSlot(AbstractContainerScreen<?> acs, Jewel j) {
        Minecraft mc = Minecraft.getInstance();
        for (Slot s : acs.getMenu().slots) {
            if (Slots.isPlayerInventory(s) || !JewelReader.matches(s.getItem(), j)) continue;
            mc.gameMode.handleInventoryMouseClick(acs.getMenu().containerId, s.index, 0,
                    ClickType.PICKUP, mc.player);
            return true;
        }
        return false;
    }

    private Scanner() {}
}
