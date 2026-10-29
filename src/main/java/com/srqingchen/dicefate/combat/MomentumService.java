package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.DFConfig;
import com.srqingchen.dicefate.attachment.PlayerFateData;
import net.minecraft.server.level.ServerPlayer;

/**
 * 骰势（连击，规划 3.2）：获取/扣除/层级/衰减。
 * 0.2.0：衰减修复——setMomentum 不再覆写 lastContestTick（markContest 显式标记），
 * 删除 decay 的 else 重置分支；5s -4/s、10s -8/s、30s 归零三档真正生效。
 */
public final class MomentumService {
    private MomentumService() {}

    /** 层级 0..4（0=无 I=25-49 II=50-74 III=75-99 IV=100）。 */
    public static int tier(int momentum) {
        if (momentum >= 100) return 4;
        if (momentum >= 75) return 3;
        if (momentum >= 50) return 2;
        if (momentum >= 25) return 1;
        return 0;
    }

    /** 层级伤害加成（入增伤桶）。 */
    public static double damageBonus(int momentum) {
        return switch (tier(momentum)) {
            case 2 -> 0.05;
            case 3, 4 -> 0.10;
            default -> 0;
        };
    }

    /** 层级总点数平加。 */
    public static int flatTotal(int momentum) {
        return switch (tier(momentum)) {
            case 2, 3 -> 1;
            case 4 -> 2;
            default -> 0;
        };
    }

    /** 层级命势加成。 */
    public static double poiseBonus(int momentum) {
        return tier(momentum) >= 1 ? 0.05 : 0;
    }

    /** 攻击结果骰势获取（赌徒护符 +10%）。 */
    public static int gainFor(Outcome o, boolean gamblerCharm) {
        int base = switch (o) {
            case NARROW_WIN -> 8;
            case WIN -> 10;
            case GREAT_WIN -> 14;
            case DESTINY -> 18;
            default -> 0;
        };
        if (base > 0 && gamblerCharm) base = (int) Math.floor(base * 1.10);
        return base;
    }

    /** 负 -30% / 崩 -50%（III 层免扣一次，8s CD）。 */
    public static void applyLoss(ServerPlayer player, PlayerFateData data, Outcome o, long tick) {
        if (o != Outcome.LOSE && o != Outcome.CRASH) return;
        if (data.getMomentum() <= 0) return;
        int cur = data.getMomentum();
        // 进入 III 层时获得免扣
        data.refreshMomentumFreeNegate(tick);
        if (tier(cur) == 3 && data.momentumFreeNegateReady(tick)) {
            data.useMomentumFreeNegate();
            data.setMomentumFreeNegateCd(tick + 8 * 20);
            data.setMomentum(cur, tick);
            return;
        }
        double ratio = o == Outcome.CRASH ? DFConfig.MOMENTUM_NULL_LOSS_RATIO.get() : DFConfig.MOMENTUM_LOSS_RATIO.get();
        int loss = (int) Math.floor(cur * ratio);
        data.setMomentum(cur - loss, tick);
    }

    /** 衰减（每秒结算一次；idle 从 markContest 起算，不再被衰减本身重置）。 */
    public static void decay(ServerPlayer player, PlayerFateData data, long tick) {
        if (data.getMomentum() <= 0) return;
        long idle = tick - data.lastContestTick();
        if (idle >= 30 * 20) {
            data.setMomentum(0, tick);
            return;
        }
        if (idle >= 10 * 20) {
            data.setMomentum(data.getMomentum() - 8, tick);
        } else if (idle >= 5 * 20) {
            data.setMomentum(data.getMomentum() - 4, tick);
        }
        // idle < 5s：不衰减、不重置基准
    }
}
