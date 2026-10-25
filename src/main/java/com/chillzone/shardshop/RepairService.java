package com.chillzone.shardshop;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RepairService {
    private RepairService() {}

    public static int cost(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() <= 0) return 0;
        double missing = stack.getDamageValue() / (double) stack.getMaxDamage();
        int cost;
        if (missing <= 0.25) {
            cost = 5 + (int)Math.round(25.0 * (missing / 0.25));
        } else if (missing <= 0.65) {
            cost = 30 + (int)Math.round(50.0 * ((missing - 0.25) / 0.40));
        } else {
            cost = 80 + (int)Math.round(70.0 * ((missing - 0.65) / 0.35));
        }
        return Math.max(5, Math.min(ChillZoneShardShop.config().repairMaxCost, cost));
    }

    public static boolean repair(ServerPlayer player, int inventorySlot, String expectedFingerprint) {
        ItemStack real = player.getInventory().getItem(inventorySlot);
        if (!ItemGuard.fingerprint(real).equals(expectedFingerprint)) {
            player.sendSystemMessage(Component.literal("That item changed. Repair cancelled and you were not charged.").withStyle(ChatFormatting.RED));
            return false;
        }
        int cost = cost(real);
        if (cost <= 0) {
            player.sendSystemMessage(Component.literal("That item does not need repairing.").withStyle(ChatFormatting.YELLOW));
            return false;
        }
        int balance = ShardBridge.balance(player.getUUID());
        if (balance < cost) {
            player.sendSystemMessage(Component.literal("You need " + cost + " Shards. You have " + Math.max(0, balance) + ".").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!ShardBridge.take(player, cost)) return false;
        try {
            real.setDamageValue(0);
            player.getInventory().setChanged();
            ChillZoneShardShop.logTransaction(player, "ITEM_REPAIR", cost, ItemGuard.fingerprint(real));
            player.sendSystemMessage(Component.literal("Item repaired for " + cost + " Shards.").withStyle(ChatFormatting.GREEN));
            return true;
        } catch (Exception e) {
            ShardBridge.refund(player, cost);
            ChillZoneShardShop.LOGGER.error("Repair failed; refunded {}", player.getScoreboardName(), e);
            return false;
        }
    }
}
