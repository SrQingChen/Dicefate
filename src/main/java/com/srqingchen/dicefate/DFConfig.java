package com.srqingchen.dicefate;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 服务端权威配置（规划 3.11/7.6）。所有数值可在此覆盖基线。
 */
public final class DFConfig {
    public static final ModConfigSpec SPEC;

    // —— 拼点 ——
    public static final ModConfigSpec.DoubleValue MISS_THRESHOLD;      // A < missThreshold*D 视为崩
    public static final ModConfigSpec.DoubleValue TIE_COEFFICIENT;     // 平局伤害系数
    public static final ModConfigSpec.DoubleValue MAX_DAMAGE_CAP;      // 基础伤害倍率上限
    public static final ModConfigSpec.IntValue LUCK_CAP;               // 幸运/霉运修正上限
    public static final ModConfigSpec.BooleanValue LIGHT_CONTEST_FOR_MOB_RANGED;
    public static final ModConfigSpec.DoubleValue ADD_BUCKET_CAP;      // 增伤桶上限
    public static final ModConfigSpec.DoubleValue REDUCE_BUCKET_CAP;   // 减伤桶上限
    public static final ModConfigSpec.DoubleValue VULN_CAP;            // 易伤上限

    // —— 命势 ——
    public static final ModConfigSpec.DoubleValue POISE_NORMAL_MULT;
    public static final ModConfigSpec.DoubleValue POISE_NORMAL_FLAT;
    public static final ModConfigSpec.DoubleValue POISE_ELITE_MULT;
    public static final ModConfigSpec.DoubleValue POISE_ELITE_FLAT;
    public static final ModConfigSpec.DoubleValue POISE_BOSS_MULT;
    public static final ModConfigSpec.IntValue BREAK_SECONDS;
    public static final ModConfigSpec.IntValue BOSS_BREAK_SECONDS;
    public static final ModConfigSpec.IntValue RECOVERY_SECONDS;
    public static final ModConfigSpec.IntValue BOSS_RECOVERY_SECONDS;
    public static final ModConfigSpec.IntValue FATEBREAK_THRESHOLD_NORMAL;
    public static final ModConfigSpec.IntValue FATEBREAK_THRESHOLD_ELITE;
    public static final ModConfigSpec.IntValue FATEBREAK_THRESHOLD_BOSS;
    public static final ModConfigSpec.DoubleValue FATEBREAK_PCT_NORMAL;
    public static final ModConfigSpec.DoubleValue FATEBREAK_PCT_ELITE;
    public static final ModConfigSpec.DoubleValue FATEBREAK_PCT_BOSS;
    public static final ModConfigSpec.DoubleValue POISE_MAX_GAIN_PER_BREAK;
    public static final ModConfigSpec.DoubleValue POISE_MAX_GAIN_CAP;
    public static final ModConfigSpec.DoubleValue DECREE_THRESHOLD_GAIN;
    public static final ModConfigSpec.DoubleValue DECREE_THRESHOLD_CAP;
    public static final ModConfigSpec.DoubleValue WOUND_BONUS;         // 命损易伤
    public static final ModConfigSpec.IntValue WOUND_EXTRA_SECONDS;

    // —— 骰势 ——
    public static final ModConfigSpec.IntValue MOMENTUM_MAX;
    public static final ModConfigSpec.DoubleValue MOMENTUM_LOSS_RATIO;
    public static final ModConfigSpec.DoubleValue MOMENTUM_NULL_LOSS_RATIO;
    public static final ModConfigSpec.IntValue MOMENTUM_OVERFLOW_CACHE; // 溢出缓存
    public static final ModConfigSpec.IntValue CASHOUT_WINDOW_TICKS;    // 兑现窗口 6s
    public static final ModConfigSpec.DoubleValue CASHOUT_DAMAGE_MULT;
    public static final ModConfigSpec.DoubleValue CASHOUT_WOUND_DAMAGE_MULT;

    // —— 保底 ——
    public static final ModConfigSpec.DoubleValue PITY_STEP;
    public static final ModConfigSpec.DoubleValue PITY_CAP;
    public static final ModConfigSpec.DoubleValue PITY_BOSS_MULTIPLIER;

    // —— 技能骰 ——
    public static final ModConfigSpec.IntValue SKILL_CD_MIN_TICKS;

    // —— 骰子包 ——
    public static final ModConfigSpec.BooleanValue PACK_BOSS_PRESSURE;

    // —— 骰灵 ——
    public static final ModConfigSpec.IntValue SPIRIT_FEED_PER_USE;

    // —— 经济 ——
    public static final ModConfigSpec.IntValue APPRAISAL_COST_SAND;
    public static final ModConfigSpec.IntValue REROLL_MAX;
    public static final ModConfigSpec.IntValue REPAIR_COST_PER_20_BASE;
    public static final ModConfigSpec.DoubleValue SELL_MULTIPLIER;

    // —— 战斗杂项 ——
    public static final ModConfigSpec.IntValue CONTEST_HUD_RANGE;      // 旁观者接收半径
    public static final ModConfigSpec.BooleanValue HUD_SHAKE;          // 0.2.0 落定震屏（可访问性）
    public static final ModConfigSpec.BooleanValue DEBUG_VERBOSE;       // 0.3.2 全管线诊断日志

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("contest");
        MISS_THRESHOLD = b.defineInRange("missThreshold", 0.5, 0.2, 0.9);
        TIE_COEFFICIENT = b.defineInRange("tieCoefficient", 0.90, 0.5, 1.2);
        MAX_DAMAGE_CAP = b.defineInRange("maxDamageCap", 4.0, 2.0, 8.0);
        LUCK_CAP = b.defineInRange("luckCap", 2, 0, 5);
        LIGHT_CONTEST_FOR_MOB_RANGED = b.define("lightContestForMobRanged", true);
        ADD_BUCKET_CAP = b.defineInRange("additiveBucketCap", 1.0, 0.25, 3.0);
        REDUCE_BUCKET_CAP = b.defineInRange("reduceBucketCap", 0.6, 0.1, 0.9);
        VULN_CAP = b.defineInRange("vulnerabilityCap", 0.30, 0.05, 1.0);
        b.pop();

        b.push("poise");
        POISE_NORMAL_MULT = b.defineInRange("normalMultiplier", 0.55, 0.3, 1.0);
        POISE_NORMAL_FLAT = b.defineInRange("normalFlat", 2.0, 0.0, 20.0);
        POISE_ELITE_MULT = b.defineInRange("eliteMultiplier", 0.65, 0.3, 1.2);
        POISE_ELITE_FLAT = b.defineInRange("eliteFlat", 5.0, 0.0, 30.0);
        POISE_BOSS_MULT = b.defineInRange("bossMultiplier", 0.55, 0.3, 1.2);
        BREAK_SECONDS = b.defineInRange("breakSeconds", 10, 4, 20);
        BOSS_BREAK_SECONDS = b.defineInRange("bossBreakSeconds", 8, 4, 15);
        RECOVERY_SECONDS = b.defineInRange("recoverySeconds", 12, 6, 30);
        BOSS_RECOVERY_SECONDS = b.defineInRange("bossRecoverySeconds", 15, 8, 40);
        FATEBREAK_THRESHOLD_NORMAL = b.defineInRange("fatebreakThresholdNormal", 100, 50, 300);
        FATEBREAK_THRESHOLD_ELITE = b.defineInRange("fatebreakThresholdElite", 90, 50, 300);
        FATEBREAK_THRESHOLD_BOSS = b.defineInRange("fatebreakThresholdBoss", 120, 60, 400);
        FATEBREAK_PCT_NORMAL = b.defineInRange("fatebreakPctNormal", 0.08, 0.03, 0.15);
        FATEBREAK_PCT_ELITE = b.defineInRange("fatebreakPctElite", 0.10, 0.03, 0.15);
        FATEBREAK_PCT_BOSS = b.defineInRange("fatebreakPctBoss", 0.06, 0.03, 0.15);
        POISE_MAX_GAIN_PER_BREAK = b.defineInRange("maxGainPerBreak", 0.05, 0.0, 0.15);
        POISE_MAX_GAIN_CAP = b.defineInRange("maxGainCap", 0.50, 0.0, 1.0);
        DECREE_THRESHOLD_GAIN = b.defineInRange("decreeThresholdGain", 0.10, 0.0, 0.3);
        DECREE_THRESHOLD_CAP = b.defineInRange("decreeThresholdCap", 0.50, 0.0, 1.0);
        WOUND_BONUS = b.defineInRange("woundBonus", 0.15, 0.0, 0.5);
        WOUND_EXTRA_SECONDS = b.defineInRange("woundExtraSeconds", 3, 0, 10);
        b.pop();

        b.push("momentum");
        MOMENTUM_MAX = b.defineInRange("max", 100, 50, 200);
        MOMENTUM_LOSS_RATIO = b.defineInRange("lossRatio", 0.30, 0.0, 1.0);
        MOMENTUM_NULL_LOSS_RATIO = b.defineInRange("nullLossRatio", 0.50, 0.0, 1.0);
        MOMENTUM_OVERFLOW_CACHE = b.defineInRange("overflowCache", 20, 0, 100);
        CASHOUT_WINDOW_TICKS = b.defineInRange("cashoutWindowTicks", 120, 20, 400);
        CASHOUT_DAMAGE_MULT = b.defineInRange("cashoutDamageMult", 1.1, 1.0, 2.0);
        CASHOUT_WOUND_DAMAGE_MULT = b.defineInRange("cashoutWoundDamageMult", 1.2, 1.0, 2.0);
        b.pop();

        b.push("pity");
        PITY_STEP = b.defineInRange("step", 0.08, 0.0, 0.2);
        PITY_CAP = b.defineInRange("cap", 0.40, 0.0, 0.8);
        PITY_BOSS_MULTIPLIER = b.defineInRange("bossMultiplier", 0.5, 0.0, 1.0);
        b.pop();

        b.push("skill");
        SKILL_CD_MIN_TICKS = b.defineInRange("minCdTicks", 40, 20, 200);
        b.pop();

        b.push("pack");
        PACK_BOSS_PRESSURE = b.define("bossPressure", true);
        b.pop();

        b.push("spirit");
        SPIRIT_FEED_PER_USE = b.defineInRange("feedPerUse", 1, 1, 3);
        b.pop();

        b.push("economy");
        APPRAISAL_COST_SAND = b.defineInRange("appraisalCostSand", 10, 0, 100);
        REROLL_MAX = b.defineInRange("rerollMax", 2, 0, 5);
        REPAIR_COST_PER_20_BASE = b.defineInRange("repairCostPer20Base", 10, 0, 100);
        SELL_MULTIPLIER = b.defineInRange("sellMultiplier", 0.30, 0.05, 1.0);
        b.pop();

        b.push("misc");
        CONTEST_HUD_RANGE = b.defineInRange("hudRange", 32, 8, 128);
        HUD_SHAKE = b.define("hudShake", true);
        DEBUG_VERBOSE = b.define("debugVerbose", true);
        b.pop();

        SPEC = b.build();
    }


    /** 0.3.0：未加载 config（纯 JVM 冒烟测试等）时返回注册默认值。 */
    private static double orDefault(ModConfigSpec.DoubleValue v, double def) {
        try {
            return v.get();
        } catch (IllegalStateException e) {
            return def;
        }
    }

    public static double missThreshold() {
        return orDefault(MISS_THRESHOLD, 0.5);
    }

    public static double tieCoefficient() {
        return orDefault(TIE_COEFFICIENT, 0.90);
    }

    private DFConfig() {}
}
