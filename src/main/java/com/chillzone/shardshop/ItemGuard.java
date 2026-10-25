package com.chillzone.shardshop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class ItemGuard {
    private ItemGuard() {}
    public static String fingerprint(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "empty";
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getCount() + "|" + stack.getDamageValue() + "|" + stack.getComponents();
    }
}
