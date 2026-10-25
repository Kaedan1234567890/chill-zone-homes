package com.chillzone.shardshop;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Adds only the positive difference between the player's normal Fortune drops and effective Fortune+2 (max V). */
public final class ProspectorService {
    private ProspectorService() {}
    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) return;
            if (!ChillZoneShardShop.perks().active(sp.getUUID(), PerkStore.Perk.PROSPECTOR)) return;
            ItemStack tool = sp.getMainHandItem();
            if (tool.isEmpty()) return;
            try {
                var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var fortune = registry.getOrThrow(Enchantments.FORTUNE);
                int existing = tool.getEnchantments().getLevel(fortune);
                int target = Math.min(5, existing + 2);
                if (target <= existing) return;

                ItemStack boosted = tool.copy();
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(boosted.getEnchantments());
                mutable.set(fortune, target);
                boosted.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, mutable.toImmutable());

                List<ItemStack> normalDrops = Block.getDrops(state, level, pos, blockEntity, sp, tool);
                List<ItemStack> boostedDrops = Block.getDrops(state, level, pos, blockEntity, sp, boosted);
                Map<String,Integer> normal = counts(normalDrops);
                for (ItemStack drop : boostedDrops) {
                    if (drop.isEmpty()) continue;
                    String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(drop.getItem()).toString();
                    int extra = drop.getCount() - normal.getOrDefault(key, 0);
                    if (extra <= 0) continue;
                    ItemStack bonus = drop.copy(); bonus.setCount(extra);
                    ItemEntity entity = new ItemEntity(level, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, bonus);
                    level.addFreshEntity(entity);
                }
            } catch (Throwable t) {
                ChillZoneShardShop.LOGGER.warn("Prospector bonus processing failed for {}", sp.getScoreboardName(), t);
            }
        });
    }
    private static Map<String,Integer> counts(List<ItemStack> list){Map<String,Integer> out=new HashMap<>();for(ItemStack s:list){if(s.isEmpty())continue;String k=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();out.merge(k,s.getCount(),Integer::sum);}return out;}
}
