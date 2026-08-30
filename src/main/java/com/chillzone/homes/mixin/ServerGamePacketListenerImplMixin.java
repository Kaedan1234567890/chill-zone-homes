package com.chillzone.homes.mixin;

import com.chillzone.homes.ui.SignInputManager;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleSignUpdate", at = @At("HEAD"), cancellable = true)
    private void chillzonehomes$handleVirtualSignInput(ServerboundSignUpdatePacket packet, CallbackInfo ci) {
        if (SignInputManager.handleUpdate(player, packet)) {
            ci.cancel();
        }
    }
}
