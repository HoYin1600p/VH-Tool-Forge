package dev.hoyin1600p.vhtoolforge.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vhtoolforge.model.Settings;
import dev.hoyin1600p.vhtoolforge.plan.Planner;
import dev.hoyin1600p.vhtoolforge.solve.Solver;
import dev.hoyin1600p.vhtoolforge.stat.BaseType;
import iskallia.vault.item.tool.ToolMaterial;
import iskallia.vault.item.tool.ToolType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

import static dev.hoyin1600p.vhtoolforge.gui.Format.materialName;
import static dev.hoyin1600p.vhtoolforge.gui.Format.parseInt;
import static dev.hoyin1600p.vhtoolforge.gui.Format.title;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.checkbox;
import static dev.hoyin1600p.vhtoolforge.gui.GuiDraw.slot;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.BAD;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.BLACK;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.CAPACITY_FILL;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.EDGE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.GOLD;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.TITLE;
import static dev.hoyin1600p.vhtoolforge.gui.Palette.WHITE;
import static net.minecraft.client.gui.GuiComponent.fill;

/** The top of the planner: material and base type pickers, the limits row and the capacity bar. */
final class ToolPickerPanel {
    private final UiContext ctx;
    private final EditBox maxLevelBox, extraCapBox;

    ToolPickerPanel(UiContext ctx) {
        this.ctx = ctx;
        Settings s = ctx.s;
        maxLevelBox = NumberBox.create(ctx.font, 110, 50, 34, s.maxLevel == null ? "" : String.valueOf(s.maxLevel), v -> {
            s.maxLevel = parseInt(v);
            ctx.changed();
        });
        extraCapBox = NumberBox.create(ctx.font, 0, 50, 34, String.valueOf(s.extraCapacityAmount), v -> {
            Integer n = parseInt(v);
            s.extraCapacityAmount = n == null ? 0 : n;
            ctx.changed();
        });
        extraCapBox.x = 160 + ctx.font.width(I18n.get("gui.vhtoolforge.picker.extra_capacity")) + 26;
    }

    EditBox maxLevelBox() {
        return maxLevelBox;
    }

    EditBox extraCapBox() {
        return extraCapBox;
    }

    void tick() {
        maxLevelBox.tick();
        extraCapBox.tick();
    }

    void unfocus() {
        maxLevelBox.setFocus(false);
        extraCapBox.setFocus(false);
    }

    void renderBoxes(PoseStack pose, int mx, int my, float pt) {
        maxLevelBox.render(pose, mx, my, pt);
        extraCapBox.render(pose, mx, my, pt);
    }

    void render(PoseStack pose) {
        int x = drawMaterials(pose);
        drawBaseTypes(pose, x);
        drawLimits(pose);
        drawCapacityBar(pose);
    }

    /** Draws the material row and returns the x where the base type label starts. */
    private int drawMaterials(PoseStack pose) {
        Settings s = ctx.s;
        ctx.font.draw(pose, I18n.get("gui.vhtoolforge.picker.material"), 16, 30, TITLE);
        int x = 70;
        for (ToolMaterial m : ctx.pack.materials) {
            boolean sel = m.name().equals(s.material);
            slot(pose, x, 24, 20, sel);
            ctx.items.renderAndDecorateFakeItem(materialIcon(m), x + 2, 26);
            String cap = String.valueOf(m.getCapacity());
            pose.pushPose();
            pose.translate(0, 0, 300);
            pose.scale(0.5f, 0.5f, 1);
            ctx.font.drawShadow(pose, cap, (x + 19) * 2 - ctx.font.width(cap), (24 + 14) * 2, WHITE);
            pose.popPose();
            ctx.hit(x, 24, 20, 20, () -> {
                s.material = m.name();
                ctx.changed();
                ctx.relayout.run();
            }, () -> List.of(new TranslatableComponent("tooltip.vhtoolforge.material_name", materialName(m)),
                    new TranslatableComponent("tooltip.vhtoolforge.material_capacity", m.getCapacity()).withStyle(ChatFormatting.GOLD),
                    new TranslatableComponent("tooltip.vhtoolforge.material_level", m.getLevel()).withStyle(ChatFormatting.GRAY)));
            x += 22;
        }
        return x + 14;
    }

    private void drawBaseTypes(PoseStack pose, int startX) {
        Settings s = ctx.s;
        int x = startX;
        String label = I18n.get("gui.vhtoolforge.picker.base_type");
        ctx.font.draw(pose, label, x, 30, TITLE);
        x += ctx.font.width(label) + 6;
        for (String bt : ctx.pack.baseTypes) {
            boolean sel = bt.equals(s.baseType);
            slot(pose, x, 24, 20, sel);
            ToolType type = toolType(bt);
            ctx.items.renderAndDecorateFakeItem(ctx.stacks.tool(Planner.material(s), type), x + 2, 26);
            String tipKey = BaseType.fromId(bt).isHammer() ? "tooltip.vhtoolforge.base_type_hammer" : "tooltip.vhtoolforge.base_type_other";
            ctx.hit(x, 24, 20, 20, () -> {
                s.baseType = bt;
                ctx.changed();
                ctx.relayout.run();
            }, () -> List.of(new TranslatableComponent("tooltip.vhtoolforge.base_type_name", materialName(Planner.material(s)), title(bt)),
                    new TranslatableComponent(tipKey).withStyle(ChatFormatting.GRAY)));
            x += 22;
        }
    }

    private void drawLimits(PoseStack pose) {
        Settings s = ctx.s;
        ctx.font.draw(pose, I18n.get("gui.vhtoolforge.picker.max_level"), 16, 53, TITLE);
        maxLevelBox.setSuggestion(maxLevelBox.getValue().isEmpty() ? I18n.get("gui.vhtoolforge.picker.none") : null);
        int cbX = 160;
        String capLabel = I18n.get("gui.vhtoolforge.picker.extra_capacity");
        checkbox(pose, cbX, 50, s.extraCapacity);
        ctx.font.draw(pose, capLabel, cbX + 16, 53, TITLE);
        ctx.hit(cbX, 50, 16 + ctx.font.width(capLabel), 14, () -> {
            s.extraCapacity = !s.extraCapacity;
            ctx.changed();
        }, () -> List.of(new TranslatableComponent("tooltip.vhtoolforge.extra_capacity"),
                new TranslatableComponent("tooltip.vhtoolforge.extra_capacity_note").withStyle(ChatFormatting.GRAY)));
        ctx.font.draw(pose, "+", extraCapBox.x - 9, 53, TITLE);
    }

    private void drawCapacityBar(PoseStack pose) {
        Solver.Result result = ctx.solve.result();
        int barX = extraCapBox.x + extraCapBox.getWidth() + 14;
        int barW = Math.max(60, ctx.width - 16 - barX - 120);
        int cap = Planner.capacity(ctx.s);
        int used = result == null ? 0 : result.used;
        String label = I18n.get("gui.vhtoolforge.picker.capacity");
        ctx.font.draw(pose, label, barX, 53, TITLE);
        int tx = barX + ctx.font.width(label) + 6;
        fill(pose, tx, 50, tx + barW, 62, EDGE);
        fill(pose, tx + 1, 51, tx + barW - 1, 61, BLACK);
        int fw = (int) ((barW - 2) * Math.min(1.0, (double) used / Math.max(1, cap)));
        fill(pose, tx + 1, 51, tx + 1 + fw, 61, result != null && result.over ? BAD : CAPACITY_FILL);
        ctx.font.draw(pose, used + " / " + cap, tx + barW + 6, 53, TITLE);
    }

    private static ToolType toolType(String id) {
        try {
            return ToolType.valueOf(id);
        } catch (IllegalArgumentException | NullPointerException e) {
            return ToolType.PICK;
        }
    }

    private static ItemStack materialIcon(ToolMaterial m) {
        var item = Registry.ITEM.get(new ResourceLocation("the_vault", m.getId()));
        return new ItemStack(item == Items.AIR ? Items.IRON_INGOT : item);
    }
}
