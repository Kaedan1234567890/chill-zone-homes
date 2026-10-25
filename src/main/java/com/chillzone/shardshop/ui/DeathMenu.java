package com.chillzone.shardshop.ui;

import com.chillzone.shardshop.ChillZoneShardShop;
import com.chillzone.shardshop.DeathRecord;
import com.chillzone.shardshop.DeathTeleport;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Double-chest rolling history containing up to 28 saved deaths. */
public final class DeathMenu extends ChestMenu {
    // The 4x7 interior of a double chest = exactly 28 death slots.
    private static final int[] DEATH_SLOTS = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34,
        37,38,39,40,41,42,43
    };
    private static final int BACK = 49;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    private final ServerPlayer player;
    private List<DeathRecord> deaths;

    private DeathMenu(int id, Inventory inv, ServerPlayer player) {
        super(MenuType.GENERIC_9x6, id, inv, new SimpleContainer(54), 6);
        this.player = player;
        refresh();
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new DeathMenu(id, inv, player),
            Component.literal("Shard Shop — Deaths")
        ));
    }

    private void refresh() {
        // Only the outside edge is decorative glass. Empty death positions stay
        // visually empty until the player actually has a death saved there.
        ItemStack filler = Ui.button(Ui.item("gray_stained_glass_pane"), Component.empty());
        int[] border = {
            0,1,2,3,4,5,6,7,8,
            9,17,
            18,26,
            27,35,
            36,44,
            45,46,47,48,49,50,51,52,53
        };
        for (int slot : border) getContainer().setItem(slot, filler.copy());

        deaths = ChillZoneShardShop.deaths().get(player.getUUID());

        // A skeleton skull appears in a position only when that saved death
        // exists. As new deaths happen, records roll forward automatically.
        for (int i = 0; i < DEATH_SLOTS.length && i < deaths.size(); i++) {
            DeathRecord d = deaths.get(i);
            String label = i == 0 ? "Last Death" : "Death " + (i + 1);
            getContainer().setItem(DEATH_SLOTS[i], Ui.button(
                Ui.item("skeleton_skull"),
                Ui.name(label, ChatFormatting.AQUA, ChatFormatting.BOLD),
                Ui.lore("Dimension: " + prettyDimension(d.dimension())),
                Ui.lore(String.format(Locale.ROOT, "Death Location: %.1f, %.1f, %.1f", d.x(), d.y(), d.z())),
                Ui.lore("Time: " + TIME.format(Instant.ofEpochMilli(d.timestamp()))),
                Ui.lore("Cause: " + trim(d.cause(), 60)),
                Ui.lore("Cost: " + ChillZoneShardShop.config().deathTeleportCost + " Shards"),
                Ui.lore("Click to teleport back.")
            ));
        }

        getContainer().setItem(BACK, Ui.button(
            Ui.item("arrow"),
            Ui.name("Back", ChatFormatting.YELLOW)
        ));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (slotId == BACK) {
            MainShopMenu.open(player);
            return;
        }

        for (int i = 0; i < DEATH_SLOTS.length; i++) {
            if (slotId == DEATH_SLOTS[i] && i < deaths.size()) {
                DeathTeleport.teleport(player, deaths.get(i));
                return;
            }
        }
    }

    private static String prettyDimension(String d) {
        String p = d.contains(":") ? d.substring(d.indexOf(':') + 1) : d;
        return switch (p) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "The Nether";
            case "the_end" -> "The End";
            default -> p;
        };
    }

    private static String trim(String s, int max) {
        return s == null ? "Unknown" : (s.length() <= max ? s : s.substring(0, max - 3) + "...");
    }

    @Override public ItemStack quickMoveStack(Player clicker, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot) { return false; }
    @Override public boolean stillValid(Player clicker) { return true; }
}
