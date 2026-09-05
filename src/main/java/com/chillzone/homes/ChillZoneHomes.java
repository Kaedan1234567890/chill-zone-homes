package com.chillzone.homes;

import com.chillzone.homes.ui.SignInputManager;
import com.chillzone.homes.ui.BedrockInputManager;

import com.chillzone.homes.ui.HomeListMenu;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

public final class ChillZoneHomes implements ModInitializer {
    public static final String MOD_ID = "chillzonehomes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static HomeStore store;
    private static Config config;
    private static ShardStore shards;
    private static long ticks = 0;

    public static HomeStore store() { return store; }
    public static Config config() { return config; }
    public static ShardStore shards() { return shards; }

    @Override public void onInitialize() {
        SignInputManager.init();
        BedrockInputManager.init();
        config = Config.load();
        shards = ShardStore.load();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> store = HomeStore.load(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (store != null) store.save();
            if (shards != null) shards.save();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            server.execute(() -> ShardSidebar.update(handler.player, shards.shards(handler.player.getUUID()))));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ShardSidebar.forget(handler.player.getUUID()));

        // One Shard for every full two minutes the player is online, including AFK time.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ticks++;
            if (ticks % 2400L != 0L) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                shards.addShard(player.getUUID());
                ShardSidebar.update(player, shards.shards(player.getUUID()));
            }
            shards.save();
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("home")
                .requires(LuckPermsPermissions::canUseHome)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    HomeListMenu.open(player);
                    return 1;
                })
                .then(Commands.argument("homeName", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        List<Home> homes = store().getHomes(player.getUUID()).stream()
                            .sorted(Comparator.comparing(Home::name, String.CASE_INSENSITIVE_ORDER))
                            .toList();
                        String remaining = builder.getRemainingLowerCase();
                        for (Home home : homes) {
                            if (home.name().toLowerCase().startsWith(remaining)) builder.suggest(home.name());
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String requested = StringArgumentType.getString(ctx, "homeName").strip();
                        Home home = store().findHomeByName(player.getUUID(), requested);
                        if (home == null) {
                            player.sendSystemMessage(Component.literal("Home not found: " + requested).withStyle(ChatFormatting.RED));
                            return 0;
                        }
                        return HomeTeleport.teleport(player, home) ? 1 : 0;
                    }))
            );


            dispatcher.register(Commands.literal("shards")
                .requires(LuckPermsPermissions::canManageShards)
                .then(Commands.literal("give")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                int balance = shards().addShards(target.getUUID(), amount);
                                ShardSidebar.update(target, balance);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Gave " + amount + " Shards to " + target.getScoreboardName() + ". New balance: " + balance
                                ).withStyle(ChatFormatting.GREEN), false);
                                return 1;
                            }))))
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                int balance = shards().setShards(target.getUUID(), amount);
                                ShardSidebar.update(target, balance);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Set " + target.getScoreboardName() + "'s Shards to " + balance + "."
                                ).withStyle(ChatFormatting.GREEN), false);
                                return 1;
                            }))))
                .then(Commands.literal("take")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                int balance = shards().takeShards(target.getUUID(), amount);
                                ShardSidebar.update(target, balance);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Took " + amount + " Shards from " + target.getScoreboardName() + ". New balance: " + balance
                                ).withStyle(ChatFormatting.GREEN), false);
                                return 1;
                            }))))
                .then(Commands.literal("balance")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            int balance = shards().shards(target.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                target.getScoreboardName() + " has " + balance + " Shards."
                            ).withStyle(ChatFormatting.AQUA), false);
                            return 1;
                        })))
            );

            dispatcher.register(Commands.literal("homes")
                .requires(LuckPermsPermissions::canManageLimits)
                .then(Commands.literal("limit")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 28))
                            .executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                String key = config().luckPermsMetaKey;
                                String command = "lp user " + target.getScoreboardName() + " meta set " + key + " " + amount;
                                ctx.getSource().getServer().getCommands().performPrefixedCommand(
                                    ctx.getSource().getServer().createCommandSourceStack(), command
                                );
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Set " + target.getScoreboardName() + "'s home limit to " + amount + "."
                                ).withStyle(ChatFormatting.GREEN), false);
                                return 1;
                            }))
                        .then(Commands.literal("reset")
                            .executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                String key = config().luckPermsMetaKey;
                                String command = "lp user " + target.getScoreboardName() + " meta unset " + key;
                                ctx.getSource().getServer().getCommands().performPrefixedCommand(
                                    ctx.getSource().getServer().createCommandSourceStack(), command
                                );
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Reset " + target.getScoreboardName() + "'s home limit to the default."
                                ).withStyle(ChatFormatting.GREEN), false);
                                return 1;
                            }))))
            );
        });

        LOGGER.info("Chill Zone Homes initialized — /home is ready.");
    }
}
