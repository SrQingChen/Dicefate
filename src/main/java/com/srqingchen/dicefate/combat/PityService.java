package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.DFConfig;
import com.srqingchen.dicefate.attachment.PlayerFateData;
import net.minecraft.util.RandomSource;

/** 保底/命佑（规划 3.4）：隐藏层数 0-5，攻击拼点取高。 */
public final class PityService {
    private PityService() {}

    /** 取高概率（Boss/精英减半）。 */
    public static double chance(PlayerFateData data, boolean bossOrElite) {
        double p = data.pityStacks() * DFConfig.PITY_STEP.get();
        // 命途·命佑：每层 +1%
        if (data.playerRune() != null && data.playerRune().getPath().equals("rune_pity")) {
            p += data.pityStacks() * 0.01;
        }
        if (bossOrElite) p *= DFConfig.PITY_BOSS_MULTIPLIER.get();
        return Math.min(p, DFConfig.PITY_CAP.get());
    }

    /** 掷两次取更高总点（用于攻击骰）。 */
    public static boolean shouldCoRoll(PlayerFateData data, boolean bossOrElite, RandomSource rng) {
        double c = chance(data, bossOrElite);
        return c > 0 && rng.nextDouble() < c;
    }

    /** 攻击拼点结果更新层数：胜清空；平 -1；负/崩 +1。 */
    public static void onAttackResult(PlayerFateData data, Outcome o) {
        switch (o) {
            case NARROW_WIN, WIN, GREAT_WIN, DESTINY -> data.setPityStacks(0);
            case TIE -> data.setPityStacks(data.pityStacks() - 1);
            case LOSE, CRASH -> data.setPityStacks(data.pityStacks() + 1);
        }
    }
}
