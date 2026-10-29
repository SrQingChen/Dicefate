package com.srqingchen.dicefate.item;

import com.srqingchen.dicefate.compat.CuriosCompat;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 骰袋（0.2.0）：右键打开 6 格备用骰储存界面。
 * 需佩戴（Curios belt 槽）或携带在背包；否则提示。
 */
public class PouchItem extends Item {

    public PouchItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!CuriosCompat.hasCurio(player, this)) {
            player.sendSystemMessage(Component.translatable("message.dicefate.pouch_not_equipped"));
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            com.srqingchen.dicefate.client.screen.PouchScreen.open();
        }
        return InteractionResult.SUCCESS;
    }

    /** 服务端判定（防客户端绕过）。 */
    public static boolean canUse(ServerPlayer player) {
        return CuriosCompat.hasCurio(player, com.srqingchen.dicefate.registry.GearItems.DICE_POUCH.get());
    }
}
