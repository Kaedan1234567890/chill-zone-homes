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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persists custom timed perks that vanilla potion NBT cannot represent. */
public final class PerkStore {
    public enum Perk { SCHOLARS_BLESSING, PROSPECTOR }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<UUID, Map<Perk, Long>>>(){}.getType();
    private final Path path;
    private final Map<UUID, Map<Perk, Long>> expiry;

    private PerkStore(Path path, Map<UUID, Map<Perk, Long>> expiry) {
        this.path = path;
        this.expiry = expiry == null ? new HashMap<>() : expiry;
    }

    public static PerkStore load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("chill-zone-shard-shop-perks.json");
        try {
            if (Files.exists(path)) {
                try (Reader r = Files.newBufferedReader(path)) {
                    return new PerkStore(path, GSON.fromJson(r, TYPE));
                }
            }
        } catch (Exception e) {
            ChillZoneShardShop.LOGGER.error("Could not load custom shop perks", e);
        }
        return new PerkStore(path, new HashMap<>());
    }

    public synchronized void set(UUID player, Perk perk, long durationMillis) {
        // Rebuying refreshes the defined duration; it never stacks duration or strength.
        expiry.computeIfAbsent(player, x -> new HashMap<>()).put(perk, System.currentTimeMillis() + durationMillis);
        save();
    }

    public synchronized boolean active(UUID player, Perk perk) {
        Map<Perk, Long> map = expiry.get(player);
        if (map == null) return false;
        Long until = map.get(perk);
        if (until == null) return false;
        if (until <= System.currentTimeMillis()) {
            map.remove(perk);
            if (map.isEmpty()) expiry.remove(player);
            save();
            return false;
        }
        return true;
    }

    public synchronized void save() {
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) { GSON.toJson(expiry, TYPE, w); }
        } catch (Exception e) {
            ChillZoneShardShop.LOGGER.error("Could not save custom shop perks", e);
        }
    }
}
