package com.chillzone.shardshop;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class DeathTeleport {
    private DeathTeleport() {}

    public static boolean teleport(ServerPlayer player, DeathRecord death) {
        ServerLevel level = player.level().getServer().getLevel(
            ResourceKey.create(Registries.DIMENSION, Identifier.parse(death.dimension()))
        );
        if (level == null) {
            player.sendSystemMessage(Component.literal("That death dimension no longer exists.").withStyle(ChatFormatting.RED));
            return false;
        }

        Destination destination = findSafe(level, death);
        if (destination == null) {
            player.sendSystemMessage(Component.literal(
                "No safe teleport location was found within " + ChillZoneShardShop.config().safeTeleportRadius +
                    " blocks of that death. You were not charged."
            ).withStyle(ChatFormatting.RED));
            return false;
        }

        int cost = ChillZoneShardShop.config().deathTeleportCost;
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
            player.sendSystemMessage(Component.literal("Your Shards could not be charged. Teleport cancelled.").withStyle(ChatFormatting.RED));
            return false;
        }

        try {
            player.closeContainer();
            player.teleportTo(level, destination.x, destination.y, destination.z, Set.of(), death.yaw(), death.pitch(), false);
            level.playSound(null, BlockPos.containing(destination.x, destination.y, destination.z),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
            player.sendOverlayMessage(Component.literal("Returned near your death for " + cost + " Shards.")
                .withStyle(ChatFormatting.AQUA));
            ChillZoneShardShop.logTransaction(player, "DEATH_TELEPORT", cost,
                death.dimension() + " @ " + fmt(death.x()) + ", " + fmt(death.y()) + ", " + fmt(death.z()));
            return true;
        } catch (Exception e) {
            ShardBridge.refund(player, cost);
            ChillZoneShardShop.LOGGER.error("Death teleport failed; refunded {} shards to {}", cost, player.getScoreboardName(), e);
            player.sendSystemMessage(Component.literal("Teleport failed. Your " + cost + " Shards were refunded.").withStyle(ChatFormatting.RED));
            return false;
        }
    }

    private static Destination findSafe(ServerLevel level, DeathRecord death) {
        BlockPos exact = BlockPos.containing(death.x(), death.y(), death.z());
        if (safe(level, exact)) return new Destination(death.x(), death.y(), death.z());

        int r = ChillZoneShardShop.config().safeTeleportRadius;
        int vr = ChillZoneShardShop.config().safeTeleportVerticalRadius;
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if ((dx * dx) + (dz * dz) > r * r) continue;
                for (int dy = -vr; dy <= vr; dy++) {
                    candidates.add(exact.offset(dx, dy, dz));
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(p -> p.distSqr(exact)));
        for (BlockPos p : candidates) {
            if (safe(level, p)) return new Destination(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        }
        return null;
    }

    private static boolean safe(ServerLevel level, BlockPos feet) {
        // Deaths can be in a different dimension from the player. In that case
        // the destination chunk may not currently be loaded. Force-load the
        // small destination area before checking collision/fluid safety so
        // Nether, Nether-roof, End, and cross-dimension deaths can be used.
        level.getChunk(feet.getX() >> 4, feet.getZ() >> 4);

        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(head).isEmpty()) return false;
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) return false;
        if (!level.getBlockState(head).getCollisionShape(level, head).isEmpty()) return false;
        return !level.getBlockState(floor).getCollisionShape(level, floor).isEmpty();
    }

    private static String fmt(double n) { return String.format(java.util.Locale.ROOT, "%.1f", n); }
    private record Destination(double x, double y, double z) {}
}
