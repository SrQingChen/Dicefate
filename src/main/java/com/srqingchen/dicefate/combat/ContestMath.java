package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.DFConfig;
import com.srqingchen.dicefate.data.PoolProfile;

/**
 * 拼点数学：差率、战果分类、姿态曲线、穿透（规划 02/11 全部基线公式）。
 */
public final class ContestMath {
    private ContestMath() {}

    /** r = (A-D)/D；D=0 且 A>0 视为 1.0；双方 0 → 0。 */
    public static double ratio(int a, int d) {
        if (d <= 0) return a > 0 ? 1.0 : 0.0;
        return (double) (a - d) / d;
    }

    public static Outcome classify(int a, int d) {
        double missThreshold = DFConfig.missThreshold();
        if (d <= 0) {
            if (a > 0) return Outcome.DESTINY;   // 命气散竭的防御方
            return Outcome.TIE;                  // 双方无骰
        }
        if (a < missThreshold * d) return Outcome.CRASH;
        if (a == d) return Outcome.TIE;
        double r = ratio(a, d);
        if (r < 0) return Outcome.LOSE;
        if (r < 0.25) return Outcome.NARROW_WIN;
        if (r < 0.5) return Outcome.WIN;
        if (r < 1.0) return Outcome.GREAT_WIN;
        return Outcome.DESTINY;
    }

    // ==================== 攻击曲线 C(r) ====================

    /** 均衡：r≥0 min(1.65, 1+0.55r)；r<0 max(0.50, 0.90+0.80r)。 */
    public static double balancedAttack(double r) {
        if (r == 0) return DFConfig.tieCoefficient();
        if (r > 0) return Math.min(1.65, 1.0 + 0.55 * r);
        return Math.max(0.50, 0.90 + 0.80 * r);
    }

    /** 背水：clamp(1+1.80×r×|r|, 0.30, 2.00)。 */
    public static double backwaterAttack(double r) {
        if (r == 0) return DFConfig.tieCoefficient();
        return clamp(1.0 + 1.80 * r * Math.abs(r), 0.30, 2.00);
    }

    /** 守势：clamp(0.90×均衡, 0.30, 1.50)。 */
    public static double guardAttack(double r) {
        if (r == 0) return DFConfig.tieCoefficient();
        return clamp(0.90 * balancedAttack(r), 0.30, 1.50);
    }

    /** 诡道：与均衡同曲线（其价值在"败局改写"与完美格挡阈值）。 */
    public static double trickyAttack(double r) { return balancedAttack(r); }

    public static double attackCurve(PoolProfile.Stance stance, double r) {
        return switch (stance) {
            case BACKWATER -> backwaterAttack(r);
            case GUARD -> guardAttack(r);
            case TRICKY -> trickyAttack(r);
            default -> balancedAttack(r);
        };
    }

    /** 姿态精通：把曲线偏离 1.0 的部分放大 5%/级。 */
    public static double withMastery(double curve, int masteryLevel) {
        return 1.0 + (curve - 1.0) * (1.0 + 0.05 * masteryLevel);
    }

    // ==================== 防御曲线 T(q) ====================

    /** 均衡防御。 */
    public static double balancedDefense(double q) {
        if (q == 0) return 0.85;
        if (q > 0) return Math.max(0.45, 1.0 - 0.55 * q);
        return Math.min(1.40, 1.0 + 0.80 * Math.abs(q));
    }

    /** 守势防御。 */
    public static double guardDefense(double q) {
        if (q == 0) return 0.85;
        if (q > 0) return Math.max(0.25, 1.0 - 0.75 * q);
        return Math.min(1.30, 1.0 + 0.60 * Math.abs(q));
    }

    /** 背水防御。 */
    public static double backwaterDefense(double q) {
        if (q == 0) return 0.85;
        if (q > 0) return Math.max(0.65, 1.0 - 0.35 * q);
        return Math.min(1.55, 1.0 + 1.10 * Math.abs(q));
    }

    public static double defenseCurve(PoolProfile.Stance stance, double q) {
        return switch (stance) {
            case GUARD -> guardDefense(q);
            case BACKWATER -> backwaterDefense(q);
            default -> balancedDefense(q);
        };
    }

    /** 完美格挡阈值：均衡/诡道 0.75；守势 0.50；背水不可能。 */
    public static double perfectBlockThreshold(PoolProfile.Stance stance) {
        return switch (stance) {
            case GUARD -> 0.50;
            case BACKWATER -> Double.MAX_VALUE;   // 不可
            default -> 0.75;
        };
    }

    // ==================== 穿透 ====================

    /** pen = clamp(0.15 + 0.45×min(r,1), 0, 0.60)；平 10%；负/崩 0。 */
    public static double penetration(double r, Outcome o) {
        if (o == Outcome.TIE) return 0.10;
        if (!o.isWin()) return 0.0;
        return clamp(0.15 + 0.45 * Math.min(r, 1.0), 0.0, 0.60);
    }

    public static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static int floorInt(double v) { return (int) Math.floor(v); }
}
