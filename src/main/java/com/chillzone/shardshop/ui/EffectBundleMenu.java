package com.chillzone.shardshop.ui;

import com.chillzone.shardshop.BundleService;
import com.chillzone.shardshop.BundleType;
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

import java.util.LinkedHashMap;
import java.util.Map;

public final class EffectBundleMenu extends ChestMenu {
    private final ServerPlayer player;
    private final Map<Integer, BundleType> slots = new LinkedHashMap<>();

    private EffectBundleMenu(int id, Inventory inv, ServerPlayer player) {
        super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
        this.player = player;
        refresh();
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new EffectBundleMenu(id, inv, player), Component.literal("Shop — Effect Bundles")));
    }

    private void refresh() {
        ItemStack filler = Ui.button(Ui.item("gray_stained_glass_pane"), Component.empty());
        for (int i = 0; i < 27; i++) getContainer().setItem(i, filler.copy());
        int[] menuSlots = {9,10,11,12,13,14,15,16,17};
        String[] icons = {"diamond_pickaxe","heart_of_the_sea","rabbit_foot","experience_bottle","netherite_sword","scaffolding","ender_pearl","blaze_powder","emerald_ore"};
        BundleType[] types = BundleType.values();
        for (int i = 0; i < types.length; i++) {
            BundleType b = types[i];
            int slot = menuSlots[i];
            slots.put(slot, b);
            String detail = detailText(b);
            if (detail == null) {
                getContainer().setItem(slot, Ui.button(Ui.item(icons[i]),
                    Ui.name(b.display, b == BundleType.WARRIORS_FURY ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    Ui.lore(b.description),
                    Ui.lore(effectText(b)),
                    Ui.lore("Duration: " + (b.seconds / 60) + " minutes"),
                    Ui.lore("Cost: " + b.cost() + " Shards"),
                    Ui.lore("Click to review purchase.")));
            } else {
                getContainer().setItem(slot, Ui.button(Ui.item(icons[i]),
                    Ui.name(b.display, b == BundleType.WARRIORS_FURY ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    Ui.lore(b.description),
                    Ui.lore(effectText(b)),
                    Ui.lore(detail),
                    Ui.lore("Duration: " + (b.seconds / 60) + " minutes"),
                    Ui.lore("Cost: " + b.cost() + " Shards"),
                    Ui.lore("Click to review purchase.")));
            }
        }
        getContainer().setItem(22, Ui.button(Ui.item("arrow"), Ui.name("Back", ChatFormatting.YELLOW)));
    }

    private static String effectText(BundleType b) {
        return switch (b) {
            case MINERS_FOCUS -> "Haste IV + Night Vision";
            case DEEP_DIVER -> "Dolphin's Grace + Conduit Power + Night Vision";
            case FORTUNES_FAVOR -> "Luck V";
            case SCHOLARS_BLESSING -> "1.5x XP earned";
            case WARRIORS_FURY -> "Strength III + Speed II + Fire Resistance";
            case BUILDERS_FOCUS -> "Haste II + Jump Boost II + Speed I + Night Vision";
            case VOID_WALKER -> "Slow Falling + Night Vision";
            case NETHER_WORKER -> "Fire Resistance + Night Vision";
            case PROSPECTOR -> "+2 effective Fortune, capped at Fortune V";
        };
    }

    private static String detailText(BundleType b) {
        return switch (b) {
            case FORTUNES_FAVOR -> "Vanilla Luck improves fishing treasure odds; it does not boost enchanting or Fortune.";
            case WARRIORS_FURY -> "High-power combat bundle intended for short PvP fights.";
            default -> null;
        };
    }

    @Override public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (slotId == 22) { MainShopMenu.open(player); return; }
        BundleType bundle = slots.get(slotId);
        if (bundle == null) return;
        ItemStack info = getContainer().getItem(slotId).copy();
        ConfirmMenu.open(player, "Confirm — " + bundle.display, info,
            () -> BundleService.purchase(player, bundle),
            () -> EffectBundleMenu.open(player));
    }
    @Override public ItemStack quickMoveStack(Player clicker, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot) { return false; }
    @Override public boolean stillValid(Player clicker) { return true; }
}
