package com.chillzone.homes;

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

/** Persistent shard balances and shard-purchased home allowance. */
public final class ShardStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<UUID, Record>>(){}.getType();
    private final Path path;
    private final Map<UUID, Record> data;

    public static final class Record {
        public int shards = 0;
        public int purchasedHomeLimit = 3;
    }

    private ShardStore(Path path, Map<UUID, Record> data) {
        this.path = path;
        this.data = data == null ? new HashMap<>() : data;
    }

    public static ShardStore load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("chill-zone-shards.json");
        try {
            if (Files.exists(path)) {
                try (Reader r = Files.newBufferedReader(path)) {
                    Map<UUID, Record> loaded = GSON.fromJson(r, TYPE);
                    return new ShardStore(path, loaded);
                }
            }
        } catch (Exception e) {
            ChillZoneHomes.LOGGER.error("Could not load shard data", e);
        }
        return new ShardStore(path, new HashMap<>());
    }

    private Record record(UUID id) {
        Record r = data.computeIfAbsent(id, k -> new Record());
        r.shards = Math.max(0, r.shards);
        r.purchasedHomeLimit = Math.max(3, Math.min(28, r.purchasedHomeLimit));
        return r;
    }

    public int shards(UUID id) { return record(id).shards; }
    public int purchasedHomeLimit(UUID id) { return record(id).purchasedHomeLimit; }

    public void addShard(UUID id) {
        record(id).shards++;
    }

    public int addShards(UUID id, int amount) {
        Record r = record(id);
        r.shards = Math.max(0, r.shards + Math.max(0, amount));
        save();
        return r.shards;
    }

    public int setShards(UUID id, int amount) {
        Record r = record(id);
        r.shards = Math.max(0, amount);
        save();
        return r.shards;
    }

    public int takeShards(UUID id, int amount) {
        Record r = record(id);
        r.shards = Math.max(0, r.shards - Math.max(0, amount));
        save();
        return r.shards;
    }

    public boolean purchaseNextHome(UUID id, int nextHomeNumber) {
        Record r = record(id);
        if (nextHomeNumber < 4 || nextHomeNumber > 28) return false;
        int cost = costForHome(nextHomeNumber);
        if (r.shards < cost) return false;
        r.shards -= cost;
        r.purchasedHomeLimit = Math.max(r.purchasedHomeLimit, nextHomeNumber);
        save();
        return true;
    }

    public static int costForHome(int homeNumber) {
        // Home 4 = 100; each additional home costs 50 more.
        return 100 + ((homeNumber - 4) * 50);
    }

    public synchronized void save() {
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) { GSON.toJson(data, TYPE, w); }
        } catch (Exception e) {
            ChillZoneHomes.LOGGER.error("Could not save shard data", e);
        }
    }
}
