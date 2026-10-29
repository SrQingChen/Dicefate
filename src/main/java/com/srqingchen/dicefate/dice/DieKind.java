package com.srqingchen.dicefate.dice;

import net.minecraft.util.StringRepresentable;

/** 骰子分类：决定可放入的槽位。 */
public enum DieKind implements StringRepresentable {
    BASIC("basic"),       // 基础槽
    ELEMENT("element"),   // 基础槽（带元素）
    SKILL("skill"),       // 技能槽
    ADD("add"),           // 加算槽
    MUL("mul"),           // 乘算槽
    UNIQUE("unique"),     // 基础槽，命盘限1
    CURSED("cursed");     // 基础槽

    private final String name;
    DieKind(String name) { this.name = name; }
    @Override public String getSerializedName() { return name; }

    /** 是否可以放进基础骰位。 */
    public boolean fitsBase() {
        return this == BASIC || this == ELEMENT || this == UNIQUE || this == CURSED;
    }
}
