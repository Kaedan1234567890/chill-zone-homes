package com.chillzone.shardshop;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Reflection bridge to Chill Zone Homes so this stays a separate mod while using
 * the exact same in-memory shard balance/store. No second currency is created.
 */
public final class ShardBridge {
    private ShardBridge() {}

    private static Object shardStore() throws ReflectiveOperationException {
        Class<?> homes = Class.forName("com.chillzone.homes.ChillZoneHomes");
        Method shards = homes.getMethod("shards");
        Object store = shards.invoke(null);
        if (store == null) throw new IllegalStateException("Homes shard store is not ready yet.");
        return store;
    }

    public static int balance(UUID id) {
        try {
            Object store = shardStore();
            return (int) store.getClass().getMethod("shards", UUID.class).invoke(store, id);
        } catch (Exception e) {
            ChillZoneShardShop.LOGGER.error("Could not read shard balance from Chill Zone Homes", e);
            return -1;
        }
    }

    public static boolean take(ServerPlayer player, int amount) {
        if (amount <= 0) return true;
        try {
            Object store = shardStore();
            Method shards = store.getClass().getMethod("shards", UUID.class);
            Method take = store.getClass().getMethod("takeShards", UUID.class, int.class);
            int before = (int) shards.invoke(store, player.getUUID());
            if (before < amount) return false;
            int after = (int) take.invoke(store, player.getUUID(), amount);
            updateSidebar(player, after);
            return after == before - amount;
        } catch (Exception e) {
            ChillZoneShardShop.LOGGER.error("Could not deduct shards through Chill Zone Homes", e);
            return false;
        }
    }

    public static void refund(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        try {
            Object store = shardStore();
            int after = (int) store.getClass().getMethod("addShards", UUID.class, int.class)
                .invoke(store, player.getUUID(), amount);
            updateSidebar(player, after);
        } catch (Exception e) {
            ChillZoneShardShop.LOGGER.error("CRITICAL: Could not refund {} shards to {}", amount, player.getScoreboardName(), e);
        }
    }

    private static void updateSidebar(ServerPlayer player, int balance) {
        try {
            Class<?> sidebar = Class.forName("com.chillzone.homes.ShardSidebar");
            sidebar.getMethod("update", ServerPlayer.class, int.class).invoke(null, player, balance);
        } catch (Exception e) {
            ChillZoneShardShop.LOGGER.warn("Shard balance changed, but sidebar refresh failed for {}", player.getScoreboardName());
        }
    }
}
