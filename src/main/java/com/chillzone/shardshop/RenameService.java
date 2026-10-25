package com.chillzone.shardshop;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RenameService {
    private RenameService() {}

    public static boolean rename(ServerPlayer player, int slot, String expectedFingerprint, String requestedName) {
        String cleaned = sanitize(requestedName);
        if (cleaned.isBlank()) {
            player.sendSystemMessage(Component.literal("Rename cancelled: the name was empty.").withStyle(ChatFormatting.YELLOW));
            return false;
        }
        ItemStack real = player.getInventory().getItem(slot);
        if (!ItemGuard.fingerprint(real).equals(expectedFingerprint)) {
            player.sendSystemMessage(Component.literal("That item changed. Rename cancelled.").withStyle(ChatFormatting.RED));
            return false;
        }
        int cost = ChillZoneShardShop.config().renameCost;
        int balance = ShardBridge.balance(player.getUUID());
        if (balance < cost) {
            player.sendSystemMessage(Component.literal("You need " + cost + " Shards. You have " + Math.max(0, balance) + ".").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!ShardBridge.take(player, cost)) return false;
        try {
            real.set(DataComponents.CUSTOM_NAME, Component.literal(cleaned));
            player.getInventory().setChanged();
            ChillZoneShardShop.logTransaction(player, "ITEM_RENAME", cost, cleaned);
            player.sendSystemMessage(Component.literal("Item renamed for " + cost + " Shards.").withStyle(ChatFormatting.GREEN));
            return true;
        } catch (Exception e) {
            ShardBridge.refund(player, cost);
            ChillZoneShardShop.LOGGER.error("Rename failed for {}", player.getScoreboardName(), e);
            return false;
        }
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        String cleaned = s.replace('§', ' ').replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").strip();
        return cleaned.substring(0, Math.min(32, cleaned.length()));
    }
}
