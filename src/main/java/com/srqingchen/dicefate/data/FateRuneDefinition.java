package com.srqingchen.dicefate.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

/**
 * 命纹（骰运台产出的长时规则修正，规划 4.13，12 条）。
 * scope: die=挂在某颗骰上；player=玩家命途（至多 1 条）。
 */
public record FateRuneDefinition(
    Identifier id,
    Scope scope,
    Effect effect,
    double value,
    boolean consumed,       // 消耗型（回响命/逆命）
    int costSand,
    String offering         // 祭品物品 id
) {
    public enum Scope implements StringRepresentable {
        DIE("die"), PLAYER("player");
        private final String name;
        Scope(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public enum Effect implements StringRepresentable {
        MIN_AS_TWO("min_as_two"),     // 稳命：本骰掷 1 视为 2（每场 1 次）
        POISE_PLUS("poise_plus"),     // 锋命：胜利时命势伤害 +value
        DEF_PLUS("def_plus"),         // 壁命：防御时本骰 +value
        PIERCE_PLUS("pierce_plus"),   // 破命：胜利时穿透 +value
        MOMENTUM_PLUS("momentum_plus"), // 连命：胜利时骰势 +value
        REROLL_ONCE("reroll_once"),   // 回响命：失败时 value 概率重掷本骰（每场 1 次，消耗型）
        NULL_TO_LOSS("null_to_loss"), // 逆命：崩时 value 概率改写为负 40% 伤害（每场 1 次，消耗型）
        DAMAGE_REDUCE("damage_reduce"), // 守命：受伤 -value
        KILL_HEAL("kill_heal"),       // 猎命：击杀恢复 value 生命
        BREAK_FRENZY("break_frenzy"), // 祭命：破势后 8s 伤害 +value
        GREEDY("greedy"),             // 贪命：伤害 +value；最大生命 -value
        PITY_STEP("pity_step");       // 命佑：保底每层概率 +value

        private final String name;
        Effect(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public static final Codec<FateRuneDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.fieldOf("id").forGetter(FateRuneDefinition::id),
        StringRepresentable.fromEnum(Scope::values).fieldOf("scope").forGetter(FateRuneDefinition::scope),
        StringRepresentable.fromEnum(Effect::values).fieldOf("effect").forGetter(FateRuneDefinition::effect),
        Codec.doubleRange(0.0, 5.0).optionalFieldOf("value", 1.0).forGetter(FateRuneDefinition::value),
        Codec.BOOL.optionalFieldOf("consumed", false).forGetter(FateRuneDefinition::consumed),
        Codec.intRange(0, 500).optionalFieldOf("cost_sand", 20).forGetter(FateRuneDefinition::costSand),
        Codec.STRING.optionalFieldOf("offering", "dicefate:offering_bone").forGetter(FateRuneDefinition::offering)
    ).apply(i, FateRuneDefinition::new));
}
