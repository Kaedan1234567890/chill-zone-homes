package com.chillzone.homes.ui;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Cross-play text input used for home names and icon searches.
 *
 * 0.3.2 intentionally uses Minecraft's vanilla sign editor instead of an anvil,
 * because Geyser/Bedrock can interact with the sign editor reliably while the
 * custom anvil rename field is not translated correctly.
 */
public final class NameInputMenu {
    private NameInputMenu() {}

    public static void open(ServerPlayer player, String initial, Consumer<String> callback) {
        open(player, initial, "Type answer here", callback);
    }

    public static void open(ServerPlayer player, String initial, String title, Consumer<String> callback) {
        // "initial" is deliberately not inserted into the editable line.
        // The bottom line stays empty so Java and Bedrock players type a fresh answer.
        SignInputManager.open(player, title, callback);
    }
}
