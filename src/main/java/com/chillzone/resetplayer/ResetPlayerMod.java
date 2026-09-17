package com.chillzone.resetplayer;

import com.chillzone.homes.ChillZoneHomes;
import com.chillzone.homes.ShardStore;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public final class ResetPlayerMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("resetplayer")
                .requires(source -> source.permissions().hasPermission(
                    net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((context, builder) ->
                        SharedSuggestionProvider.suggest(
                            ChillZoneHomes.shards().rankedBalances().stream()
                                .map(ShardStore.BalanceEntry::name),
                            builder))
                    .executes(context -> {
                        String name = StringArgumentType.getString(context, "player");
                        ShardStore store = ChillZoneHomes.shards();
                        ShardStore.BalanceEntry entry = store.findByName(name);

                        if (entry == null) {
                            context.getSource().sendFailure(
                                Component.literal("No stored /baltop player named " + name + " was found."));
                            return 0;
                        }

                        store.setShards(entry.uuid(), 0);
                        store.setPlayTicks(entry.uuid(), 0L);
                        store.save();

                        context.getSource().sendSuccess(
                            () -> Component.literal("Reset " + entry.name()
                                + ": shards = 0, playtime = 0. They will no longer appear on /baltop."),
                            false);
                        return 1;
                    })))
        );
    }
}
