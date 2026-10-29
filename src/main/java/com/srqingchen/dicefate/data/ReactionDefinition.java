package com.srqingchen.dicefate.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/**
 * 元素反应定义（数据包 dicefate/reaction）。四种反应：
 * 蒸汽(ember+frost 差率减半+10%)、爆燃(ember+storm 3%真伤/自损2%)、
 * 腐蚀(venom+storm 护甲-15%)、湮灭(radiant+umbral 平→胜)。
 */
public record ReactionDefinition(
    Identifier id,
    List<String> elements,       // 恰好两个
    Effect effect,
    double value,                // 效果主参数
    double bonusDamage,          // 胜利附加增伤（入增伤桶）
    int cooldownTicks
) {
    public enum Effect implements StringRepresentable {
        COMPRESS_MARGIN("compress_margin"),   // 差率绝对值 ×value
        TRUE_DAMAGE("true_damage"),           // value=目标最大生命% 真伤；自损固定 2%
        CORRODE("corrode"),                   // 护甲 -value 持续 value2 秒
        NULLIFY_TIE("nullify_tie");           // 平 → 胜 r=0.25

        private final String name;
        Effect(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public static final Codec<ReactionDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.fieldOf("id").forGetter(ReactionDefinition::id),
        Codec.STRING.listOf().fieldOf("elements").forGetter(ReactionDefinition::elements),
        StringRepresentable.fromEnum(Effect::values).fieldOf("effect").forGetter(ReactionDefinition::effect),
        Codec.doubleRange(0.0, 1.0).optionalFieldOf("value", 0.0).forGetter(ReactionDefinition::value),
        Codec.doubleRange(0.0, 2.0).optionalFieldOf("bonus_damage", 0.0).forGetter(ReactionDefinition::bonusDamage),
        Codec.intRange(0, 1200).optionalFieldOf("cooldown", 0).forGetter(ReactionDefinition::cooldownTicks)
    ).apply(i, ReactionDefinition::new));
}
