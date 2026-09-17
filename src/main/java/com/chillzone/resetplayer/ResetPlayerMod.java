package com.chillzone.resetplayer;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ResetPlayerMod implements ModInitializer {
    private static final Pattern PLAYER_OBJECT = Pattern.compile(
        "\\{[^{}]*?\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[^{}]*?\\}",
        Pattern.DOTALL
    );

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("resetplayer")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (String name : storedNames(context.getSource().getServer().getServerDirectory())) {
                            if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                builder.suggest(name);
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        String wanted = StringArgumentType.getString(context, "player");
                        Path file = locateStore(context.getSource().getServer().getServerDirectory());
                        if (file == null) {
                            context.getSource().sendFailure(Component.literal("Could not find chill-zone-shards.json."));
                            return 0;
                        }

                        try {
                            String json = Files.readString(file, StandardCharsets.UTF_8);
                            Pattern target = Pattern.compile(
                                "(\\{[^{}]*?\\\"name\\\"\\s*:\\s*\\\"" + Pattern.quote(wanted) +
                                "\\\"[^{}]*?\\})", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                            Matcher m = target.matcher(json);
                            if (!m.find()) {
                                context.getSource().sendFailure(Component.literal("No stored player named " + wanted + " was found."));
                                return 0;
                            }

                            String obj = m.group(1);
                            String changed = obj
                                .replaceAll("(\\\"shards\\\"\\s*:\\s*)-?\\d+", "$10")
                                .replaceAll("(\\\"playTicks\\\"\\s*:\\s*)-?\\d+", "$10")
                                .replaceAll("(\\\"playtime\\\"\\s*:\\s*)-?\\d+", "$10");

                            if (changed.equals(obj)) {
                                context.getSource().sendFailure(Component.literal(
                                    "Found " + wanted + " but could not identify its shard/playtime fields safely."));
                                return 0;
                            }

                            Path backup = file.resolveSibling(file.getFileName() + ".resetplayer-backup");
                            Files.copy(file, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            String out = json.substring(0, m.start(1)) + changed + json.substring(m.end(1));
                            Files.writeString(file, out, StandardCharsets.UTF_8);

                            context.getSource().sendSuccess(
                                () -> Component.literal("Reset " + wanted + " to 0 shards and 0 playtime. Backup created."),
                                false);
                            return 1;
                        } catch (Exception e) {
                            context.getSource().sendFailure(Component.literal("Reset failed: " + e.getMessage()));
                            return 0;
                        }
                    })))
        );
    }

    private static Path locateStore(Path serverDir) {
        List<Path> candidates = List.of(
            serverDir.resolve("chill-zone-shards.json"),
            serverDir.resolve("config/chill-zone-shards.json"),
            serverDir.resolve("world/chill-zone-shards.json")
        );
        for (Path p : candidates) if (Files.isRegularFile(p)) return p;
        return null;
    }

    private static List<String> storedNames(Path serverDir) {
        Path file = locateStore(serverDir);
        if (file == null) return List.of();
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            ArrayList<String> names = new ArrayList<>();
            Matcher m = PLAYER_OBJECT.matcher(json);
            while (m.find()) names.add(m.group(1));
            return names;
        } catch (Exception e) {
            return List.of();
        }
    }
}
