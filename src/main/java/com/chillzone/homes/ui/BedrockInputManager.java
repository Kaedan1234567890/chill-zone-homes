package com.chillzone.homes.ui;

import net.minecraft.server.level.ServerPlayer;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.function.Consumer;

/** Native Bedrock text input using Floodgate/Cumulus forms. */
public final class BedrockInputManager {
    private BedrockInputManager() {}

    public static boolean isBedrock(ServerPlayer player) {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            return api != null && api.isFloodgatePlayer(player.getUUID());
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void open(ServerPlayer player, String title, String initial, Consumer<String> callback) {
        player.closeContainer();

        try {
            FloodgateApi api = FloodgateApi.getInstance();
            if (api == null || !api.isFloodgatePlayer(player.getUUID())) {
                SignInputManager.open(player, title, callback);
                return;
            }

            String formTitle = normalizeTitle(title);
            String defaultText = initial == null ? "" : initial;

            CustomForm.Builder form = CustomForm.builder()
                .title(formTitle)
                .label("Type your answer below")
                .input("Answer", "Type here...", defaultText)
                .validResultHandler(response -> {
                    Object value = response.next();
                    String answer = value == null ? "" : value.toString().strip();
                    if (answer.length() > 32) answer = answer.substring(0, 32);
                    String finalAnswer = answer;
                    player.level().getServer().execute(() -> callback.accept(finalAnswer));
                })
                .closedOrInvalidResultHandler(() ->
                    player.level().getServer().execute(() -> callback.accept(""))
                );

            boolean sent = api.sendForm(player.getUUID(), form);
            if (!sent) {
                player.level().getServer().execute(() -> callback.accept(""));
            }
        } catch (Throwable throwable) {
            // Do not crash the server if Floodgate's API changes or is unavailable.
            // Falling back to an empty/cancel result is safer than reopening the
            // known-broken translated sign UI for a Bedrock player.
            player.level().getServer().execute(() -> callback.accept(""));
        }
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()
            || title.equalsIgnoreCase("Name this home")
            || title.equalsIgnoreCase("Rename home")
            || title.equalsIgnoreCase("Search home icons")) {
            return "Chill Zone Homes";
        }
        return title;
    }
}
