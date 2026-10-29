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

/** 霜寂女王（Tier 4）：每第3次拼点冰封玩家一个骰位2次；阶段2冰甲受伤-20%直到破势 */
public class FrostSilenceQueen extends BossEntity {
    public FrostSilenceQueen(EntityType<? extends Monster> type, Level level) {
        super(type, level, "BLUE");
    }

    @Override
    protected Identifier profileId() {
        return DiceFate.id("boss_frost_silence_queen");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BossEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 300.0)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ARMOR, 16.0)
            .add(Attributes.MOVEMENT_SPEED, 0.27);
    }
}
