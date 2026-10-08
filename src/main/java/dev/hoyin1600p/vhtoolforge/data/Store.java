package dev.hoyin1600p.vhtoolforge.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hoyin1600p.vhtoolforge.VHToolForge;
import dev.hoyin1600p.vhtoolforge.model.Jewel;
import dev.hoyin1600p.vhtoolforge.model.Settings;
import dev.hoyin1600p.vhtoolforge.model.Source;
import dev.hoyin1600p.vhtoolforge.util.ModPaths;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything the mod remembers for one world or server: the planner settings, scanned storages,
 * the jewels found in them, and which planned jewels are already in the backpack.
 * Kept locally in {@code config/vhtoolforge/data/<world>.json}.
 */
public final class Store {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Settings settings = new Settings();
    public List<Source> sources = new ArrayList<>();
    public List<Jewel> jewels = new ArrayList<>();
    /**
     * The plan being collected. Locked when collecting starts, so jewels leaving storage
     * do not change it mid-way. Null when no plan is locked.
     */
    public List<Jewel> plan;
    public Set<String> collected = new LinkedHashSet<>();

    private transient String worldKey;
    private static Store current;

    /** The store for the world the player is in now (loaded on first use, switched when the world changes). */
    public static Store get() {
        String key = worldKey();
        if (current == null || !key.equals(current.worldKey)) {
            current = load(key);
        }
        return current;
    }

    private static String worldKey() {
        Minecraft mc = Minecraft.getInstance();
        ServerData server = mc.getCurrentServer();
        String raw;
        if (server != null) raw = "server_" + server.ip;
        else if (mc.getSingleplayerServer() != null) raw = "local_" + mc.getSingleplayerServer().getWorldData().getLevelName();
        else raw = "unknown";
        return raw.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static Path file(String key) {
        return ModPaths.dataDir().resolve(key + ".json");
    }

    private static Store load(String key) {
        Path f = file(key);
        Store s = null;
        if (Files.isRegularFile(f)) {
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                s = GSON.fromJson(r, Store.class);
            } catch (Exception e) {
                VHToolForge.LOG.error("Could not read {}; starting empty", f, e);
            }
        }
        if (s == null) s = new Store();
        if (s.settings == null) s.settings = new Settings();
        if (s.sources == null) s.sources = new ArrayList<>();
        if (s.jewels == null) s.jewels = new ArrayList<>();
        if (s.collected == null) s.collected = new LinkedHashSet<>();
        s.worldKey = key;
        return s;
    }

    /** Writes the store to its world's JSON file, through a temporary file so a crash cannot leave half a file. */
    public void save() {
        Path f = file(worldKey);
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            VHToolForge.LOG.error("Could not save {}", f, e);
        }
    }

    /** The scanned storage with this key, or null. */
    public Source source(String key) {
        for (Source s : sources) if (s.key.equals(key)) return s;
        return null;
    }

    /** Replaces a storage's jewel list with a fresh scan. */
    public void replaceSource(Source src, List<Jewel> found) {
        Source old = source(src.key);
        if (old != null) {
            src.enabled = old.enabled;
            sources.remove(old);
        }
        sources.add(src);
        jewels.removeIf(j -> src.key.equals(j.sourceKey));
        jewels.addAll(found);
        save();
    }

    /** Removes a storage and the jewels scanned from it. */
    public void forgetSource(String key) {
        sources.removeIf(s -> s.key.equals(key));
        jewels.removeIf(j -> key.equals(j.sourceKey));
        save();
    }

    /** Freezes these jewels as the plan and clears the collected marks. */
    public void lockPlan(List<Jewel> selected) {
        plan = new ArrayList<>(selected);
        collected.clear();
        save();
    }

    /** Drops the locked plan and the collected marks. */
    public void unlockPlan() {
        plan = null;
        collected.clear();
        save();
    }

    /** Planned jewels in a storage that are not in the backpack yet. */
    public List<Jewel> toCollect(String sourceKey) {
        List<Jewel> out = new ArrayList<>();
        if (plan == null) return out;
        for (Jewel j : plan) if (sourceKey.equals(j.sourceKey) && !collected.contains(j.id)) out.add(j);
        return out;
    }

    /** A jewel left its storage (moved into the backpack). */
    public void removeJewel(String id) {
        for (Iterator<Jewel> it = jewels.iterator(); it.hasNext(); ) {
            if (it.next().id.equals(id)) {
                it.remove();
                break;
            }
        }
    }
}
