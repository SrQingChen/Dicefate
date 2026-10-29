package com.srqingchen.dicefate.combat;

/** 牌型（规划 4.9）：连续 3 骰位窗口；全池升格天命。 */
public enum PatternType {
    NONE(null, 0, 0, 0),
    STRAIGHT("straight", 0.45, 0.10, 2),       // 顺子 +45% 穿透+10% 命势+2
    TRIPLE("triple", 0.60, 0.0, 3),            // 豹子 +60% 命势+3 返10%CD
    DESTINY_STRAIGHT("destiny_straight", 0.70, 0.15, 3), // 天命顺子
    DESTINY_TRIPLE("destiny_triple", 0.90, 0.0, 5);      // 天命豹子 命势+5 返20%CD

    public final String id;
    public final double damageBonus;   // 入增伤桶
    public final double pierceBonus;   // 穿透加值
    public final int poiseBonus;       // 命势加值

    PatternType(String id, double dmg, double pierce, int poise) {
        this.id = id; this.damageBonus = dmg; this.pierceBonus = pierce; this.poiseBonus = poise;
    }

    public boolean isTriple() { return this == TRIPLE || this == DESTINY_TRIPLE; }
    public boolean isDestiny() { return this == DESTINY_STRAIGHT || this == DESTINY_TRIPLE; }
}
