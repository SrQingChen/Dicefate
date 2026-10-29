package com.srqingchen.dicefate.entity.projectile;

import com.srqingchen.dicefate.registry.ModEntities;
import com.srqingchen.dicefate.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import net.minecraft.world.level.Level;

/** 命矢：伤害略高于铁箭；拼点中 +1 总点数 +10% 伤害。 */
public class FateArrow extends Arrow {

    public FateArrow(EntityType<? extends FateArrow> type, Level level) {
        super(type, level);
    }

    public FateArrow(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon) {
        super(level, owner, ammo, weapon);
    }
}
