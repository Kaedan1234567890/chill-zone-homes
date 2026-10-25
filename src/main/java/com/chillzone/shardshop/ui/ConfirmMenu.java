package com.chillzone.shardshop.ui;

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

public final class ConfirmMenu extends ChestMenu {
    private static final int CANCEL = 11, INFO = 13, CONFIRM = 15;
    private final ServerPlayer player;
    private final Runnable confirm;
    private final Runnable cancel;

    private ConfirmMenu(int id, Inventory inv, ServerPlayer player, String title, ItemStack info, Runnable confirm, Runnable cancel) {
        super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
        this.player = player;
        this.confirm = confirm;
        this.cancel = cancel;
        ItemStack filler = Ui.button(Ui.item("gray_stained_glass_pane"), Component.empty());
        for (int i = 0; i < 27; i++) getContainer().setItem(i, filler.copy());
        getContainer().setItem(CANCEL, Ui.button(Ui.item("red_wool"), Ui.name("Cancel", ChatFormatting.RED, ChatFormatting.BOLD)));
        getContainer().setItem(INFO, info);
        getContainer().setItem(CONFIRM, Ui.button(Ui.item("lime_wool"), Ui.name("Confirm", ChatFormatting.GREEN, ChatFormatting.BOLD)));
    }

    public static void open(ServerPlayer player, String title, ItemStack info, Runnable confirm, Runnable cancel) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ConfirmMenu(id, inv, player, title, info, confirm, cancel), Component.literal(title)));
    }

    /** Opens the normal confirmation menu, then decorates the green Confirm button with extra lore. */
    public static void open(ServerPlayer player, String title, ItemStack info, Component[] confirmLore, Runnable confirm, Runnable cancel) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> {
            ConfirmMenu menu = new ConfirmMenu(id, inv, player, title, info, confirm, cancel);
            menu.getContainer().setItem(CONFIRM, Ui.button(
                Ui.item("lime_wool"),
                Ui.name("Confirm", ChatFormatting.GREEN, ChatFormatting.BOLD),
                confirmLore
            ));
            return menu;
        }, Component.literal(title)));
    }

    @Override public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (slotId == CONFIRM) { player.closeContainer(); confirm.run(); }
        else if (slotId == CANCEL) { player.closeContainer(); if (cancel != null) cancel.run(); }
    }
    @Override public ItemStack quickMoveStack(Player clicker, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot) { return false; }
    @Override public boolean stillValid(Player clicker) { return true; }
}
