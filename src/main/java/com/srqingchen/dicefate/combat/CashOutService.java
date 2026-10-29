package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.DFConfig;
import com.srqingchen.dicefate.attachment.PlayerFateData;
import com.srqingchen.dicefate.network.DFNetwork;
import com.srqingchen.dicefate.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * 兑现（规划 3.2.5）：满骰势/1s 内满 → 武装 6s，下一次攻击 max vs 0。
 * 兑现窗口到期未命中 → 保命Buff；窗口内致命 → 图腾效果。
 */
public final class CashOutService {
    private CashOutService() {}

    /** V 键请求兑现。返回是否成功武装。 */
    public static boolean tryArm(ServerPlayer player, PlayerFateData data) {
        long tick = player.level().getGameTime();
        if (data.isCashoutArmed()) return false;
        boolean full = data.getMomentum() >= DFConfig.MOMENTUM_MAX.get();
        boolean recentFull = data.momentumPeakTick() > Long.MIN_VALUE
            && tick - data.momentumPeakTick() <= 20;
        if (!full && !recentFull) return false;

        int n = data.getMomentum();                     // 95..100
        int overflow = data.getMomentumOverflow();
        data.armCashout(n, tick + DFConfig.CASHOUT_WINDOW_TICKS.get());
        data.setMomentum(overflow, tick);               // 消耗后从溢出缓存继续
        data.consumeOverflow(overflow);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.playSound(null, player.blockPosition(),
                com.srqingchen.dicefate.registry.ModSounds.CASHOUT.value(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        DFNetwork.sendFateState(player, data, tick);
        return true;
    }

    /** 兑现窗口到期处理（未命中 → 生命恢复+抗性 6s）。 */
    public static void onExpire(ServerPlayer player, PlayerFateData data) {
        if (!data.isCashoutArmed()) return;
        data.disarmCashout();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0));
        DFNetwork.sendFateState(player, data, player.level().getGameTime());
    }

    /** 窗口内受到致命伤害：优先于原版图腾触发免死。 */
    public static boolean onFatal(ServerPlayer player, PlayerFateData data) {
        if (!data.isCashoutArmed()) return false;
        data.disarmCashout();
        player.setHealth(1.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        DFNetwork.sendFateState(player, data, player.level().getGameTime());
        return true;
    }
}
