package com.srqingchen.dicefate.entity.projectile;

import com.srqingchen.dicefate.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 骰投索射出的骰子弹射物：完整拼点（玩家有源伤害），命中伤害 3/5 由 baseDamage 控制。
 * 0.2.0：实现 ItemSupplier —— 客户端 ThrownItemRenderer 渲染真实骰子物品模型
 * （替换原 ArrowRenderer+骰面贴图的错位拉伸）。
 */
public class DieProjectile extends AbstractArrow implements ItemSupplier {

    /** 渲染/拾取展示用的骰子物品（发射时记录，默认骨 d6）。 */
    private ItemStack dieStack = ItemStack.EMPTY;

    public DieProjectile(EntityType<? extends DieProjectile> type, Level level) {
        super(type, level);
    }

    public DieProjectile(Level level, LivingEntity owner, float damage) {
        this(level, owner, damage, ItemStack.EMPTY);
    }

    public DieProjectile(Level level, LivingEntity owner, float damage, ItemStack dieVisual) {
        super(ModEntities.DIE_PROJECTILE.get(), owner, level,
            new ItemStack(net.minecraft.world.item.Items.IRON_NUGGET), null);
        this.setBaseDamage(damage);
        this.pickup = Pickup.DISALLOWED;
        this.dieStack = dieVisual;
    }

    @Override
    public ItemStack getItem() {
        return dieStack;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!dieStack.isEmpty()) {
            output.store("DieItem", ItemStack.CODEC, dieStack);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        dieStack = input.read("DieItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
}
