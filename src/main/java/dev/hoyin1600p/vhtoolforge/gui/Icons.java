package dev.hoyin1600p.vhtoolforge.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vhtoolforge.stat.StatIds;
import iskallia.vault.item.tool.ToolMaterial;
import iskallia.vault.item.tool.ToolType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/** 16x16 icons for stats: VH's own textures where it has them, otherwise a fitting item. */
public final class Icons {
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static final Map<String, ItemStack> ITEMS = new HashMap<>();

    static {
        tex(StatIds.MINING_SPEED, "faceted_focus/mining_speed");
        tex(StatIds.ITEM_QUANTITY, "faceted_focus/item_quantity");
        tex(StatIds.ITEM_RARITY, "faceted_focus/item_rarity");
        tex(StatIds.TRAP_DISARMING, "faceted_focus/trap_disarm");
        tex(StatIds.COPIOUSLY, "faceted_focus/copiously");
        tex(StatIds.DURABILITY, "faceted_focus/durability");
        tex(StatIds.REACH, "faceted_focus/reach");
        tex(StatIds.WOODEN_AFFINITY, "skills/hunter_wooden");
        tex(StatIds.ORNATE_AFFINITY, "skills/hunter_ornate");
        tex(StatIds.GILDED_AFFINITY, "skills/hunter_gilded");
        tex(StatIds.LIVING_AFFINITY, "skills/hunter_living");
        tex(StatIds.COIN_AFFINITY, "skills/hunter_coins");
    }

    private static void tex(String stat, String path) {
        TEXTURES.put(stat, new ResourceLocation("the_vault", "textures/item/" + path + ".png"));
    }

    private static ItemStack itemFor(String stat) {
        return ITEMS.computeIfAbsent(stat, s -> switch (s) {
            case StatIds.IMMORTALITY -> new ItemStack(Items.TOTEM_OF_UNDYING);
            case StatIds.SMELTING -> new ItemStack(Items.FURNACE);
            case StatIds.PULVERIZING -> new ItemStack(Items.FLINT);
            case StatIds.HYDROVOID -> new ItemStack(Items.SPONGE);
            case StatIds.SOULBOUND -> vhItem("soul_dust", Items.SOUL_LANTERN);
            case StatIds.PICKING -> ToolStacks.plainTool(ToolMaterial.CHROMATIC_IRON_INGOT, ToolType.PICK);
            case StatIds.AXING -> ToolStacks.plainTool(ToolMaterial.CHROMATIC_IRON_INGOT, ToolType.AXE);
            case StatIds.SHOVELLING -> ToolStacks.plainTool(ToolMaterial.CHROMATIC_IRON_INGOT, ToolType.SHOVEL);
            case StatIds.REAPING -> ToolStacks.plainTool(ToolMaterial.CHROMATIC_IRON_INGOT, ToolType.SICKLE);
            case StatIds.HAMMERING, StatIds.HAMMER_SIZE -> ToolStacks.plainTool(ToolMaterial.CHROMATIC_IRON_INGOT, ToolType.HAMMER);
            default -> new ItemStack(Items.PAPER);
        });
    }

    static ItemStack vhItem(String path, Item fallback) {
        var item = Registry.ITEM.get(new ResourceLocation("the_vault", path));
        return new ItemStack(item == Items.AIR ? fallback : item);
    }

    /** Draws the 16x16 icon of a stat at the given position. */
    public static void draw(PoseStack pose, String stat, int x, int y) {
        ResourceLocation tex = TEXTURES.get(stat);
        if (tex != null) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShaderTexture(0, tex);
            RenderSystem.enableBlend();
            GuiComponent.blit(pose, x, y, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
        } else {
            Minecraft.getInstance().getItemRenderer().renderAndDecorateFakeItem(itemFor(stat), x, y);
        }
    }

    private Icons() {}
}
