package com.chillzone.homes;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;

/**
 * Reads LuckPerms meta without adding LuckPerms as a hard dependency.
 * If LuckPerms is absent or the player has no homes-max meta, config.defaultHomes is used.
 */
public final class HomeLimitResolver {
    private HomeLimitResolver() {}

    public static int resolve(ServerPlayer player) {
        int fallback = Math.max(ChillZoneHomes.config().defaultHomes, ChillZoneHomes.shards().purchasedHomeLimit(player.getUUID()));
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object lp = provider.getMethod("get").invoke(null);
            Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUUID());
            if (user == null) return fallback;
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
            Method getMetaValue = metaData.getClass().getMethod("getMetaValue", String.class);
            Object raw = getMetaValue.invoke(metaData, ChillZoneHomes.config().luckPermsMetaKey);
            if (raw == null) return fallback;
            int parsed = Integer.parseInt(raw.toString());
            return Math.max(fallback, Math.max(1, Math.min(parsed, ChillZoneHomes.config().maximumVisibleSlots)));
        } catch (ClassNotFoundException e) {
            return fallback;
        } catch (Exception e) {
            ChillZoneHomes.LOGGER.debug("LuckPerms meta lookup failed; using default", e);
            return fallback;
        }
    }
}
