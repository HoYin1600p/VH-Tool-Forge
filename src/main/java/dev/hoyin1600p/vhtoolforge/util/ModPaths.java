package dev.hoyin1600p.vhtoolforge.util;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

/** Where the mod reads and writes files under the game's config folder. */
public final class ModPaths {
    /** Per-world saved data ({@code config/vhtoolforge/data}). */
    public static Path dataDir() {
        return FMLPaths.CONFIGDIR.get().resolve("vhtoolforge/data");
    }

    /** Diagnostic reports ({@code config/vhtoolforge/debug}). */
    public static Path debugDir() {
        return FMLPaths.CONFIGDIR.get().resolve("vhtoolforge/debug");
    }

    /** Vault Hunters' own config files, read for the active pack ({@code config/the_vault}). */
    public static Path vaultConfigDir() {
        return FMLPaths.CONFIGDIR.get().resolve("the_vault");
    }

    private ModPaths() {}
}
