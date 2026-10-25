package com.chillzone.shardshop.ui;

import com.chillzone.shardshop.XpRecoveryService;
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

public final class XpRecoveryMenu extends ChestMenu {
    private static final int CONFIRM=11, INFO=13, CANCEL=15;
    private final ServerPlayer player;
    private final XpRecoveryService.Quote quote;
    private XpRecoveryMenu(int id, Inventory inv, ServerPlayer player) {
        super(MenuType.GENERIC_9x3,id,inv,new SimpleContainer(27),3);
        this.player=player; this.quote=XpRecoveryService.quote(player);
        ItemStack filler=Ui.button(Ui.item("gray_stained_glass_pane"), Component.empty());
        for(int i=0;i<27;i++)getContainer().setItem(i,filler.copy());
        getContainer().setItem(CANCEL,Ui.button(Ui.item("red_wool"),Ui.name("Cancel",ChatFormatting.RED,ChatFormatting.BOLD)));
        if(quote.available()) {
            getContainer().setItem(CONFIRM,Ui.button(Ui.item("lime_wool"),Ui.name("Recover XP",ChatFormatting.GREEN,ChatFormatting.BOLD),Ui.lore("Cost: "+quote.cost()+" Shards")));
            getContainer().setItem(INFO,Ui.button(Ui.item("experience_bottle"),Ui.name("XP Recovery",ChatFormatting.AQUA,ChatFormatting.BOLD),Ui.lore("Recoverable XP: "+quote.recoverable()),Ui.lore("Most recent death only."),Ui.lore("One claim per death.")));
        } else {
            getContainer().setItem(INFO,Ui.button(Ui.item("barrier"),Ui.name("XP Recovery Unavailable",ChatFormatting.RED,ChatFormatting.BOLD),Ui.lore(quote.reason())));
        }
    }
    public static void open(ServerPlayer player){player.openMenu(new SimpleMenuProvider((id,inv,p)->new XpRecoveryMenu(id,inv,player),Component.literal("Shop — XP Recovery")));}
    @Override public void clicked(int slotId,int button,ContainerInput input,Player clicker){
        if(slotId==CANCEL){ServiceMenu.open(player);return;}
        if(slotId==CONFIRM&&quote.available()){player.closeContainer();XpRecoveryService.purchase(player,quote);}
    }
    @Override public ItemStack quickMoveStack(Player clicker,int slot){return ItemStack.EMPTY;}
    @Override public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot){return false;}
    @Override public boolean stillValid(Player clicker){return true;}
}
