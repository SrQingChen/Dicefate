package com.srqingchen.dicefate.entity.boss;

import com.srqingchen.dicefate.DiceFate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import com.srqingchen.dicefate.registry.ModEntities;

/** 零面者（Tier 5）：三阶段：零面(玩家最高骰归0)/万面(wild骰+第7骰)/归零(玩家全骰最小) */
public class Nullface extends BossEntity {
    public Nullface(EntityType<? extends Monster> type, Level level) {
        super(type, level, "WHITE");
    }

    @Override
    protected Identifier profileId() {
        return DiceFate.id("boss_nullface");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BossEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 520.0)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 18.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3);
    }
}
