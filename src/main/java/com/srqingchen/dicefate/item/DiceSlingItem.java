package com.srqingchen.dicefate.item;

import com.srqingchen.dicefate.entity.projectile.DieProjectile;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

/** 骰投索：蓄力掷出骰弹（完整拼点的玩家有源伤害）。 */
public class DiceSlingItem extends Item {
    private final float maxDamage;

    public DiceSlingItem(Properties props, float maxDamage) {
        super(props);
        this.maxDamage = maxDamage;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide()) return false;
        int charge = this.getUseDuration(stack, entity) - timeLeft;
        float power = Math.min(1.0F, charge / 20.0F);
        if (power < 0.25F) return false;   // 未蓄满不发

        float damage = maxDamage * power;
        DieProjectile proj = new DieProjectile(level, entity, damage, firstDieFromInventory(entity));
        proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0.0F, power * 2.2F, 0.8F);
        level.addFreshEntity(proj);
        level.playSound(null, entity.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.7F, 1.3F);
        stack.hurtAndBreak(1, entity, getBrokenSlot(entity));
        return true;
    }

    /** 取玩家命盘/背包里第一颗骰作为弹射物视觉（纯展示，不消耗）。 */
    private static ItemStack firstDieFromInventory(LivingEntity entity) {
        if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
            var data = sp.getData(com.srqingchen.dicefate.registry.ModAttachments.PLAYER_FATE);
            for (int i = 0; i < data.getBaseSlots(); i++) {
                ItemStack s = data.pool().baseAt(i);
                if (s.getItem() instanceof DiceItem) return s.copy();
            }
        }
        if (entity instanceof net.minecraft.world.entity.player.Player p) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.getItem() instanceof DiceItem) return s.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private static net.minecraft.world.entity.EquipmentSlot getBrokenSlot(LivingEntity entity) {
        return entity.getUsedItemHand() == InteractionHand.OFF_HAND
            ? net.minecraft.world.entity.EquipmentSlot.OFFHAND
            : net.minecraft.world.entity.EquipmentSlot.MAINHAND;
    }
}
