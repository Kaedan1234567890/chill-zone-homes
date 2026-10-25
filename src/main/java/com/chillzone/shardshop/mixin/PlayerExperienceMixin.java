package com.chillzone.shardshop.mixin;

import com.chillzone.shardshop.ChillZoneShardShop;
import com.chillzone.shardshop.PerkStore;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class PlayerExperienceMixin {
    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int chillzone$scholarsBlessing(int points) {
        if (points <= 0) return points;
        Player self = (Player)(Object)this;
        if (!ChillZoneShardShop.ready() || !ChillZoneShardShop.perks().active(self.getUUID(), PerkStore.Perk.SCHOLARS_BLESSING)) return points;
        return points + Math.max(1, points / 2);
    }
}
