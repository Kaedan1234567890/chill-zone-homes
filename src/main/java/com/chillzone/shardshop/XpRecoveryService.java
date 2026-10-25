package com.chillzone.shardshop;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class XpRecoveryService {
    private XpRecoveryService() {}

    public record Quote(boolean available, String reason, int recoverable, int cost, long deathTimestamp) {}

    public static Quote quote(ServerPlayer player) {
        DeathRecord death = ChillZoneShardShop.deaths().latest(player.getUUID());
        if (death == null) return new Quote(false, "You do not have a recorded death yet.", 0, 0, 0);
        if (death.xpRecoveryClaimed()) return new Quote(false, "XP Recovery was already used for your most recent death.", 0, 0, death.timestamp());
        if (death.xpBeforeDeath() <= 0) return new Quote(false, "No recoverable XP was recorded for your most recent death.", 0, 0, death.timestamp());

        int target = (int)Math.floor(death.xpBeforeDeath() * ChillZoneShardShop.config().xpRecoveryFraction);
        // Never permit the service to take the player above their pre-death XP total.
        int roomBeforeOriginalTotal = Math.max(0, death.xpBeforeDeath() - player.totalExperience);
        int amount = Math.min(target, roomBeforeOriginalTotal);
        if (amount <= 0) return new Quote(false, "You have already regained enough XP from that death.", 0, 0, death.timestamp());
        return new Quote(true, "", amount, ChillZoneShardShop.config().xpRecoveryCost, death.timestamp());
    }

    public static boolean purchase(ServerPlayer player, Quote expected) {
        Quote current = quote(player);
        if (!current.available() || current.deathTimestamp() != expected.deathTimestamp()) {
            player.sendSystemMessage(Component.literal(current.available() ? "Your death record changed. Try again." : current.reason()).withStyle(ChatFormatting.RED));
            return false;
        }
        int balance = ShardBridge.balance(player.getUUID());
        if (balance < current.cost()) {
            player.sendSystemMessage(Component.literal("You need " + current.cost() + " Shards. You have " + Math.max(0, balance) + ".").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!ShardBridge.take(player, current.cost())) return false;
        try {
            // Mark the death first so a double-click/re-entrant GUI cannot claim twice.
            if (!ChillZoneShardShop.deaths().markLatestXpClaimed(player.getUUID(), current.deathTimestamp())) {
                ShardBridge.refund(player, current.cost());
                return false;
            }
            player.giveExperiencePoints(current.recoverable());
            ChillZoneShardShop.logTransaction(player, "XP_RECOVERY", current.cost(), "xp=" + current.recoverable());
            player.sendSystemMessage(Component.literal("Recovered " + current.recoverable() + " XP points for " + current.cost() + " Shards.").withStyle(ChatFormatting.GREEN));
            return true;
        } catch (Exception e) {
            // We intentionally do not reset the claim flag here if XP was partially applied. This prevents duplication.
            ShardBridge.refund(player, current.cost());
            ChillZoneShardShop.LOGGER.error("XP Recovery failed for {}", player.getScoreboardName(), e);
            return false;
        }
    }
}
