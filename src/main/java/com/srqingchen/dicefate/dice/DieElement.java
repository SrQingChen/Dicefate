package com.srqingchen.dicefate.dice;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Rarity;

/** 骰子元素。六元素 + 无。 */
public enum DieElement implements StringRepresentable {
    NONE("none"),
    EMBER("ember"),     // 火
    FROST("frost"),     // 冰
    STORM("storm"),     // 雷
    VENOM("venom"),     // 毒
    RADIANT("radiant"), // 光
    UMBRAL("umbral");   // 暗

    public static final DieElement[] ELEMENTS = {EMBER, FROST, STORM, VENOM, RADIANT, UMBRAL};
    private final String serializedName;

    DieElement(String name) { this.serializedName = name; }

    @Override public String getSerializedName() { return serializedName; }

    public boolean isReal() { return this != NONE; }

    /** 与另一元素可否构成反应（顺序无关）。 */
    public boolean reactsWith(DieElement other) {
        return switch (this) {
            case EMBER -> other == FROST || other == STORM;
            case FROST -> other == EMBER;
            case STORM -> other == EMBER || other == VENOM;
            case VENOM -> other == STORM;
            case RADIANT -> other == UMBRAL;
            case UMBRAL -> other == RADIANT;
            default -> false;
        };
    }

    public static DieElement byName(String s) {
        for (DieElement e : values()) if (e.serializedName.equals(s)) return e;
        return NONE;
    }
}
