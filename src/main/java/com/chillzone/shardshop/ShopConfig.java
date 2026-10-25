package com.chillzone.shardshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Economy values can be tuned later without recompiling. */
public final class ShopConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int deathTeleportCost = 15;
    public int safeTeleportRadius = 10;
    public int safeTeleportVerticalRadius = 4;

    public int minersFocusCost = 30;
    public int deepDiverCost = 30;
    public int fortunesFavorCost = 35;
    public int scholarsBlessingCost = 35;
    public int warriorsFuryCost = 65;
    public int buildersFocusCost = 35;
    public int voidWalkerCost = 25;
    public int netherWorkerCost = 30;
    public int prospectorCost = 50;

    public int renameCost = 5;
    public int removeCurseCostPerCurse = 25;
    public int xpRecoveryCost = 25;
    public double xpRecoveryFraction = 0.50;
    public int repairMaxCost = 150;

    public static ShopConfig load() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("chill-zone-shard-shop.json");
        ShopConfig cfg = new ShopConfig();
        try {
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    ShopConfig loaded = GSON.fromJson(r, ShopConfig.class);
                    if (loaded != null) cfg = loaded;
                }
            }
            // One-time migration of every historical default we used before the 15-Shard design.
            if (cfg.deathTeleportCost == 50 || cfg.deathTeleportCost == 20 || cfg.deathTeleportCost == 25) {
                cfg.deathTeleportCost = 15;
            }
            cfg.deathTeleportCost = Math.max(0, cfg.deathTeleportCost);
            cfg.safeTeleportRadius = Math.max(0, Math.min(10, cfg.safeTeleportRadius));
            cfg.safeTeleportVerticalRadius = Math.max(0, Math.min(8, cfg.safeTeleportVerticalRadius));
            cfg.repairMaxCost = Math.max(1, Math.min(500, cfg.repairMaxCost));
            cfg.xpRecoveryFraction = Math.max(0.0, Math.min(1.0, cfg.xpRecoveryFraction));
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) { GSON.toJson(cfg, w); }
        } catch (Exception e) {
            ChillZoneShardShop.LOGGER.error("Could not load/save shard shop config", e);
        }
        return cfg;
    }
}
