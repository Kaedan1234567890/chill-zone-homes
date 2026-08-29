package com.chillzone.homes.ui;

import com.chillzone.homes.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Set;

public class HomeActionsMenu extends ChestMenu {
    private static final int ROWS=3;
    private static final int TELEPORT=10, ICON=12, RENAME=14, DELETE=16, BACK=22;
    private static final String[] ICONS={"red_bed","blue_bed","lime_bed","ender_pearl","diamond","chest","beacon","nether_star","compass"};
    private final ServerPlayer player; private final int index;
    private HomeActionsMenu(int id, Inventory inv, ServerPlayer p, int index){
        super(MenuType.GENERIC_9x3,id,inv,new SimpleContainer(ROWS*9),ROWS); this.player=p;this.index=index;build();
    }
    public static void open(ServerPlayer p,int index){
        Home h=ChillZoneHomes.store().getHome(p.getUUID(),index); if(h==null){HomeListMenu.open(p,0);return;}
        p.openMenu(new SimpleMenuProvider((id,inv,x)->new HomeActionsMenu(id,inv,p,index),Component.literal(h.name())));
    }
    private void build(){
        ItemStack filler=Ui.button(Ui.item("gray_stained_glass_pane"),Component.empty()); for(int i=0;i<27;i++)getContainer().setItem(i,filler.copy());
        getContainer().setItem(TELEPORT,Ui.button(Items.ENDER_PEARL,Ui.name("Teleport",ChatFormatting.AQUA,ChatFormatting.BOLD),Ui.lore("Go to this home.")));
        getContainer().setItem(ICON,Ui.button(Items.ITEM_FRAME,Ui.name("Change Icon",ChatFormatting.LIGHT_PURPLE,ChatFormatting.BOLD),Ui.lore("Cycle through home icons.")));
        getContainer().setItem(RENAME,Ui.button(Items.NAME_TAG,Ui.name("Rename",ChatFormatting.YELLOW,ChatFormatting.BOLD),Ui.lore("Give this home a new name.")));
        getContainer().setItem(DELETE,Ui.button(Items.BARRIER,Ui.name("Delete",ChatFormatting.RED,ChatFormatting.BOLD),Ui.lore("Permanently remove this home.")));
        getContainer().setItem(BACK,Ui.button(Items.ARROW,Ui.name("Back",ChatFormatting.WHITE)));
    }
    @Override public void clicked(int slotId,int button,ContainerInput input,Player clicker){
        Home h=ChillZoneHomes.store().getHome(player.getUUID(),index); if(h==null){HomeListMenu.open(player,0);return;}
        switch(slotId){
            case TELEPORT -> teleport(h);
            case ICON -> changeIcon(h);
            case RENAME -> rename(h);
            case DELETE -> {ChillZoneHomes.store().deleteHome(player.getUUID(),index);player.sendSystemMessage(Component.literal("Home deleted.").withStyle(ChatFormatting.RED));HomeListMenu.open(player,0);}
            case BACK -> HomeListMenu.open(player,0);
        }
    }
    private void teleport(Home h){
        if(!ChillZoneHomes.config().allowCrossDimensionTeleport && !player.level().dimension().identifier().toString().equals(h.dimension())){
            player.sendSystemMessage(Component.literal("Cross-dimension home teleport is disabled.").withStyle(ChatFormatting.RED));return;
        }
        ServerLevel level=player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(h.dimension())));
        if(level==null){player.sendSystemMessage(Component.literal("That dimension no longer exists.").withStyle(ChatFormatting.RED));return;}
        player.closeContainer(); player.teleportTo(level,h.x(),h.y(),h.z(),Set.of(),h.yaw(),h.pitch(),false);
        level.playSound(null,BlockPos.containing(h.x(),h.y(),h.z()),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1f,1f);
        player.sendOverlayMessage(Component.literal("Welcome to "+h.name()+".").withStyle(ChatFormatting.AQUA));
    }
    private void rename(Home h){NameInputMenu.open(player,h.name(),name->{ChillZoneHomes.store().setHome(player.getUUID(),index,h.withName(name));HomeListMenu.open(player,0);});}
    private void changeIcon(Home h){
        String current=h.icon()==null?"red_bed":h.icon(); int pos=0;for(int i=0;i<ICONS.length;i++)if(ICONS[i].equals(current)){pos=i;break;}
        String next=ICONS[(pos+1)%ICONS.length]; ChillZoneHomes.store().setHome(player.getUUID(),index,h.withIcon(next)); player.sendSystemMessage(Component.literal("Home icon changed.").withStyle(ChatFormatting.LIGHT_PURPLE)); open(player,index);
    }
    @Override public ItemStack quickMoveStack(Player c,int s){return ItemStack.EMPTY;}
    @Override public boolean canTakeItemForPickAll(ItemStack stack,net.minecraft.world.inventory.Slot slot){return false;}
    @Override public boolean stillValid(Player c){return true;}
}
