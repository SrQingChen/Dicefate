package com.srqingchen.dicefate.item;

import com.srqingchen.dicefate.attachment.PlayerFateData;
import com.srqingchen.dicefate.network.DFNetwork;
import com.srqingchen.dicefate.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 骰位扩容符 I–V：首次使用 +1 基础骰位，可乱序，同级重复无效。 */
public class SlotExpandItem extends Item {
    private final int level0;

    public SlotExpandItem(Properties props, int level0) {
        super(props);
        this.level0 = level0;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        PlayerFateData data = sp.getData(ModAttachments.PLAYER_FATE);
        if (data.hasUsedExpansion(level0)) {
            sp.sendOverlayMessage(Component.translatable("message.dicefate.expand_used"));
            return InteractionResult.FAIL;
        }
        if (data.useExpansion(level0)) {
            stack.shrink(1);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.0F);
            sp.sendOverlayMessage(Component.translatable("message.dicefate.expand_success", data.getBaseSlots()));
            DFNetwork.sendFateState(sp, data, sp.level().getGameTime());
            // 0.3.0 F1：扩容进度直授
            com.srqingchen.dicefate.util.AdvancementGrant.grant(sp, "expand_1");
            if (data.getBaseSlots() >= 4) com.srqingchen.dicefate.util.AdvancementGrant.grant(sp, "expand_3");
            if (data.getBaseSlots() >= 6) com.srqingchen.dicefate.util.AdvancementGrant.grant(sp, "expand_6");
            return InteractionResult.CONSUME;
        }
        sp.sendOverlayMessage(Component.translatable("message.dicefate.expand_full"));
        return InteractionResult.FAIL;
    }
}
