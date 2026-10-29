package com.srqingchen.dicefate.dice;

import net.minecraft.util.StringRepresentable;

/**
 * 属性骰纹（规划 4.4，技能纹已删除）。刻骰台用命纹墨施加/替换。
 * 咒纹仅诅咒骰原生携带。
 */
public enum DieRune implements StringRepresentable {
    NONE("none", 0),
    /** 锋：胜利时命势伤害 +1（不同骰最多 3 层）。 */
    EDGE("edge", 3),
    /** 壁：防御时本骰点数 +1（最多 3 层）。 */
    WALL("wall", 3),
    /** 稳：本骰掷出 1 视为 2（不叠加）。 */
    STEADY("steady", 1),
    /** 烈：大胜时伤害 +5%（不叠加）。 */
    FIERCE("fierce", 1),
    /** 韧：装备期间护甲 +1（不叠加）。 */
    TOUGH("tough", 1),
    /** 迅：攻击冷却 -3%（最多 2 层）。 */
    SWIFT("swift", 2),
    /** 贪：本骰点数 +1；命势伤害 -1（不叠加）。 */
    GREEDY("greedy", 1),
    /** 咒：本骰点数 +2；8% 概率裂面（仅诅咒骰）。 */
    CURSED("cursed", 1);

    private final String name;
    public final int maxStacks;
    DieRune(String name, int maxStacks) { this.name = name; this.maxStacks = maxStacks; }
    @Override public String getSerializedName() { return name; }
}
