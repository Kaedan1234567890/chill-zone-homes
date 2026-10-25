package com.chillzone.shardshop.ui;

import com.chillzone.shardshop.ChillZoneShardShop;
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

public final class ServiceMenu extends ChestMenu {
    private static final int REPAIR=10, CURSE=12, RENAME=14, XP=16, BACK=22;
    private final ServerPlayer player;
    private ServiceMenu(int id, Inventory inv, ServerPlayer player) {
        super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
        this.player = player;
        ItemStack filler = Ui.button(Ui.item("gray_stained_glass_pane"), Component.empty());
        for (int i=0;i<27;i++) getContainer().setItem(i, filler.copy());
        getContainer().setItem(REPAIR, Ui.button(Ui.item("anvil"), Ui.name("Item Repair", ChatFormatting.AQUA, ChatFormatting.BOLD), Ui.lore("Select a damaged item from a read-only inventory preview."), Ui.lore("Price scales with missing durability."), Ui.lore("Maximum: " + ChillZoneShardShop.config().repairMaxCost + " Shards")));
        getContainer().setItem(CURSE, Ui.button(Ui.item("grindstone"), Ui.name("Remove Curse", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), Ui.lore("Remove Binding/Vanishing curses."), Ui.lore("Cost: " + ChillZoneShardShop.config().removeCurseCostPerCurse + " Shards per curse.")));
        getContainer().setItem(RENAME, Ui.button(Ui.item("name_tag"), Ui.name("Item Rename", ChatFormatting.YELLOW, ChatFormatting.BOLD), Ui.lore("Rename one item."), Ui.lore("Java: anvil text entry."), Ui.lore("Bedrock: native naming form."), Ui.lore("Cost: " + ChillZoneShardShop.config().renameCost + " Shards")));
        getContainer().setItem(XP, Ui.button(Ui.item("experience_bottle"), Ui.name("XP Recovery", ChatFormatting.GREEN, ChatFormatting.BOLD), Ui.lore("Recover 50% of eligible XP from your most recent death."), Ui.lore("Can only be claimed once per death."), Ui.lore("Cost: " + ChillZoneShardShop.config().xpRecoveryCost + " Shards")));
        getContainer().setItem(BACK, Ui.button(Ui.item("arrow"), Ui.name("Back", ChatFormatting.WHITE)));
    }
    public static void open(ServerPlayer player) { player.openMenu(new SimpleMenuProvider((id,inv,p)->new ServiceMenu(id,inv,player), Component.literal("Shop — Server Services"))); }
    @Override public void clicked(int slotId,int button,ContainerInput input,Player clicker) {
        switch(slotId) {
            case REPAIR -> InventoryServiceMenu.open(player, InventoryServiceMenu.Service.REPAIR);
            case CURSE -> InventoryServiceMenu.open(player, InventoryServiceMenu.Service.CURSE);
            case RENAME -> InventoryServiceMenu.open(player, InventoryServiceMenu.Service.RENAME);
            case XP -> XpRecoveryMenu.open(player);
            case BACK -> MainShopMenu.open(player);
        }
    }
    @Override public ItemStack quickMoveStack(Player clicker,int slot){return ItemStack.EMPTY;}
    @Override public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot){return false;}
    @Override public boolean stillValid(Player clicker){return true;}
}
