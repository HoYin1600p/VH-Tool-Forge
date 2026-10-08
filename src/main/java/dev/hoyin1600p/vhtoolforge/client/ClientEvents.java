package dev.hoyin1600p.vhtoolforge.client;

import dev.hoyin1600p.vhtoolforge.collect.Collector;
import dev.hoyin1600p.vhtoolforge.data.Store;
import dev.hoyin1600p.vhtoolforge.gui.ForgeScreen;
import dev.hoyin1600p.vhtoolforge.input.Keys;
import dev.hoyin1600p.vhtoolforge.scan.Scanner;
import dev.hoyin1600p.vhtoolforge.util.Toast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Client-side wiring: the hotkey that opens the planner, the per-tick collector update, and a Scan / Collect
 * button column (plus a short status message) added beside storage screens. The target backpack is chosen in the planner.
 */
public final class ClientEvents {
    /** Registers the setup listener and the Forge event handlers of this class. */
    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientEvents::setup);
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
    }

    private static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> ClientRegistry.registerKeyBinding(Keys.OPEN));
    }

    /** Opens the planner on its hotkey and advances the collector once per client tick. */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (Keys.OPEN.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new ForgeScreen());
        }
        Collector.tick();
    }

    /** Remembers the block the player clicked, so the storage screen that opens next can be tied to it. */
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getWorld().isClientSide()) Scanner.noteBlockClick(e.getPos());
    }

    /** Adds the Scan and Collect buttons to a storage screen, if the player has them switched on. */
    @SubscribeEvent
    public void onScreenInit(ScreenEvent.InitScreenEvent.Post e) {
        Screen screen = e.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> acs) || screen instanceof ForgeScreen) return;
        if (Minecraft.getInstance().player == null || Scanner.kindOf(screen) == null) return;
        Scanner.sourceFor(screen); // bind the screen to the block just clicked
        if (!Store.get().settings.showStorageButtons) return;
        StorageButtons.add(e, acs);
    }

    /** Draws the collector status or the latest toast beside a storage screen. */
    @SubscribeEvent
    public void onScreenDraw(ScreenEvent.DrawScreenEvent.Post e) {
        if (Toast.expired() && !Collector.running()) return;
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> acs) || e.getScreen() instanceof ForgeScreen) return;
        if (!Store.get().settings.showStorageButtons && !Collector.running()) return;
        StorageButtons.drawStatus(e, acs);
    }
}
