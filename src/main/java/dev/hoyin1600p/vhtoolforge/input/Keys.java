package dev.hoyin1600p.vhtoolforge.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Key bindings, shared by the event wiring (which registers them) and the planner screen. */
public final class Keys {
    /** The key that opens the planner screen. */
    public static final KeyMapping OPEN = new KeyMapping("key.vhtoolforge.open", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA, "key.categories.vhtoolforge");

    private Keys() {}
}
