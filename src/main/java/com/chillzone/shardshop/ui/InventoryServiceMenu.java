package com.chillzone.shardshop.ui;

import com.chillzone.shardshop.*;
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

public final class InventoryServiceMenu extends ChestMenu {
    public enum Service { REPAIR, CURSE, RENAME }
    private static final int BACK=49;
    private final ServerPlayer player;
    private final Service service;
    private final int count;

    private InventoryServiceMenu(int id, Inventory inv, ServerPlayer player, Service service) {
        super(MenuType.GENERIC_9x6, id, inv, new SimpleContainer(54), 6);
        this.player=player; this.service=service;
        this.count=Math.min(45, player.getInventory().getContainerSize());
        refresh();
    }
    public static void open(ServerPlayer player, Service service) {
        player.openMenu(new SimpleMenuProvider((id,inv,p)->new InventoryServiceMenu(id,inv,player,service), Component.literal("Select Item — " + pretty(service))));
    }
    private void refresh() {
        for(int i=0;i<45;i++) getContainer().setItem(i, ItemStack.EMPTY);
        for(int i=0;i<count;i++) {
            ItemStack real=player.getInventory().getItem(i);
            if(!real.isEmpty()) getContainer().setItem(i, real.copy());
        }
        ItemStack filler=Ui.button(Ui.item("gray_stained_glass_pane"), Component.empty());
        for(int i=45;i<54;i++) getContainer().setItem(i,filler.copy());
        getContainer().setItem(BACK, Ui.button(Ui.item("arrow"), Ui.name("Back", ChatFormatting.YELLOW)));
    }
    @Override public void clicked(int slotId,int button,ContainerInput input,Player clicker) {
        if(slotId==BACK){ ServiceMenu.open(player); return; }
        if(slotId<0||slotId>=count) return;
        ItemStack real=player.getInventory().getItem(slotId);
        if(real.isEmpty()) return;
        String fp=ItemGuard.fingerprint(real);
        switch(service) {
            case REPAIR -> {
                int cost=RepairService.cost(real);
                if(cost<=0){player.sendSystemMessage(Component.literal("That item does not need repairing.").withStyle(ChatFormatting.YELLOW)); return;}
                ItemStack info=real.copy();
                ConfirmMenu.open(
                    player,
                    "Confirm Repair",
                    info,
                    new Component[]{
                        Component.literal("Repair this item to full durability.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Cost: " + cost + " Shards").withStyle(ChatFormatting.YELLOW),
                        Component.literal("Click to confirm purchase.").withStyle(ChatFormatting.GREEN)
                    },
                    ()->RepairService.repair(player,slotId,fp),
                    ()->open(player,service)
                );
            }
            case CURSE -> {
                int count=CurseService.curseCount(real);
                if(count<=0){player.sendSystemMessage(Component.literal("That item has no Curse of Binding or Vanishing.").withStyle(ChatFormatting.YELLOW)); return;}
                int cost=count*ChillZoneShardShop.config().removeCurseCostPerCurse;
                ItemStack info=real.copy();
                ConfirmMenu.open(player,"Confirm Curse Removal",info,()->CurseService.remove(player,slotId,fp),()->open(player,service));
            }
            case RENAME -> {
                ItemStack selected=real.copy();
                NameInputMenu.open(player, selected.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) ? selected.getHoverName().getString() : "", "Rename item", name -> RenameService.rename(player,slotId,fp,name));
            }
        }
    }
    private static String pretty(Service s){return switch(s){case REPAIR->"Repair";case CURSE->"Remove Curse";case RENAME->"Rename";};}
    @Override public ItemStack quickMoveStack(Player clicker,int slot){return ItemStack.EMPTY;}
    @Override public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot){return false;}
    @Override public boolean stillValid(Player clicker){return true;}
}
