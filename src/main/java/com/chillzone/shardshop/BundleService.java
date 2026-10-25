package com.chillzone.shardshop;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public final class BundleService {
    private BundleService() {}

    public static boolean purchase(ServerPlayer player, BundleType bundle) {
        int cost = bundle.cost();
        int balance = ShardBridge.balance(player.getUUID());
        if (balance < 0) {
            player.sendSystemMessage(Component.literal("The shard system is unavailable right now.").withStyle(ChatFormatting.RED));
            return false;
        }
        if (balance < cost) {
            player.sendSystemMessage(Component.literal("You need " + cost + " Shards. You have " + balance + ".").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!ShardBridge.take(player, cost)) {
            player.sendSystemMessage(Component.literal("Your Shards could not be charged.").withStyle(ChatFormatting.RED));
            return false;
        }
        try {
            for (MobEffectInstance effect : bundle.effects) {
                // Replace/refresh the defined bundle effect. Rebuys do not combine amplifiers.
                player.removeEffect(effect.getEffect());
                player.addEffect(new MobEffectInstance(effect));
            }
            if (bundle.customPerk != null) {
                ChillZoneShardShop.perks().set(player.getUUID(), bundle.customPerk, bundle.seconds * 1000L);
            }
            ChillZoneShardShop.logTransaction(player, "EFFECT_BUNDLE", cost, bundle.name());
            player.sendSystemMessage(Component.literal(bundle.display + " activated for " + (bundle.seconds / 60) + " minutes.").withStyle(ChatFormatting.GREEN));
            return true;
        } catch (Exception e) {
            ShardBridge.refund(player, cost);
            ChillZoneShardShop.LOGGER.error("Could not activate {} for {}", bundle, player.getScoreboardName(), e);
            player.sendSystemMessage(Component.literal("The bundle could not be applied. Your Shards were refunded.").withStyle(ChatFormatting.RED));
            return false;
        }
    }
}
