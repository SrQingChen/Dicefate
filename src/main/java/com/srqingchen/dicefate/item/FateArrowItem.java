package com.srqingchen.dicefate.item;

import com.srqingchen.dicefate.entity.projectile.FateArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** 命矢物品：射出 FateArrow 实体（拼点 +1 总点数 +10% 伤害）。 */
public class FateArrowItem extends ArrowItem {

    public FateArrowItem(Properties props) {
        super(props);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack itemStack, LivingEntity owner, @Nullable ItemStack firedFromWeapon) {
        return new FateArrow(level, owner, itemStack.copyWithCount(1), firedFromWeapon);
    }
}
