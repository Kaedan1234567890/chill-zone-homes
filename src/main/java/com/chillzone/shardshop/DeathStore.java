package com.chillzone.shardshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Persistent rolling history: newest first, maximum 28 deaths per player. */
public final class DeathStore {
    public static final int MAX_DEATHS = 28;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<UUID, List<DeathRecord>>>(){}.getType();

    private final Path path;
    private final Map<UUID, List<DeathRecord>> data;

    private DeathStore(Path path, Map<UUID, List<DeathRecord>> data) {
        this.path = path;
        this.data = data == null ? new HashMap<>() : data;
    }

    public static DeathStore load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("chill-zone-shard-shop-deaths.json");
        try {
            if (Files.exists(path)) {
                try (Reader r = Files.newBufferedReader(path)) {
                    return new DeathStore(path, GSON.fromJson(r, TYPE));
                }
            }
        } catch (Exception e) {
            ChillZoneShardShop.LOGGER.error("Could not load death history", e);
        }
        return new DeathStore(path, new HashMap<>());
    }

    public synchronized void add(UUID player, DeathRecord death) {
        List<DeathRecord> list = data.computeIfAbsent(player, k -> new ArrayList<>());
        list.add(0, death);
        while (list.size() > MAX_DEATHS) list.remove(list.size() - 1);
        save();
    }

    public synchronized List<DeathRecord> get(UUID player) {
        return List.copyOf(data.getOrDefault(player, List.of()));
    }

    public synchronized DeathRecord latest(UUID player) {
        List<DeathRecord> list = data.get(player);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public synchronized boolean markLatestXpClaimed(UUID player, long expectedTimestamp) {
        List<DeathRecord> list = data.get(player);
        if (list == null || list.isEmpty()) return false;
        DeathRecord first = list.get(0);
        if (first.timestamp() != expectedTimestamp || first.xpRecoveryClaimed()) return false;
        list.set(0, first.withXpRecoveryClaimed(true));
        save();
        return true;
    }

    public synchronized void save() {
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) { GSON.toJson(data, TYPE, w); }
        } catch (Exception e) {
            ChillZoneShardShop.LOGGER.error("Could not save death history", e);
        }
    }
}
