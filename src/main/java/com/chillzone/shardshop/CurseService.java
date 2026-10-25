package com.chillzone.shardshop;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class CurseService {
    private CurseService() {}

    public static int curseCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantments ench = stack.get(DataComponents.ENCHANTMENTS);
        if (ench == null || ench.isEmpty()) return 0;
        int count = 0;
        for (var holder : ench.keySet()) {
            String id = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("");
            if (id.endsWith(":binding_curse") || id.endsWith(":vanishing_curse")) count++;
        }
        return count;
    }

    public static boolean remove(ServerPlayer player, int slot, String expectedFingerprint) {
        ItemStack real = player.getInventory().getItem(slot);
        if (!ItemGuard.fingerprint(real).equals(expectedFingerprint)) {
            player.sendSystemMessage(Component.literal("That item changed. Curse removal cancelled.").withStyle(ChatFormatting.RED));
            return false;
        }
        ItemEnchantments ench = real.get(DataComponents.ENCHANTMENTS);
        if (ench == null) return false;
        int count = curseCount(real);
        if (count <= 0) {
            player.sendSystemMessage(Component.literal("That item has no removable curse.").withStyle(ChatFormatting.YELLOW));
            return false;
        }
        int cost = count * ChillZoneShardShop.config().removeCurseCostPerCurse;
        int balance = ShardBridge.balance(player.getUUID());
        if (balance < cost) {
            player.sendSystemMessage(Component.literal("You need " + cost + " Shards. You have " + Math.max(0, balance) + ".").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!ShardBridge.take(player, cost)) return false;
        try {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ench);
            mutable.removeIf(holder -> {
                String id = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("");
                return id.endsWith(":binding_curse") || id.endsWith(":vanishing_curse");
            });
            real.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
            player.getInventory().setChanged();
            ChillZoneShardShop.logTransaction(player, "REMOVE_CURSE", cost, "removed=" + count);
            player.sendSystemMessage(Component.literal("Removed " + count + " curse" + (count == 1 ? "" : "s") + " for " + cost + " Shards.").withStyle(ChatFormatting.GREEN));
            return true;
        } catch (Exception e) {
            ShardBridge.refund(player, cost);
            ChillZoneShardShop.LOGGER.error("Curse removal failed; refunded {}", player.getScoreboardName(), e);
            return false;
        }
    }
}
