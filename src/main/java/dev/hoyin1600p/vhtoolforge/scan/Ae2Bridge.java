package dev.hoyin1600p.vhtoolforge.scan;

import appeng.api.stacks.AEItemKey;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.helpers.InventoryAction;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.common.MEStorageMenu;
import dev.hoyin1600p.vhtoolforge.model.Jewel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** AE2 terminal access. Only touched when AE2 is loaded. */
final class Ae2Bridge {
    static boolean is(Screen screen) {
        return screen instanceof MEStorageScreen<?>;
    }

    /** Every jewel in the network the terminal shows (unfiltered: search and tabs do not matter). */
    static List<ItemStack> jewels(Screen screen) {
        List<ItemStack> out = new ArrayList<>();
        IClientRepo repo = menu(screen).getClientRepo();
        if (repo == null) return out;
        for (GridInventoryEntry e : repo.getAllEntries()) {
            if (!(e.getWhat() instanceof AEItemKey key)) continue;
            ItemStack stack = key.toStack();
            if (!JewelReader.isJewel(stack)) continue;
            long n = Math.min(e.getStoredAmount(), JewelReader.MAX_COPIES_PER_ENTRY);
            for (int i = 0; i < n; i++) out.add(stack.copy());
        }
        return out;
    }

    /** Asks the network to put this jewel on the cursor. False if the terminal no longer lists it. */
    static boolean pull(Screen screen, Jewel j) {
        MEStorageMenu menu = menu(screen);
        IClientRepo repo = menu.getClientRepo();
        if (repo == null) return false;
        String want = JewelReader.fingerprint(j);
        GridInventoryEntry pick = JewelMatch.pick(repo.getAllEntries(),
                e -> e.getStoredAmount() > 0 && e.getWhat() instanceof AEItemKey key && JewelReader.matchesTag(key.getTag(), j),
                e -> e.getStoredAmount() > 0 && e.getWhat() instanceof AEItemKey key && want.equals(JewelReader.fingerprint(key.toStack())));
        if (pick == null) return false;
        menu.handleInteraction(pick.getSerial(), InventoryAction.PICKUP_OR_SET_DOWN);
        return true;
    }

    /** Puts the cursor stack back into the network (a click on empty grid space). */
    static void putBack(Screen screen) {
        menu(screen).handleInteraction(-1, InventoryAction.PICKUP_OR_SET_DOWN);
    }

    private static MEStorageMenu menu(Screen screen) {
        return ((MEStorageScreen<?>) screen).getMenu();
    }

    private Ae2Bridge() {}
}
