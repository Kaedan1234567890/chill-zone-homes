package com.chillzone.homes;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Tiny per-player packet sidebar: player name as title, one Shards line. */
public final class ShardSidebar {
    private static final Scoreboard DUMMY = new Scoreboard();
    private static final Set<UUID> REGISTERED = new HashSet<>();
    private ShardSidebar() {}

    public static void forget(UUID id) { REGISTERED.remove(id); }

    public static void update(net.minecraft.server.level.ServerPlayer player, int shards) {
        String objectiveName = "czs_" + player.getUUID().toString().replace("-", "").substring(0, 12);
        Objective objective = new Objective(
            DUMMY,
            objectiveName,
            ObjectiveCriteria.DUMMY,
            Component.literal(player.getScoreboardName()),
            ObjectiveCriteria.RenderType.INTEGER,
            false,
            null
        );

        if (REGISTERED.add(player.getUUID())) {
            player.connection.send(new ClientboundSetObjectivePacket(objective, 0));
            player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));
        }
        player.connection.send(new ClientboundSetScorePacket(
            "Shards",
            objectiveName,
            shards,
            Optional.of(Component.literal("Shards")),
            Optional.empty()
        ));
    }
}
