package com.srqingchen.dicefate.dice;

import net.minecraft.util.StringRepresentable;

/** 骰子材质：决定基础耐久与套装协同。 */
public enum DieMaterial implements StringRepresentable {
    BONE("bone", 120),
    IRON("iron", 180),
    MITHRIL("mithril", 240),
    FATEGOLD("fategold", 300),
    STARLIGHT("starlight", 360),
    ELEMENTAL("elemental", 200),  // 元素骰（水晶质感）
    SKILL("skill", 240),          // 技能骰
    UNIQUE("unique", 280),        // 唯一骰
    CURSED("cursed", 220);        // 诅咒骰

    private final String name;
    private final int baseWear;
    DieMaterial(String name, int baseWear) { this.name = name; this.baseWear = baseWear; }
    @Override public String getSerializedName() { return name; }
    public int baseWear() { return baseWear; }
}
