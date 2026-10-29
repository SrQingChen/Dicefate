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

/** 双面赌徒（Tier 2）：每第3次拼点抛硬币：正面交换总点数，反面自身+5；阶段2获得第5骰并改为每2次 */
public class TwoFacedGambler extends BossEntity {
    public TwoFacedGambler(EntityType<? extends Monster> type, Level level) {
        super(type, level, "PURPLE");
    }

    @Override
    protected Identifier profileId() {
        return DiceFate.id("boss_two_faced_gambler");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BossEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 220.0)
            .add(Attributes.ATTACK_DAMAGE, 9.0)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3);
    }
}
