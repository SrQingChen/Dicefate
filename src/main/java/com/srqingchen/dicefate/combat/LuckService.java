package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.DFConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** 幸运/霉运修正（规划 2.5.5）：luckShift = clamp(luck-unluck, -2, 2)，加到总点数。 */
public final class LuckService {
    private LuckService() {}

    public static int shift(LivingEntity entity) {
        int luck = 0, unluck = 0;
        MobEffectInstance l = entity.getEffect(MobEffects.LUCK);
        MobEffectInstance u = entity.getEffect(MobEffects.UNLUCK);
        if (l != null) luck = l.getAmplifier() + 1;
        if (u != null) unluck = u.getAmplifier() + 1;
        int cap = DFConfig.LUCK_CAP.get();
        return Math.max(-cap, Math.min(cap, luck - unluck));
    }
}
