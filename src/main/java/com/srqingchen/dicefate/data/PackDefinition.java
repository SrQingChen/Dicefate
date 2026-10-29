package com.srqingchen.dicefate.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/**
 * 骰子包定义（数据包 dicefate/pack）。
 * mode: replace=替换战斗骰池；fate=替换+每槽按 coRoll 概率取高。
 */
public record PackDefinition(
    Identifier id,
    int duration,          // 拼点次数
    Mode mode,
    double coRoll,         // 取高概率（命运包）
    List<PackSlot> slots   // 替换池（按玩家骰位数取前 N）
) {
    public enum Mode implements StringRepresentable {
        REPLACE("replace"), FATE("fate");
        private final String name;
        Mode(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public record PackSlot(int face, String element, List<Integer> faceValues) {
        public PackSlot(int face, String element) { this(face, element, List.of()); }
        /** 该槽的特殊面值（如零面包 0/0/0/6/6/6；为空则 1..face）。 */
        public boolean hasCustomFaces() { return faceValues != null && !faceValues.isEmpty(); }
        public static final Codec<PackSlot> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 24).fieldOf("face").forGetter(PackSlot::face),
            Codec.STRING.optionalFieldOf("element", "none").forGetter(PackSlot::element),
            Codec.INT.listOf().optionalFieldOf("face_values", List.of()).forGetter(PackSlot::faceValues)
        ).apply(i, PackSlot::new));
    }

    public static final Codec<PackDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.fieldOf("id").forGetter(PackDefinition::id),
        Codec.intRange(1, 8).fieldOf("duration").forGetter(PackDefinition::duration),
        StringRepresentable.fromEnum(Mode::values).optionalFieldOf("mode", Mode.REPLACE).forGetter(PackDefinition::mode),
        Codec.doubleRange(0.0, 1.0).optionalFieldOf("co_roll", 0.0).forGetter(PackDefinition::coRoll),
        PackSlot.CODEC.listOf().fieldOf("slots").forGetter(PackDefinition::slots)
    ).apply(i, PackDefinition::new));
}
