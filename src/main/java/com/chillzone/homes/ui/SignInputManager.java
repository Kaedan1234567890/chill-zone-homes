package com.chillzone.homes.ui;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Server-side, cross-play sign text input.
 *
 * No real sign is placed in the world. A temporary sign is sent only to the
 * requesting player's client, the vanilla sign editor is opened, and the
 * returned sign-update packet is intercepted by our mixin. The real block is
 * immediately re-sent afterwards so nothing in the world is changed.
 */
public final class SignInputManager {
    private static final Map<UUID, PendingInput> PENDING = new HashMap<>();
    private static final List<ScheduledOpen> OPEN_QUEUE = new ArrayList<>();
    private static boolean initialized;

    private SignInputManager() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<ScheduledOpen> it = OPEN_QUEUE.iterator();
            while (it.hasNext()) {
                ScheduledOpen scheduled = it.next();
                scheduled.ticks--;
                if (scheduled.ticks <= 0) {
                    it.remove();
                    ServerPlayer player = server.getPlayerList().getPlayer(scheduled.playerId);
                    if (player != null && player.connection != null) {
                        openNow(player, scheduled.prompt, scheduled.callback);
                    }
                }
            }
        });
    }

    public static void open(ServerPlayer player, String prompt, Consumer<String> callback) {
        // Close the chest/menu first. A short delay prevents Bedrock/Geyser from
        // instantly dismissing the sign screen while the previous menu closes.
        player.closeContainer();
        PENDING.remove(player.getUUID());
        OPEN_QUEUE.removeIf(s -> s.playerId.equals(player.getUUID()));
        OPEN_QUEUE.add(new ScheduledOpen(player.getUUID(), prompt, callback, 3));
    }

    private static void openNow(ServerPlayer player, String prompt, Consumer<String> callback) {
        // Keep the fake sign close enough that both Java and Geyser treat the
        // edit screen as a normal nearby sign interaction.
        BlockPos pos = player.blockPosition().above(3);
        BlockState signState = Blocks.OAK_SIGN.defaultBlockState();

        String heading = (prompt == null || prompt.isBlank()) ? "Type answer here" : prompt;
        // The sign has only four vanilla text rows. We reserve the last row for
        // the player's answer, matching the requested layout.
        Component[] lines = new Component[] {
            Component.literal(fitHeading(heading)),
            Component.literal("      ↓      "),
            Component.empty(),
            Component.empty()
        };

        SignBlockEntity fakeSign = new SignBlockEntity(pos, signState);
        fakeSign.setText(new SignText(lines, lines, DyeColor.BLACK, false), true);

        PENDING.put(player.getUUID(), new PendingInput(pos, callback));

        player.connection.send(new ClientboundBlockUpdatePacket(pos, signState));
        player.connection.send(ClientboundBlockEntityDataPacket.create(fakeSign));
        player.connection.send(new ClientboundOpenSignEditorPacket(pos, true));
    }

    private static String fitHeading(String heading) {
        String h = heading.strip();
        if (h.equalsIgnoreCase("Name this home") || h.equalsIgnoreCase("Rename home") || h.equalsIgnoreCase("Search home icons")) {
            return "Type answer here";
        }
        return h.length() > 24 ? h.substring(0, 24) : h;
    }

    /** Called from ServerGamePacketListenerImplMixin. Returns true when consumed. */
    public static boolean handleUpdate(ServerPlayer player, ServerboundSignUpdatePacket packet) {
        PendingInput pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.pos.equals(packet.getPos())) return false;

        PENDING.remove(player.getUUID());
        restoreRealBlock(player, pending.pos);

        String[] lines = packet.getLines();
        String answer = "";
        if (lines != null) {
            // Preferred input line is the fourth/bottom row. Line three is a
            // fallback for clients that place the cursor one row higher.
            if (lines.length > 3 && lines[3] != null) answer = lines[3].strip();
            if (answer.isBlank() && lines.length > 2 && lines[2] != null) answer = lines[2].strip();
        }
        if (answer.length() > 32) answer = answer.substring(0, 32);

        final String submitted = answer;
        pending.callback.accept(submitted);
        return true;
    }

    private static void restoreRealBlock(ServerPlayer player, BlockPos pos) {
        player.connection.send(new ClientboundBlockUpdatePacket(player.level(), pos));
        BlockEntity real = player.level().getBlockEntity(pos);
        if (real != null) {
            player.connection.send(ClientboundBlockEntityDataPacket.create(real));
        }
    }

    private record PendingInput(BlockPos pos, Consumer<String> callback) {}

    private static final class ScheduledOpen {
        final UUID playerId;
        final String prompt;
        final Consumer<String> callback;
        int ticks;

        ScheduledOpen(UUID playerId, String prompt, Consumer<String> callback, int ticks) {
            this.playerId = playerId;
            this.prompt = prompt;
            this.callback = callback;
            this.ticks = ticks;
        }
    }
}
