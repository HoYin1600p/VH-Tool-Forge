package dev.hoyin1600p.vhtoolforge.collect;

import dev.hoyin1600p.vhtoolforge.VHToolForge;
import dev.hoyin1600p.vhtoolforge.data.Store;
import dev.hoyin1600p.vhtoolforge.model.Jewel;
import dev.hoyin1600p.vhtoolforge.model.Source;
import dev.hoyin1600p.vhtoolforge.scan.JewelMatch;
import dev.hoyin1600p.vhtoolforge.scan.JewelReader;
import dev.hoyin1600p.vhtoolforge.scan.Scanner;
import dev.hoyin1600p.vhtoolforge.util.ModPaths;
import dev.hoyin1600p.vhtoolforge.util.Text;
import dev.hoyin1600p.vhtoolforge.util.Toast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Moves the planned jewels of the open storage into the chosen backpack, one at a time:
 * jewel onto the cursor (AE2/RS extract, or a slot pickup), then a right-click on the backpack,
 * which Sophisticated Backpacks turns into "put the cursor item inside".
 * Each step waits for the server's answer before the next one.
 */
public final class Collector {
    private enum Phase { PULL, WAIT_CURSOR, DEPOSIT, WAIT_EMPTY, PUT_BACK }

    private static final int TIMEOUT = 60;
    private static final int GAP = 3;

    private static Screen screen;
    private static final Deque<Jewel> queue = new ArrayDeque<>();
    private static Jewel current;
    private static Phase phase;
    private static int timer;
    private static int moved;
    private static int missing;
    private static int returned;
    private static String status = "";

    /** Whether a collection run is in progress. */
    public static boolean running() {
        return screen != null;
    }

    /** The latest collector status message. */
    public static String status() {
        return status;
    }

    /** Planned jewels still in the storage this screen shows. */
    public static int pending(Screen s) {
        Source src = Scanner.sourceFor(s);
        return src == null ? 0 : Store.get().toCollect(src.key).size();
    }

    /** Starts collecting the planned jewels of the open storage, or sets a status message saying why it cannot. */
    public static void start(Screen s) {
        Store store = Store.get();
        Source src = Scanner.sourceFor(s);
        if (src == null || !(s instanceof AbstractContainerScreen<?> acs)) {
            status = I18n.get("message.vhtoolforge.not_a_storage");
            return;
        }
        if (Backpacks.chosen(store.settings) == null) {
            status = I18n.get("message.vhtoolforge.no_backpack");
            return;
        }
        if (!acs.getMenu().getCarried().isEmpty()) {
            status = I18n.get("message.vhtoolforge.empty_cursor");
            return;
        }
        queue.clear();
        queue.addAll(store.toCollect(src.key));
        if (queue.isEmpty()) {
            status = I18n.get("message.vhtoolforge.nothing_to_collect");
            return;
        }
        screen = s;
        moved = 0;
        missing = 0;
        returned = 0;
        next();
        VHToolForge.LOG.info("Collecting {} jewels from {}", queue.size() + 1, src.key);
    }

    /** Ends the run and shows the reason as the status and as a toast. */
    public static void stop(String why) {
        screen = null;
        queue.clear();
        current = null;
        status = why;
        Toast.show(why);
    }

    private static void next() {
        current = queue.poll();
        if (current == null) {
            String missingNote = missing > 0 ? I18n.get("message.vhtoolforge.collect_done_missing", missing) : "";
            String returnedNote = returned > 0 ? I18n.get("message.vhtoolforge.collect_done_returned", returned, Text.jewels(returned)) : "";
            stop(I18n.get("message.vhtoolforge.collect_done", moved, Text.jewels(moved), missingNote, returnedNote));
            return;
        }
        enter(Phase.PULL, GAP);
        status = I18n.get("message.vhtoolforge.collecting", moved + 1);
    }

    /** Advances the run by one client tick: waits out the timer, then runs the current phase. */
    public static void tick() {
        if (screen == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != screen || mc.player == null) {
            stop(I18n.get("message.vhtoolforge.stopped_screen_closed"));
            return;
        }
        AbstractContainerScreen<?> acs = (AbstractContainerScreen<?>) screen;
        ItemStack carried = acs.getMenu().getCarried();
        if (timer > 0 && (phase == Phase.PULL || phase == Phase.DEPOSIT)) {
            timer--;
            return;
        }
        switch (phase) {
            case PULL -> pull(carried);
            case WAIT_CURSOR -> waitCursor(carried);
            case PUT_BACK -> putBack(carried);
            case DEPOSIT -> deposit(mc, acs);
            case WAIT_EMPTY -> waitEmpty(carried);
        }
    }

    private static void enter(Phase next, int ticks) {
        phase = next;
        timer = ticks;
    }

    /** The current jewel could not be had: count it as missing and go on to the next one. */
    private static void skipMissing() {
        missing++;
        next();
    }

    private static void pull(ItemStack carried) {
        if (!carried.isEmpty()) {
            stop(I18n.get("message.vhtoolforge.stopped_cursor_busy"));
            return;
        }
        if (!Scanner.pull(screen, current)) {
            skipMissing();
            return;
        }
        enter(Phase.WAIT_CURSOR, TIMEOUT);
    }

    private static void waitCursor(ItemStack carried) {
        if (carried.isEmpty()) {
            if (--timer <= 0) skipMissing();
            return;
        }
        Jewel got = plannedMatch(carried);
        if (got != null) {
            if (got != current) {
                // The storage handed over a different planned jewel: take it, and try for the other one later.
                queue.remove(got);
                queue.addLast(current);
                current = got;
            }
            enter(Phase.DEPOSIT, GAP);
        } else if (Scanner.putBack(screen)) {
            dumpMismatch(carried);
            enter(Phase.PUT_BACK, TIMEOUT);
        } else {
            dumpMismatch(carried);
            stop(I18n.get("message.vhtoolforge.stopped_unplanned"));
        }
    }

    private static void putBack(ItemStack carried) {
        if (carried.isEmpty()) {
            returned++;
            skipMissing();
        } else if (--timer <= 0) {
            stop(I18n.get("message.vhtoolforge.stopped_put_back_failed"));
        }
    }

    private static void deposit(Minecraft mc, AbstractContainerScreen<?> acs) {
        Backpacks.Found bp = Backpacks.chosen(Store.get().settings);
        int slot = bp == null ? -1 : Backpacks.menuSlot(acs.getMenu(), bp.slot());
        if (slot < 0) {
            stop(I18n.get("message.vhtoolforge.stopped_no_backpack"));
            return;
        }
        mc.gameMode.handleInventoryMouseClick(acs.getMenu().containerId, slot, 1, ClickType.PICKUP, mc.player);
        enter(Phase.WAIT_EMPTY, TIMEOUT);
    }

    private static void waitEmpty(ItemStack carried) {
        if (carried.isEmpty()) {
            Store store = Store.get();
            store.collected.add(current.id);
            store.removeJewel(current.id);
            store.save();
            moved++;
            next();
        } else if (--timer <= 0) {
            stop(I18n.get("message.vhtoolforge.stopped_backpack_full"));
        }
    }

    /** The planned jewel (current first, then the rest of this storage's queue) the cursor stack is. */
    private static Jewel plannedMatch(ItemStack carried) {
        if (!JewelReader.isJewel(carried)) return null;
        List<Jewel> candidates = new ArrayList<>(queue.size() + 1);
        candidates.add(current);
        candidates.addAll(queue);
        String fp = JewelReader.fingerprint(carried);
        return JewelMatch.pick(candidates, j -> JewelReader.matches(carried, j),
                j -> fp != null && fp.equals(JewelReader.fingerprint(j)));
    }

    /** Writes what was expected and what arrived to config/vhtoolforge/debug, to diagnose storage mismatches. */
    private static void dumpMismatch(ItemStack carried) {
        try {
            Path dir = ModPaths.debugDir();
            Files.createDirectories(dir);
            Source src = Scanner.sourceFor(screen);
            StringBuilder sb = new StringBuilder();
            sb.append("storage: ").append(src == null ? "?" : src.key).append('\n');
            sb.append("expected id: ").append(current.id).append('\n');
            sb.append("expected fingerprint: ").append(JewelReader.fingerprint(current)).append('\n');
            sb.append("expected nbt: ").append(current.snbt).append('\n');
            sb.append("received fingerprint: ").append(JewelReader.fingerprint(carried)).append('\n');
            sb.append("received nbt: ").append(carried.getTag()).append('\n');
            Files.writeString(dir.resolve("mismatch-" + System.currentTimeMillis() + ".txt"), sb.toString());
            VHToolForge.LOG.warn("Collect mismatch at {}: expected {} got {}", src == null ? "?" : src.key,
                    JewelReader.fingerprint(current), JewelReader.fingerprint(carried));
        } catch (Exception e) {
            VHToolForge.LOG.error("Could not write mismatch report", e);
        }
    }

    private Collector() {}
}
