package com.chillzone.homes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import java.util.Set;

public final class HomeInspection {
    private HomeInspection() {}
    public static boolean inspect(ServerPlayer viewer, String ownerName, Home home) {
        ServerLevel destination;
        try {
            destination = viewer.level().getServer().getLevel(ResourceKey.create(
                Registries.DIMENSION, Identifier.parse(home.dimension())));
        } catch (RuntimeException ex) {
            viewer.sendSystemMessage(Component.literal("The home dimension is invalid.").withStyle(ChatFormatting.RED));
            return false;
        }
        if (destination == null) {
            viewer.sendSystemMessage(Component.literal("That dimension is not available.").withStyle(ChatFormatting.RED));
            return false;
        }
        viewer.closeContainer();
        viewer.setGameMode(GameType.SPECTATOR);
        viewer.teleportTo(destination, home.x(), home.y(), home.z(), Set.of(), home.yaw(), home.pitch(), false);
        viewer.sendSystemMessage(Component.literal("Inspecting " + ownerName + "'s home '" + home.name()
                + "' in Spectator mode. Use your normal staff return command to leave.")
                .withStyle(ChatFormatting.AQUA));
        ChillZoneHomes.LOGGER.info("[Home Inspection] {} inspected {} home {} in {} at {}, {}, {}",
                viewer.getScoreboardName(), ownerName, home.name(), home.dimension(), home.x(), home.y(), home.z());
        return true;
    }
}
