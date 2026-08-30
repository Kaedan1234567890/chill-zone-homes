package com.chillzone.homes.ui;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Cross-play text input.
 *
 * Java keeps the known-good Fix 4 vanilla sign editor path. Bedrock/Floodgate
 * uses a native Bedrock CustomForm text box instead of asking Geyser to
 * translate Java's sign editor, which is unreliable and can close immediately.
 */
public final class NameInputMenu {
    private NameInputMenu() {}

    public static void open(ServerPlayer player, String initial, Consumer<String> callback) {
        open(player, initial, "Type answer here", callback);
    }

    public static void open(ServerPlayer player, String initial, String title, Consumer<String> callback) {
        if (BedrockInputManager.isBedrock(player)) {
            BedrockInputManager.open(player, title, initial, callback);
            return;
        }

        // Java path intentionally remains identical to Fix 4.
        SignInputManager.open(player, title, callback);
    }
}
