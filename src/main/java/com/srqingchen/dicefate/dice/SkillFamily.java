package com.srqingchen.dicefate.dice;

import net.minecraft.util.StringRepresentable;

/**
 * 技能骰八族（规划 3.5.3）。
 * 每族三阶：I=d6 良品，II=d12 史诗，III=d20 传说。
 * 冷却公式：CD = 基础CD × (骰面-点数+1)/骰面，下限 SKILL_CD_MIN。
 */
public enum SkillFamily implements StringRepresentable {
    /** 回响：攻击拼点失败 → 同池重掷取高，不改骰势/保底。 */
    ECHO("echo", new int[]{32 * 20, 28 * 20, 24 * 20}),
    /** 追击：攻击胜利 → 命势伤害 +30/45/60%。 */
    PURSUIT("pursuit", new int[]{20 * 20, 18 * 20, 16 * 20}, 30, 45, 60),
    /** 护命：防御拼点失败 → 受伤 -20/30/40%。 */
    WARD("ward", new int[]{28 * 20, 24 * 20, 20 * 20}, 20, 30, 40),
    /** 破势：破势瞬间 → 额外 +10/15/20 命势；命破 +5/8/10%。 */
    BREAKER("breaker", new int[]{40 * 20, 36 * 20, 32 * 20}, 10, 15, 20),
    /** 逆命：攻击崩 → 改写为负，40/55/70% 伤害；返还 30/45/60% 攻击冷却。 */
    REVERSE("reverse", new int[]{45 * 20, 40 * 20, 35 * 20}, 40, 55, 70),
    /** 贪命：攻击胜利 → 消耗 4/6/8 饱食度，伤害 +15/20/25%。 */
    GREED("greed", new int[]{18 * 20, 16 * 20, 14 * 20}, 15, 20, 25),
    /** 定命：攻击失败 → 锁定最低骰，下次拼点该骰取最大面。 */
    LOCK("lock", new int[]{60 * 20, 50 * 20, 40 * 20}),
    /** 命佑：攻击失败 → 额外 +1/2/3 层保底。 */
    FAVOR("favor", new int[]{40 * 20, 36 * 20, 32 * 20}, 1, 2, 3);

    private final String name;
    public final int[] baseCdTicks;   // 按 I/II/III
    public final int[] params;        // 百分比或数值参数（0 表示该族无参数）

    SkillFamily(String name, int[] cd) {
        this(name, cd, new int[3]);
    }

    SkillFamily(String name, int[] cd, int p1, int p2, int p3) {
        this(name, cd, new int[]{p1, p2, p3});
    }

    SkillFamily(String name, int[] cd, int[] params) {
        this.name = name; this.baseCdTicks = cd; this.params = params;
    }

    @Override public String getSerializedName() { return name; }

    public int baseCd(int tier0) { return baseCdTicks[tier0]; }

    /** tier0: 0/1/2 → 参数。 */
    public int param(int tier0) { return params[tier0]; }

    public static SkillFamily byName(String s) {
        for (SkillFamily f : values()) if (f.name.equals(s)) return f;
        return ECHO;
    }
}
