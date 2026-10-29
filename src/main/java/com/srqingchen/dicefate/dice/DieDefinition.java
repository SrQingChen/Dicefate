package com.srqingchen.dicefate.dice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Optional;

/**
 * 骰子静态定义（数据包注册表 dicefate/die，每骰一 JSON）。
 * 物品注册与之一一对应（83 个真实物品）。
 *
 * @param id         注册 id，如 die_bone_d6
 * @param face       面数（4..24）；特殊骰如零面骰 face=6 但面值被 faceValues 覆盖
 * @param kind       槽位类别
 * @param material   材质（决定耐久基线/套装）
 * @param rarity     稀有度档
 * @param element    元素（无则 NONE）
 * @param faceValues 特殊面值覆盖（如 0/0/0/6/6/6、-1/2/3/4/5/8）；空=常规 1..face
 * @param skill      技能骰族 id（仅 SKILL）
 * @param skillTier  技能阶 0/1/2
 */
public record DieDefinition(
    Identifier id,
    int face,
    DieKind kind,
    DieMaterial material,
    DFRarity rarity,
    DieElement element,
    List<Integer> faceValues,
    Optional<Identifier> skill,
    int skillTier
) {
    public static final Codec<DieDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.fieldOf("id").forGetter(DieDefinition::id),
        Codec.intRange(1, 24).fieldOf("face").forGetter(DieDefinition::face),
        StringRepresentable.fromEnum(DieKind::values).fieldOf("kind").forGetter(DieDefinition::kind),
        StringRepresentable.fromEnum(DieMaterial::values).fieldOf("material").forGetter(DieDefinition::material),
        StringRepresentable.fromEnum(DFRarity::values).fieldOf("rarity").forGetter(DieDefinition::rarity),
        StringRepresentable.fromEnum(DieElement::values).optionalFieldOf("element", DieElement.NONE).forGetter(DieDefinition::element),
        Codec.INT.listOf().optionalFieldOf("face_values", List.of()).forGetter(DieDefinition::faceValues),
        Identifier.CODEC.optionalFieldOf("skill").forGetter(DieDefinition::skill),
        Codec.intRange(0, 2).optionalFieldOf("skill_tier", 0).forGetter(DieDefinition::skillTier)
    ).apply(i, DieDefinition::new));

    /** 常规骰最大面值；特殊面值骰取 faceValues 最大值（用于显示/定命上限）。 */
    public int maxFace() {
        if (!faceValues.isEmpty()) return faceValues.stream().max(Integer::compare).orElse(face);
        return face;
    }

    public boolean hasCustomFaces() {
        return faceValues != null && faceValues.size() > 1;
    }

    /** 该骰第 idx(0-based) 面的值。 */
    public int faceValueAt(int idx) {
        if (faceValues != null && !faceValues.isEmpty()) {
            int v = faceValues.get(Math.floorMod(idx, faceValues.size()));
            return Math.min(v, face); // 覆盖值不得超过面数上限（0 与负值允许）
        }
        return idx + 1;
    }

    public SkillFamily skillFamily() {
        return skill.map(s -> SkillFamily.byName(s.getPath())).orElse(null);
    }
}
