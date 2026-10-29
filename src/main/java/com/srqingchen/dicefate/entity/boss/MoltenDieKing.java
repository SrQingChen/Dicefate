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

/** 熔面骰王（Tier 3）：4格火焰光环；每第4次拼点随机一颗玩家骰视为1；阶段2第6骰+光环扩大 */
public class MoltenDieKing extends BossEntity {
    public MoltenDieKing(EntityType<? extends Monster> type, Level level) {
        super(type, level, "RED");
    }

    @Override
    protected Identifier profileId() {
        return DiceFate.id("boss_molten_die_king");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BossEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 340.0)
            .add(Attributes.ATTACK_DAMAGE, 12.0)
            .add(Attributes.ARMOR, 14.0)
            .add(Attributes.MOVEMENT_SPEED, 0.26);
    }
}
