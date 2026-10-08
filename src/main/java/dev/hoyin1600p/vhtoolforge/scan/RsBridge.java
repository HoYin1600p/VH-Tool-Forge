package dev.hoyin1600p.vhtoolforge.scan;

import com.refinedmods.refinedstorage.RS;
import com.refinedmods.refinedstorage.network.grid.GridItemInsertHeldMessage;
import com.refinedmods.refinedstorage.network.grid.GridItemPullMessage;
import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.refinedmods.refinedstorage.screen.grid.stack.IGridStack;
import com.refinedmods.refinedstorage.screen.grid.stack.ItemGridStack;
import dev.hoyin1600p.vhtoolforge.model.Jewel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Refined Storage grid access. Only touched when Refined Storage is loaded. */
final class RsBridge {
    static boolean is(Screen screen) {
        return screen instanceof GridScreen;
    }

    static List<ItemStack> jewels(Screen screen) {
        List<ItemStack> out = new ArrayList<>();
        for (IGridStack gs : ((GridScreen) screen).getView().getAllStacks()) {
            if (!(gs instanceof ItemGridStack igs)) continue;
            ItemStack stack = igs.getStack();
            if (!JewelReader.isJewel(stack)) continue;
            int n = Math.min(Math.max(1, stack.getCount()), JewelReader.MAX_COPIES_PER_ENTRY);
            for (int i = 0; i < n; i++) {
                ItemStack one = stack.copy();
                one.setCount(1);
                out.add(one);
            }
        }
        return out;
    }

    /** Asks the grid to put this jewel on the cursor (no extract flags = cursor). */
    static boolean pull(Screen screen, Jewel j) {
        String want = JewelReader.fingerprint(j);
        IGridStack pick = JewelMatch.pick(((GridScreen) screen).getView().getAllStacks(),
                gs -> gs instanceof ItemGridStack igs && JewelReader.isJewel(igs.getStack()) && JewelReader.matches(igs.getStack(), j),
                gs -> gs instanceof ItemGridStack igs && JewelReader.isJewel(igs.getStack()) && want.equals(JewelReader.fingerprint(igs.getStack())));
        if (pick == null) return false;
        RS.NETWORK_HANDLER.sendToServer(new GridItemPullMessage(pick.getId(), 0));
        return true;
    }

    /** Puts the cursor stack back into the network. */
    static void putBack() {
        RS.NETWORK_HANDLER.sendToServer(new GridItemInsertHeldMessage(false));
    }

    private RsBridge() {}
}
