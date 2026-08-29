package com.chillzone.homes.ui;

import com.chillzone.homes.*;
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
import java.util.List;
import java.util.Locale;

/** Donut-inspired list with visible locked slots and paging. */
public class HomeListMenu extends ChestMenu {
    private static final int ROWS = 6;
    private static final int[] GRID = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31
    };
    private static final int BACK = 45, PAGE_INFO = 49, NEXT = 53;
    private final ServerPlayer player;
    private final int page;

    private HomeListMenu(int id, Inventory inv, ServerPlayer player, int page) {
        super(MenuType.GENERIC_9x6, id, inv, new SimpleContainer(ROWS * 9), ROWS);
        this.player = player; this.page = Math.max(0, page); refresh();
    }
    public static void open(ServerPlayer player, int page) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new HomeListMenu(id, inv, player, page), Component.literal("Chill Zone — Homes")));
    }
    private void refresh() {
        ItemStack filler = Ui.button(Ui.item("gray_stained_glass_pane"), Component.empty());
        for (int i=0;i<ROWS*9;i++) getContainer().setItem(i, filler.copy());
        int limit = HomeLimitResolver.resolve(player);
        int max = ChillZoneHomes.config().maximumVisibleSlots;
        List<Home> homes = ChillZoneHomes.store().getHomes(player.getUUID());
        int start = page * GRID.length;
        for (int i=0;i<GRID.length;i++) {
            int absolute = start + i;
            if (absolute >= max) continue;
            ItemStack button;
            Home home = absolute < homes.size() ? homes.get(absolute) : null;
            if (absolute >= limit) {
                button = Ui.button(Ui.item("barrier"), Ui.name("Locked", ChatFormatting.RED, ChatFormatting.BOLD),
                    Ui.lore("This home slot is locked."), Ui.lore("Your limit: " + limit + " homes"));
            } else if (home == null) {
                button = Ui.button(Ui.item("lime_bed"), Ui.name("New Home", ChatFormatting.GREEN, ChatFormatting.BOLD),
                    Ui.lore("Click to create Home " + (absolute+1)), Ui.lore("at your current location."));
            } else {
                button = Ui.button(Ui.item(home.icon() == null || home.icon().isBlank() ? "red_bed" : home.icon()),
                    Ui.name(home.name(), ChatFormatting.AQUA, ChatFormatting.BOLD),
                    Ui.lore(String.format(Locale.ROOT,"%.0f, %.0f, %.0f",home.x(),home.y(),home.z())),
                    Ui.lore(prettyDimension(home.dimension())), Component.empty(), Ui.lore("Click to manage."));
            }
            getContainer().setItem(GRID[i], button);
        }
        int used = homes.size();
        getContainer().setItem(PAGE_INFO, Ui.button(Ui.item("book"), Ui.name("Homes ("+used+"/"+limit+")", ChatFormatting.YELLOW, ChatFormatting.BOLD), Ui.lore("LuckPerms meta: homes-max")));
        if (page > 0) getContainer().setItem(BACK, Ui.button(Ui.item("arrow"), Ui.name("Previous", ChatFormatting.WHITE)));
        if ((page+1)*GRID.length < max) getContainer().setItem(NEXT, Ui.button(Ui.item("spectral_arrow"), Ui.name("Show More", ChatFormatting.GREEN, ChatFormatting.BOLD), Ui.lore("View more home slots.")));
    }
    private static String prettyDimension(String d) {
        String p = d.contains(":") ? d.substring(d.indexOf(':')+1) : d;
        return switch(p){ case "overworld"->"Overworld"; case "the_nether"->"The Nether"; case "the_end"->"The End"; default->p; };
    }
    @Override public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (slotId == BACK && page>0) { open(player,page-1); return; }
        if (slotId == NEXT) { open(player,page+1); return; }
        int limit = HomeLimitResolver.resolve(player);
        int start = page * GRID.length;
        for (int i=0;i<GRID.length;i++) if (slotId==GRID[i]) {
            int absolute = start+i;
            if (absolute >= limit) { player.sendSystemMessage(Component.literal("That home slot is locked.").withStyle(ChatFormatting.RED)); return; }
            List<Home> homes = ChillZoneHomes.store().getHomes(player.getUUID());
            if (absolute < homes.size()) { HomeActionsMenu.open(player, absolute); return; }
            if (absolute != homes.size()) { player.sendSystemMessage(Component.literal("Create earlier home slots first.").withStyle(ChatFormatting.YELLOW)); return; }
            NameInputMenu.open(player,"Home "+(absolute+1),name->{
                Home h = new Home(name, player.level().dimension().identifier().toString(), player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot(),"red_bed");
                ChillZoneHomes.store().addHome(player.getUUID(),h);
                player.sendSystemMessage(Component.literal("Home \""+name+"\" set.").withStyle(ChatFormatting.GREEN));
                open(player,page);
            });
            return;
        }
    }
    @Override public ItemStack quickMoveStack(Player clicker,int slot){return ItemStack.EMPTY;}
    @Override public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot){return false;}
    @Override public boolean stillValid(Player clicker){return true;}
}
