package com.srqingchen.dicefate.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

/**
 * 协同效应定义（数据包 dicefate/synergy）。10 种，效果逻辑按 type 分发。
 */
public record SynergyDefinition(
    Identifier id,
    Type type,
    int count,          // 触发所需数量/种类
    double value,       // 主参数（+1/+2/百分比等）
    double value2       // 次参数（5+ 颗时的强化值等）
) {
    public enum Type implements StringRepresentable {
        SAME_FACE("same_face"),        // count+ 颗同面数：这些骰各 +value；5+ 改 +value2
        ALL_SIX("all_six"),            // 全 d6：总点数 +1；命势伤害 +value
        LADDER("ladder"),              // 至少 count 种连续面数：最低面数骰 +1/阶，最多 +value
        DUAL_RESONANCE("dual_resonance"), // 2 种元素：反应 CD -value；反应效果 +value2
        TETRA("tetra"),                // 4 种元素：反应效果 +value；基础伤害 -value2
        MONO("mono"),                  // 3+ 颗同元素：该元素被动 +value；不能触发反应
        PACK_TRIPLE("pack_triple"),    // 3+ 颗同面数且同元素：牌型增伤 +value
        BONE_SET("bone_set"),          // 3+ 颗骨制：命势伤害 +1
        IRON_SET("iron_set"),          // 3+ 颗铁制：防御总点数 +1
        MITHRIL_SET("mithril_set");    // 3+ 颗秘银：总点数 +1

        private final String name;
        Type(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public static final Codec<SynergyDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.fieldOf("id").forGetter(SynergyDefinition::id),
        StringRepresentable.fromEnum(Type::values).fieldOf("type").forGetter(SynergyDefinition::type),
        Codec.intRange(2, 8).optionalFieldOf("count", 3).forGetter(SynergyDefinition::count),
        Codec.doubleRange(0.0, 5.0).optionalFieldOf("value", 1.0).forGetter(SynergyDefinition::value),
        Codec.doubleRange(0.0, 5.0).optionalFieldOf("value2", 2.0).forGetter(SynergyDefinition::value2)
    ).apply(i, SynergyDefinition::new));
}
