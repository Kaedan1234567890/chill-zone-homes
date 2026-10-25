package com.chillzone.shardshop;

import com.chillzone.shardshop.ui.BedrockInputManager;
import com.chillzone.shardshop.ui.MainShopMenu;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChillZoneShardShop implements ModInitializer {
    public static final String MOD_ID="chillzoneshardshop";
    public static final Logger LOGGER=LogUtils.getLogger();
    private static DeathStore deaths;
    private static ShopConfig config;
    private static PerkStore perks;
    private static Path transactionLog;
    private static final Map<UUID,Integer> PRE_DEATH_XP=new ConcurrentHashMap<>();

    public static DeathStore deaths(){return deaths;}
    public static ShopConfig config(){return config;}
    public static PerkStore perks(){return perks;}
    public static boolean ready(){return perks!=null;}

    @Override public void onInitialize(){
        config=ShopConfig.load(); deaths=DeathStore.load(); perks=PerkStore.load();
        transactionLog=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("chill-zone-shard-shop-transactions.log");
        BedrockInputManager.init();
        ProspectorService.init();

        CommandRegistrationCallback.EVENT.register((dispatcher,registryAccess,environment)->{
            dispatcher.register(Commands.literal("shop").executes(ctx->{ServerPlayer p=ctx.getSource().getPlayerOrException();MainShopMenu.open(p);return 1;}));
            // Keep the old command as a compatibility alias so bookmarks/macros do not break.
            dispatcher.register(Commands.literal("shardshop").executes(ctx->{ServerPlayer p=ctx.getSource().getPlayerOrException();MainShopMenu.open(p);return 1;}));
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity,damageSource,damageAmount)->{
            if(entity instanceof ServerPlayer p) PRE_DEATH_XP.put(p.getUUID(),Math.max(0,p.totalExperience));
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,damageSource)->{
            if(!(entity instanceof ServerPlayer player))return;
            String cause;try{cause=damageSource.getLocalizedDeathMessage(player).getString();}catch(Exception ignored){cause="Unknown cause";}
            int preXp=PRE_DEATH_XP.getOrDefault(player.getUUID(), Math.max(0, player.totalExperience)); PRE_DEATH_XP.remove(player.getUUID());
            DeathRecord death=new DeathRecord(player.level().dimension().identifier().toString(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot(),System.currentTimeMillis(),cause,preXp,false);
            deaths.add(player.getUUID(),death);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{deaths.save();perks.save();});
        LOGGER.info("Chill Zone Shop initialized — /shop is ready.");
    }

    public static synchronized void logTransaction(ServerPlayer player,String action,int cost,String details){
        try{Files.createDirectories(transactionLog.getParent());try(BufferedWriter w=Files.newBufferedWriter(transactionLog,StandardOpenOption.CREATE,StandardOpenOption.APPEND)){w.write(Instant.now()+" | "+player.getUUID()+" | "+player.getScoreboardName()+" | "+action+" | "+cost+" shards | "+details);w.newLine();}}catch(Exception e){LOGGER.error("Could not write shard shop transaction log",e);}
    }
}
