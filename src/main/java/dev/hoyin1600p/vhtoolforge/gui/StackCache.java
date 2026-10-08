package dev.hoyin1600p.vhtoolforge.gui;

import dev.hoyin1600p.vhtoolforge.model.Jewel;
import iskallia.vault.item.tool.ToolMaterial;
import iskallia.vault.item.tool.ToolType;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Item stacks built for the screen once and reused every frame. */
final class StackCache {
    private final Map<String, ItemStack> jewelStacks = new HashMap<>();
    private final Map<String, ItemStack> toolStacks = new HashMap<>();

    ItemStack jewel(Jewel j) {
        String key = j.id != null ? j.id + "|" + j.size : "u|" + j.uniqueKey + "|" + j.size;
        return jewelStacks.computeIfAbsent(key, k -> ToolStacks.jewelStack(j));
    }

    ItemStack tool(ToolMaterial m, ToolType t) {
        return toolStacks.computeIfAbsent(m.name() + "|" + t.name(), k -> ToolStacks.plainTool(m, t));
    }
}
