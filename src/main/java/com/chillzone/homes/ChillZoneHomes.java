package com.chillzone.homes;

import com.chillzone.homes.ui.HomeListMenu;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChillZoneHomes implements ModInitializer {
    public static final String MOD_ID = "chillzonehomes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static HomeStore store;
    private static Config config;

    public static HomeStore store() { return store; }
    public static Config config() { return config; }

    @Override public void onInitialize() {
        config = Config.load();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> store = HomeStore.load(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { if (store != null) store.save(); });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("home").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                HomeListMenu.open(player, 0);
                return 1;
            }))
        );
        LOGGER.info("Chill Zone Homes initialized — /home is ready.");
    }
}
