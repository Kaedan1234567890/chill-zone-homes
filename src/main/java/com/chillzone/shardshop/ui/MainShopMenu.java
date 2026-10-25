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

/** Main /shop menu: Deaths, Effect Bundles, Server Services. */
public final class MainShopMenu extends ChestMenu {
    private static final int ROWS=3, DEATHS=11, BUNDLES=13, SERVICES=15;
    private final ServerPlayer player;
    private MainShopMenu(int id, Inventory inv, ServerPlayer player){
        super(MenuType.GENERIC_9x3,id,inv,new SimpleContainer(ROWS*9),ROWS);this.player=player;refresh();
    }
    public static void open(ServerPlayer player){player.openMenu(new SimpleMenuProvider((id,inv,p)->new MainShopMenu(id,inv,player),Component.literal("Chill Zone — Shop")));}
    private void refresh(){
        ItemStack filler=Ui.button(Ui.item("gray_stained_glass_pane"),Component.empty());
        for(int i=0;i<27;i++)getContainer().setItem(i,filler.copy());
        getContainer().setItem(DEATHS,Ui.button(Ui.item("skeleton_skull"),Ui.name("Deaths",ChatFormatting.AQUA,ChatFormatting.BOLD),Ui.lore("View your recent deaths."),Ui.lore("Return to a saved death for 15 Shards.")));
        getContainer().setItem(BUNDLES,Ui.button(Ui.item("bundle"),Ui.name("Effect Bundles",ChatFormatting.LIGHT_PURPLE,ChatFormatting.BOLD),Ui.lore("Temporary themed perks and effects."),Ui.lore("Click to browse bundles.")));
        getContainer().setItem(SERVICES,Ui.button(Ui.item("anvil"),Ui.name("Server Services",ChatFormatting.YELLOW,ChatFormatting.BOLD),Ui.lore("Repair, remove curses, rename items, or recover XP.")));
    }
    @Override public void clicked(int slotId,int button,ContainerInput input,Player clicker){if(slotId==DEATHS)DeathMenu.open(player);else if(slotId==BUNDLES)EffectBundleMenu.open(player);else if(slotId==SERVICES)ServiceMenu.open(player);}
    @Override public ItemStack quickMoveStack(Player clicker,int slot){return ItemStack.EMPTY;}
    @Override public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot){return false;}
    @Override public boolean stillValid(Player clicker){return true;}
}
