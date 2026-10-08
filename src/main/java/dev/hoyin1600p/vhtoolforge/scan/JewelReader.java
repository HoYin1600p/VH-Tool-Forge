package dev.hoyin1600p.vhtoolforge.scan;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.hoyin1600p.vhtoolforge.model.Jewel;
import dev.hoyin1600p.vhtoolforge.stat.Stats;
import iskallia.vault.gear.VaultGearState;
import iskallia.vault.gear.attribute.VaultGearModifier;
import iskallia.vault.gear.attribute.type.VaultGearAttributeTypeMerger;
import iskallia.vault.gear.data.VaultGearData;
import iskallia.vault.init.ModGearAttributes;
import iskallia.vault.init.ModItems;
import iskallia.vault.item.tool.JewelItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Turns a jewel ItemStack into the planner's {@link Jewel}, using VH's own gear data reader. */
public final class JewelReader {
    /** Most copies of one stack or grid entry that a scan counts, so a huge stack cannot flood the list. */
    static final int MAX_COPIES_PER_ENTRY = 64;

    /** Whether the stack is a jewel item (identified or not). */
    public static boolean isJewel(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof JewelItem;
    }

    /**
     * Reads one jewel. Returns null for anything that is not an identified jewel with a size
     * (unidentified jewels have no size or stats yet).
     */
    public static Jewel read(ItemStack stack, String sourceKey, Map<String, Integer> seen) {
        if (!isJewel(stack) || stack.getTag() == null) return null;
        VaultGearData data = VaultGearData.read(stack);
        if (data.getState() != VaultGearState.IDENTIFIED) return null;
        int size = data.getFirstValue(ModGearAttributes.JEWEL_SIZE).orElse(0);
        if (size <= 0) return null;

        Jewel j = new Jewel();
        j.sourceKey = sourceKey;
        j.size = size;
        j.level = data.getItemLevel();
        j.name = stack.getHoverName().getString();
        for (VaultGearModifier.AffixType affix : VaultGearModifier.AffixType.explicits()) {
            for (VaultGearModifier<?> mod : data.getModifiers(affix)) {
                ResourceLocation id = mod.getAttribute().getRegistryName();
                if (id == null) continue;
                String stat = id.getPath();
                Object v = mod.getValue();
                if (v instanceof Boolean b) {
                    if (b) j.flags.add(stat);
                } else if (v instanceof Number n && Stats.num(stat) != null) {
                    j.nums.merge(stat, n.doubleValue(), Double::sum);
                } else {
                    j.extras.add(Stats.name(stat));
                }
            }
        }
        List<ResourceLocation> uniques = data.get(ModGearAttributes.UNIQUE_ITEM_KEY, VaultGearAttributeTypeMerger.asList());
        if (!uniques.isEmpty()) j.uniqueKey = uniques.get(0).toString();

        j.snbt = stack.getTag().toString();
        String hash = sha1(j.snbt);
        if (seen == null) seen = new HashMap<>();
        int n = seen.merge(hash, 1, Integer::sum);
        j.id = sourceKey + "|" + hash + "|" + n;
        return j;
    }

    /** Rebuilds the jewel's ItemStack from its stored NBT, for icons and tooltips. */
    public static ItemStack stackOf(Jewel j) {
        ItemStack stack = new ItemStack(ModItems.JEWEL);
        if (j.snbt != null) {
            try {
                CompoundTag tag = TagParser.parseTag(j.snbt);
                stack.setTag(tag);
            } catch (CommandSyntaxException ignored) {
            }
        }
        return stack;
    }

    /** True if a stack in storage is this exact jewel (same NBT). */
    public static boolean matches(ItemStack stack, Jewel j) {
        return isJewel(stack) && matchesTag(stack.getTag(), j);
    }

    /** True if this item NBT is the jewel's stored NBT. */
    public static boolean matchesTag(CompoundTag tag, Jewel j) {
        if (tag == null || j.snbt == null) return false;
        if (j.snbt.equals(tag.toString())) return true;
        try {
            return TagParser.parseTag(j.snbt).equals(tag);
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    /**
     * What makes two jewels interchangeable for the plan: size, level, stats and unique key.
     * Used when a stack's raw NBT differs from the scan in ways that do not matter.
     */
    public static String fingerprint(Jewel j) {
        StringBuilder sb = new StringBuilder();
        sb.append(j.size).append('|').append(j.level).append('|').append(j.uniqueKey).append('|');
        new TreeMap<>(j.nums).forEach((k, v) -> sb.append(k).append('=').append(String.format(Locale.ROOT, "%.4f", v)).append(','));
        sb.append('|');
        new TreeSet<>(j.flags).forEach(f -> sb.append(f).append(','));
        return sb.toString();
    }

    /** Fingerprint of a stack, or null if it is not an identified jewel. */
    public static String fingerprint(ItemStack stack) {
        Jewel j = read(stack, "", null);
        return j == null ? null : fingerprint(j);
    }

    private static String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    private JewelReader() {}
}
