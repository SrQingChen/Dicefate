package com.srqingchen.dicefate.combat;

import net.minecraft.util.StringRepresentable;

/** 拼点战果（规划 2.4）。 */
public enum Outcome implements StringRepresentable {
    CRASH("crash"),        // 崩 Miss：A < missThreshold*D
    LOSE("lose"),          // 负
    TIE("tie"),            // 平
    NARROW_WIN("narrow"),  // 惨胜 0<r<0.25
    WIN("win"),            // 胜 0.25≤r<0.5
    GREAT_WIN("great"),    // 大胜 0.5≤r<1
    DESTINY("destiny");    // 天命大胜 r≥1

    private final String name;
    Outcome(String name) { this.name = name; }
    @Override public String getSerializedName() { return name; }

    public boolean isWin() { return this == NARROW_WIN || this == WIN || this == GREAT_WIN || this == DESTINY; }
    public boolean isGreat() { return this == GREAT_WIN || this == DESTINY; }
}
