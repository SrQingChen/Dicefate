package com.srqingchen.dicefate.item;

import com.srqingchen.dicefate.attachment.PlayerFateData;
import com.srqingchen.dicefate.registry.ModAttachments;
import com.srqingchen.dicefate.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;


/**
 * 一次性/杂项功能物品：专属槽印、洗命石、定命符、药剂、幸运饼干、命纹墨。
 */
public class MiscUseItem extends Item {

    public MiscUseItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        PlayerFateData data = sp.getData(ModAttachments.PLAYER_FATE);

        boolean success = false;
        if (this == ModItems.SIGIL_ADDITION.get() && !data.isAddSlotUnlocked()) {
            data.unlockAddSlot();
            success = true;
        } else if (this == ModItems.SIGIL_MULTIPLICATION.get() && !data.isMulSlotUnlocked()) {
            data.unlockMulSlot();
            success = true;
        } else if (this == ModItems.SIGIL_ECHO.get() && data.getSkillSlotsUnlocked() < 1) {
            data.unlockSkillSlot();
            success = true;
        } else if (this == ModItems.SIGIL_ECHO_2.get() && data.getSkillSlotsUnlocked() < 2) {
            data.unlockSkillSlot();
            success = true;
        } else if (this == ModItems.FATE_LOCK_TALISMAN.get() && !data.isLockUnlocked()) {
            data.unlockLock();
            success = true;
        } else if (this == ModItems.RESPEC_STONE.get()) {
            data.setPlayerRune(null);
            for (var st : com.srqingchen.dicefate.data.PoolProfile.Stance.values()) {
                data.setStanceMastery(st, 0);
            }
            success = true;
        } else if (this == ModItems.POTION_FATE_AIM.get()) {
            data.addAimCharges(3);
            success = true;
        } else if (this == ModItems.POTION_FATE_GUARD.get()) {
            data.addGuardCharges(3);
            success = true;
        } else if (this == ModItems.POTION_FATE_SIGHT.get()) {
            data.setSightTicks(3 * 60 * 20);
            // 给 32 格内敌对生物发光（命盘视界）
            if (level instanceof ServerLevel sl) {
                for (Mob mob : sl.getEntitiesOfClass(Monster.class, sp.getBoundingBox().inflate(32))) {
                    mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, 3 * 60 * 20, 0, true, false));
                }
            }
            success = true;
        } else if (this == ModItems.LUCKY_COOKIE.get()) {
            if (player.canEat(false)) {
                player.getFoodData().eat(2, 0.1F);
                data.setNextPackBonus(1);
                success = true;
            }
        }

        if (success) {
            stack.shrink(1);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
            return InteractionResult.CONSUME;
        }
        sp.sendOverlayMessage(Component.translatable("message.dicefate.use_fail"));
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> builder, TooltipFlag flag) {
        String key;
        if (this == ModItems.POTION_FATE_AIM.get()) key = "aim";
        else if (this == ModItems.POTION_FATE_GUARD.get()) key = "guard";
        else if (this == ModItems.POTION_FATE_SIGHT.get()) key = "sight";
        else if (this == ModItems.LUCKY_COOKIE.get()) key = "cookie";
        else if (this == ModItems.SIGIL_ADDITION.get()) key = "sigil_add";
        else if (this == ModItems.SIGIL_MULTIPLICATION.get()) key = "sigil_mul";
        else if (this == ModItems.SIGIL_ECHO.get()) key = "sigil_skill1";
        else if (this == ModItems.SIGIL_ECHO_2.get()) key = "sigil_skill2";
        else if (this == ModItems.FATE_LOCK_TALISMAN.get()) key = "lock_talisman";
        else if (this == ModItems.RESPEC_STONE.get()) key = "respec";
        else key = null;
        if (key != null) {
            builder.accept(Component.translatable("tooltip.dicefate.item." + key).withStyle(
                net.minecraft.ChatFormatting.DARK_AQUA));
        }
    }
}
