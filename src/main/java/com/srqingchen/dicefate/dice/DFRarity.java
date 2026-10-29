package com.srqingchen.dicefate.dice;

import net.minecraft.util.StringRepresentable;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

/**
 * 模组稀有度六档：凡品/良品/珍品/史诗/传说/命定。
 * 映射原版 Rarity 用于着色；命定档额外用暗金斜体表现。
 */
public enum DFRarity implements StringRepresentable {
    COMMON("common", Rarity.COMMON, ChatFormatting.WHITE),        // 凡品
    UNCOMMON("uncommon", Rarity.UNCOMMON, ChatFormatting.YELLOW), // 良品
    RARE("rare", Rarity.RARE, ChatFormatting.AQUA),               // 珍品
    EPIC("epic", Rarity.EPIC, ChatFormatting.LIGHT_PURPLE),       // 史诗
    LEGENDARY("legendary", Rarity.EPIC, ChatFormatting.GOLD),     // 传说
    FATED("fated", Rarity.EPIC, ChatFormatting.GOLD);             // 命定

    private final String name;
    private final Rarity vanilla;
    private final ChatFormatting color;
    DFRarity(String name, Rarity vanilla, ChatFormatting color) {
        this.name = name; this.vanilla = vanilla; this.color = color;
    }
    @Override public String getSerializedName() { return name; }
    public Rarity vanilla() { return vanilla; }
    public ChatFormatting color() { return color; }
}
