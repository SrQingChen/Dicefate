package com.srqingchen.dicefate.item;

import com.srqingchen.dicefate.attachment.PlayerFateData;
import com.srqingchen.dicefate.network.DFNetwork;
import com.srqingchen.dicefate.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;


/**
 * 骰子包：右键使用 → 临时替换战斗骰池 N 次拼点。
 * 物品 id 与数据包 dicefate/pack 定义一一对应。
 */
public class PackItem extends Item {
    public PackItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        Identifier packId = Identifier.parse(this.builtInRegistryHolder().getRegisteredName());
        PlayerFateData data = sp.getData(ModAttachments.PLAYER_FATE);
        var regOpt = sp.level().registryAccess()
            .lookup(com.srqingchen.dicefate.data.DFRegistries.PACK);
        if (regOpt.isEmpty()) return InteractionResult.FAIL;
        var held = regOpt.get().get(packId);
        if (held.isEmpty()) return InteractionResult.FAIL;

        var def = held.get().value();
        int duration = def.duration() + data.nextPackBonus();
        data.setNextPackBonus(0);
        // 使用骰包清空定命（互斥）
        data.clearLock();
        data.setPack(packId, duration);
        stack.shrink(1);
        level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 1.2F);
        sp.sendOverlayMessage(Component.translatable("message.dicefate.pack_used", duration));
        DFNetwork.sendFateState(sp, data, sp.level().getGameTime());
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.dicefate.pack").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
}
