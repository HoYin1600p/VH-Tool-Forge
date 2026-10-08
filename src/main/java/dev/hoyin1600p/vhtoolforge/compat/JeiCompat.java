package dev.hoyin1600p.vhtoolforge.compat;

import dev.hoyin1600p.vhtoolforge.VHToolForge;
import dev.hoyin1600p.vhtoolforge.client.StorageButtons;
import java.util.Collection;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

/** Tells JEI where the Scan and Collect buttons are, so its item list does not cover them. Loaded only by JEI. */
@JeiPlugin
public final class JeiCompat implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(VHToolForge.MOD_ID, "storage_buttons");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Collection<Rect2i> getGuiExtraAreas() {
                return StorageButtons.areas(Minecraft.getInstance().screen);
            }
        });
    }
}
