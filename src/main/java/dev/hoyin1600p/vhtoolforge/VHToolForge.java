package dev.hoyin1600p.vhtoolforge;

import dev.hoyin1600p.vhtoolforge.client.ClientEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The mod entry point. VH Tool Forge is client-only: it adds nothing on servers and works on any server. */
@Mod(VHToolForge.MOD_ID)
public final class VHToolForge {
    /** The mod id, also the namespace of this mod's assets and lang keys. */
    public static final String MOD_ID = "vhtoolforge";
    public static final Logger LOG = LoggerFactory.getLogger("VH Tool Forge");

    /** Declares the mod as client-only and, on the physical client, registers its event handlers. */
    public VHToolForge() {
        // Client-only: servers do not need it, and it does not need the server.
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remote, isServer) -> true));
        if (FMLEnvironment.dist == Dist.CLIENT) ClientEvents.init();
    }
}
