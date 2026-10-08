package dev.hoyin1600p.vhtoolforge.gui;

import dev.hoyin1600p.vhtoolforge.model.Jewel;
import dev.hoyin1600p.vhtoolforge.model.Settings;
import dev.hoyin1600p.vhtoolforge.plan.Planner;
import dev.hoyin1600p.vhtoolforge.scan.JewelReader;
import dev.hoyin1600p.vhtoolforge.stat.StatIds;
import dev.hoyin1600p.vhtoolforge.stat.Stats;
import iskallia.vault.gear.VaultGearState;
import iskallia.vault.gear.attribute.VaultGearAttribute;
import iskallia.vault.gear.attribute.VaultGearModifier;
import iskallia.vault.gear.data.VaultGearData;
import iskallia.vault.init.ModGearAttributes;
import iskallia.vault.init.ModItems;
import iskallia.vault.item.tool.ToolItem;
import iskallia.vault.item.tool.ToolMaterial;
import iskallia.vault.item.tool.ToolType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds real VH tool and jewel ItemStacks in memory, so the screen can show VH's own icons and tooltips.
 * The "as built" tool goes through VH's own {@link ToolItem#applyJewel}, exactly as the Jewel Application Station would.
 */
public final class ToolStacks {
    private static final Set<String> TOOL_FLAGS = Set.of(StatIds.PICKING, StatIds.AXING, StatIds.SHOVELLING, StatIds.HAMMERING, StatIds.REAPING);

    /** The VH tool type whose flags are exactly these (hammering only with a hammer base). */
    public static ToolType typeFor(Set<String> flags, boolean hammer) {
        Set<String> want = new HashSet<>();
        for (String f : flags) if (TOOL_FLAGS.contains(f)) want.add(f);
        if (hammer) want.add(StatIds.HAMMERING);
        else want.remove(StatIds.HAMMERING);
        for (ToolType t : ToolType.values()) {
            Set<String> have = new HashSet<>();
            for (VaultGearAttribute<Boolean> a : t.getAttributes()) {
                ResourceLocation id = a.getRegistryName();
                if (id != null) have.add(id.getPath());
            }
            if (have.equals(want)) return t;
        }
        return ToolType.PICK;
    }

    /** The tool type of the chosen base type; a pick if it is unknown. */
    public static ToolType baseType(Settings s) {
        try {
            return ToolType.valueOf(s.baseType);
        } catch (IllegalArgumentException | NullPointerException e) {
            return ToolType.PICK;
        }
    }

    /** A bare tool of this material and type, with no jewels applied. */
    public static ItemStack plainTool(ToolMaterial m, ToolType t) {
        return ToolItem.create(m, t);
    }

    /** The tool as the player described it: targets as totals, ticked yes/no stats on it. */
    public static ItemStack entered(Settings s) {
        Set<String> flags = new HashSet<>(Planner.requiredFlags(s));
        flags.add(Planner.baseFlag(s));
        ToolType type = typeFor(flags, Planner.hammer(s));
        ToolMaterial mat = Planner.material(s);
        return ToolItem.create(mat, type, data -> {
            data.createOrReplaceAttributeValue(ModGearAttributes.TOOL_CAPACITY, Planner.capacity(s));
            for (Stats.Num n : Stats.NUMS) {
                if (n.hammerOnly() && !Planner.hammer(s)) continue;
                Settings.Target t = s.targets.get(n.id());
                if (t == null || t.value == null || t.value <= 0) continue;
                double add = t.value - Planner.base(s, n);
                if (add <= 0) continue;
                VaultGearModifier<?> mod = numModifier(n.id(), add);
                if (mod != null) data.addModifier(VaultGearModifier.AffixType.SUFFIX, mod);
            }
            for (String f : flags) {
                if (TOOL_FLAGS.contains(f)) continue;
                VaultGearModifier<?> mod = flagModifier(f);
                if (mod != null) data.addModifier(VaultGearModifier.AffixType.SUFFIX, mod);
            }
            for (Stats.Flag f : Stats.FLAGS) {
                if (f.niceToHave() && s.flags.contains(f.id())) {
                    VaultGearModifier<?> mod = flagModifier(f.id());
                    if (mod != null) data.addModifier(VaultGearModifier.AffixType.SUFFIX, mod);
                }
            }
        });
    }

    /** The tool after applying the planned jewels with VH's own jewel application. */
    public static ItemStack built(Settings s, List<Jewel> selected) {
        ItemStack tool = ToolItem.create(Planner.material(s), baseType(s), data ->
                data.createOrReplaceAttributeValue(ModGearAttributes.TOOL_CAPACITY, Planner.capacity(s)));
        for (Jewel j : selected) ToolItem.applyJewel(tool, jewelStack(j), false);
        return tool;
    }

    /** The jewel's real stack, or a stand-in for crafted / ticked-unique placeholders. */
    public static ItemStack jewelStack(Jewel j) {
        if (j.snbt != null) return JewelReader.stackOf(j);
        ItemStack stack = new ItemStack(ModItems.JEWEL);
        VaultGearData data = VaultGearData.read(stack);
        data.setState(VaultGearState.IDENTIFIED);
        data.setItemLevel(j.level);
        data.addModifier(VaultGearModifier.AffixType.IMPLICIT, new VaultGearModifier<>(ModGearAttributes.JEWEL_SIZE, j.size));
        for (Map.Entry<String, Double> e : j.nums.entrySet()) {
            VaultGearModifier<?> mod = numModifier(e.getKey(), e.getValue());
            if (mod != null) data.addModifier(VaultGearModifier.AffixType.SUFFIX, mod);
        }
        for (String f : j.flags) {
            VaultGearModifier<?> mod = flagModifier(f);
            if (mod != null) data.addModifier(VaultGearModifier.AffixType.PREFIX, mod);
        }
        data.write(stack);
        return stack;
    }

    static VaultGearModifier<?> numModifier(String id, double v) {
        return switch (id) {
            case StatIds.MINING_SPEED -> new VaultGearModifier<>(ModGearAttributes.MINING_SPEED, (float) v);
            case StatIds.ITEM_QUANTITY -> new VaultGearModifier<>(ModGearAttributes.ITEM_QUANTITY, (float) v);
            case StatIds.ITEM_RARITY -> new VaultGearModifier<>(ModGearAttributes.ITEM_RARITY, (float) v);
            case StatIds.TRAP_DISARMING -> new VaultGearModifier<>(ModGearAttributes.TRAP_DISARMING, (float) v);
            case StatIds.COPIOUSLY -> new VaultGearModifier<>(ModGearAttributes.COPIOUSLY, (float) v);
            case StatIds.IMMORTALITY -> new VaultGearModifier<>(ModGearAttributes.IMMORTALITY, (float) v);
            case StatIds.DURABILITY -> new VaultGearModifier<>(ModGearAttributes.DURABILITY, (int) Math.round(v));
            case StatIds.REACH -> new VaultGearModifier<>(ModGearAttributes.REACH, v);
            case StatIds.HAMMER_SIZE -> new VaultGearModifier<>(ModGearAttributes.HAMMER_SIZE, (int) Math.round(v));
            default -> null;
        };
    }

    static VaultGearModifier<?> flagModifier(String id) {
        VaultGearAttribute<Boolean> a = switch (id) {
            case StatIds.SOULBOUND -> ModGearAttributes.SOULBOUND;
            case StatIds.PICKING -> ModGearAttributes.PICKING;
            case StatIds.AXING -> ModGearAttributes.AXING;
            case StatIds.SHOVELLING -> ModGearAttributes.SHOVELLING;
            case StatIds.REAPING -> ModGearAttributes.REAPING;
            case StatIds.HAMMERING -> ModGearAttributes.HAMMERING;
            case StatIds.WOODEN_AFFINITY -> ModGearAttributes.WOODEN_AFFINITY;
            case StatIds.ORNATE_AFFINITY -> ModGearAttributes.ORNATE_AFFINITY;
            case StatIds.GILDED_AFFINITY -> ModGearAttributes.GILDED_AFFINITY;
            case StatIds.LIVING_AFFINITY -> ModGearAttributes.LIVING_AFFINITY;
            case StatIds.COIN_AFFINITY -> ModGearAttributes.COIN_AFFINITY;
            case StatIds.SMELTING -> ModGearAttributes.SMELTING;
            case StatIds.PULVERIZING -> ModGearAttributes.PULVERIZING;
            case StatIds.HYDROVOID -> ModGearAttributes.HYDROVOID;
            default -> null;
        };
        return a == null ? null : new VaultGearModifier<>(a, true);
    }

    private ToolStacks() {}
}
