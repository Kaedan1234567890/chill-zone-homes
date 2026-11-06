package com.chillzone.homes.ui;

import com.chillzone.homes.ChillZoneHomes;
import com.chillzone.homes.Home;
import com.chillzone.homes.HomeLimitResolver;
import com.chillzone.homes.HomeTeleport;
import com.chillzone.homes.ShardSidebar;
import com.chillzone.homes.ShardStore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bedrock-native Homes menus; Java players continue to use the existing chest menus. */
public final class BedrockHomesMenu {
    private static final List<QueuedForm> QUEUE = new ArrayList<>();
    private static boolean initialized = false;

    private BedrockHomesMenu() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (int i = QUEUE.size() - 1; i >= 0; i--) {
                QueuedForm queued = QUEUE.get(i);
                if (--queued.waitTicks > 0) continue;
                QUEUE.remove(i);
                ServerPlayer player = server.getPlayerList().getPlayer(queued.id);
                if (player == null) continue;
                try {
                    boolean sent = FloodgateApi.getInstance().sendForm(player.getUUID(), queued.form);
                    if (!sent) player.sendSystemMessage(Component.literal("Could not open Bedrock Homes menu. Try /home again.").withStyle(ChatFormatting.RED));
                } catch (Exception ex) {
                    ChillZoneHomes.LOGGER.warn("Bedrock home form send failed for {}", player.getScoreboardName(), ex);
                    player.sendSystemMessage(Component.literal("Could not open Bedrock Homes menu. Try /home again.").withStyle(ChatFormatting.RED));
                }
            }
        });
    }

    private static void send(ServerPlayer player, SimpleForm.Builder form) {
        player.closeContainer();
        QUEUE.removeIf(q -> q.id.equals(player.getUUID()));
        // Allow Geyser a few ticks to finish closing any previous inventory/form.
        QUEUE.add(new QueuedForm(player.getUUID(), form, 5));
    }

    private static void onServer(ServerPlayer player, Runnable action) {
        MinecraftServer server = player.level().getServer();
        server.execute(() -> {
            if (server.getPlayerList().getPlayer(player.getUUID()) == player) action.run();
        });
    }

    public static void open(ServerPlayer player) {
        List<Home> homes = ChillZoneHomes.store().getHomes(player.getUUID());
        int limit = HomeLimitResolver.resolve(player);
        int max = Math.min(28, ChillZoneHomes.config().maximumVisibleSlots);
        SimpleForm.Builder form = SimpleForm.builder().title("Chill Zone Homes")
            .content("Your homes: " + homes.size() + " / " + limit + " | Shards: " + ChillZoneHomes.shards().shards(player.getUUID()));
        List<Runnable> choices = new ArrayList<>();

        if (homes.size() < limit && homes.size() < max) {
            form.button("Create New Home");
            choices.add(() -> create(player));
        }
        if (homes.size() >= limit && limit < max && limit >= 1) {
            int nextHome = limit + 1;
            int cost = ShardStore.costForHome(nextHome);
            form.button("Unlock Home " + nextHome + " (" + cost + " Shards)");
            choices.add(() -> unlock(player));
        }
        for (int index = 0; index < homes.size(); index++) {
            int selected = index;
            Home home = homes.get(index);
            form.button("Home " + (index + 1) + ": " + home.name());
            choices.add(() -> openActions(player, selected));
        }
        if (homes.isEmpty() && choices.isEmpty()) {
            form.button("No home slots available");
            choices.add(() -> open(player));
        }
        form.validResultHandler(response -> {
            int clicked = response.clickedButtonId();
            if (clicked >= 0 && clicked < choices.size()) onServer(player, choices.get(clicked));
        });
        send(player, form);
    }

    private static void create(ServerPlayer player) {
        int limit = HomeLimitResolver.resolve(player);
        int max = Math.min(28, ChillZoneHomes.config().maximumVisibleSlots);
        if (ChillZoneHomes.store().getHomes(player.getUUID()).size() >= Math.min(limit, max)) {
            player.sendSystemMessage(Component.literal("No unlocked home slot available.").withStyle(ChatFormatting.RED));
            open(player);
            return;
        }
        NameInputMenu.open(player, "", "Name this home", name -> {
            if (name == null || name.isBlank()) {
                player.sendSystemMessage(Component.literal("Home creation cancelled.").withStyle(ChatFormatting.YELLOW));
                open(player);
                return;
            }
            if (ChillZoneHomes.store().findHomeByName(player.getUUID(), name) != null) {
                player.sendSystemMessage(Component.literal("You already have a home with that name.").withStyle(ChatFormatting.RED));
                open(player);
                return;
            }
            if (ChillZoneHomes.store().getHomes(player.getUUID()).size() >=
                Math.min(HomeLimitResolver.resolve(player), Math.min(28, ChillZoneHomes.config().maximumVisibleSlots))) {
                player.sendSystemMessage(Component.literal("No unlocked home slot available.").withStyle(ChatFormatting.RED));
                open(player);
                return;
            }
            Home home = new Home(name, player.level().dimension().identifier().toString(),
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), "red_bed");
            ChillZoneHomes.store().addHome(player.getUUID(), home);
            player.sendSystemMessage(Component.literal("Home \"" + name + "\" set.").withStyle(ChatFormatting.GREEN));
            open(player);
        });
    }

    private static void unlock(ServerPlayer player) {
        int limit = HomeLimitResolver.resolve(player);
        int next = limit + 1;
        int max = Math.min(28, ChillZoneHomes.config().maximumVisibleSlots);
        if (next < 2 || next > max || ChillZoneHomes.store().getHomes(player.getUUID()).size() < limit) {
            open(player);
            return;
        }
        int cost = ShardStore.costForHome(next);
        int balance = ChillZoneHomes.shards().shards(player.getUUID());
        if (balance < cost) {
            player.sendSystemMessage(Component.literal("You need " + cost + " Shards. You have " + balance + ".").withStyle(ChatFormatting.RED));
        } else if (ChillZoneHomes.shards().purchaseNextHome(player.getUUID(), next)) {
            ShardSidebar.update(player, ChillZoneHomes.shards().shards(player.getUUID()));
            player.sendSystemMessage(Component.literal("Home " + next + " unlocked for " + cost + " Shards!").withStyle(ChatFormatting.GREEN));
        }
        open(player);
    }

    public static void openActions(ServerPlayer player, int index) {
        Home home = ChillZoneHomes.store().getHome(player.getUUID(), index);
        if (home == null) { open(player); return; }
        SimpleForm.Builder form = SimpleForm.builder().title("Home: " + home.name())
            .content("Choose what to do with this home.")
            .button("Teleport")
            .button("Rename")
            .button("Change Icon")
            .button("Delete")
            .button("Back");
        form.validResultHandler(response -> onServer(player, () -> {
            int clicked = response.clickedButtonId();
            if (clicked == 0) {
                Home current = ChillZoneHomes.store().getHome(player.getUUID(), index);
                if (current != null) HomeTeleport.teleport(player, current);
            }
            else if (clicked == 1) rename(player, index);
            else if (clicked == 2) iconMenu(player, index);
            else if (clicked == 3) confirmDelete(player, index);
            else if (clicked == 4) open(player);
        }));
        send(player, form);
    }

    private static void rename(ServerPlayer player, int index) {
        Home current = ChillZoneHomes.store().getHome(player.getUUID(), index);
        if (current == null) { open(player); return; }
        NameInputMenu.open(player, current.name(), "Rename home", name -> {
            Home latest = ChillZoneHomes.store().getHome(player.getUUID(), index);
            if (latest != null && name != null && !name.isBlank()) {
                Home duplicate = ChillZoneHomes.store().findHomeByName(player.getUUID(), name);
                if (duplicate == null || duplicate == latest) {
                    ChillZoneHomes.store().setHome(player.getUUID(), index, latest.withName(name));
                } else {
                    player.sendSystemMessage(Component.literal("You already have a home with that name.").withStyle(ChatFormatting.RED));
                }
            }
            openActions(player, index);
        });
    }

    private static void confirmDelete(ServerPlayer player, int index) {
        Home current = ChillZoneHomes.store().getHome(player.getUUID(), index);
        if (current == null) { open(player); return; }
        String name = current.name();
        SimpleForm.Builder form = SimpleForm.builder().title("Delete Home")
            .content("Permanently delete \"" + name + "\"?")
            .button("Yes, Delete")
            .button("Cancel");
        form.validResultHandler(response -> onServer(player, () -> {
            if (response.clickedButtonId() == 0) {
                Home latest = ChillZoneHomes.store().getHome(player.getUUID(), index);
                if (latest != null && latest.name().equals(name)) {
                    ChillZoneHomes.store().deleteHome(player.getUUID(), index);
                    player.sendSystemMessage(Component.literal("Home deleted.").withStyle(ChatFormatting.GREEN));
                }
                open(player);
            }
            else if (response.clickedButtonId() == 1) openActions(player, index);
        }));
        send(player, form);
    }

    private static void iconMenu(ServerPlayer player, int index) {
        Home current = ChillZoneHomes.store().getHome(player.getUUID(), index);
        if (current == null) { open(player); return; }
        String[] icons = {"red_bed", "grass_block", "diamond_block", "oak_planks", "chest", "ender_chest", "nether_bricks", "end_stone", "beacon"};
        SimpleForm.Builder form = SimpleForm.builder().title("Home Icon").content("Choose a home icon.");
        for (String icon : icons) form.button(icon.replace('_', ' '));
        form.button("Back");
        form.validResultHandler(response -> onServer(player, () -> {
            int clicked = response.clickedButtonId();
            if (clicked >= 0 && clicked < icons.length) {
                Home latest = ChillZoneHomes.store().getHome(player.getUUID(), index);
                if (latest != null) ChillZoneHomes.store().setHome(player.getUUID(), index, latest.withIcon(icons[clicked]));
            }
            openActions(player, index);
        }));
        send(player, form);
    }

    private static final class QueuedForm {
        private final UUID id;
        private final SimpleForm.Builder form;
        private int waitTicks;
        private QueuedForm(UUID id, SimpleForm.Builder form, int waitTicks) {
            this.id = id;
            this.form = form;
            this.waitTicks = waitTicks;
        }
    }
}
