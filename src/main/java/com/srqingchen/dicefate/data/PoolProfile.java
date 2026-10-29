package com.srqingchen.dicefate.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Optional;

/**
 * 原版生物命盘档案（数据包 dicefate/pool_profile）。
 * 不新增实体：野外生物全部用档案驱动。
 */
public record PoolProfile(
    Identifier id,
    Identifier entity,          // 目标实体类型
    int tier,                   // 0=序章 … 3=后期
    Rank rank,                  // normal/elite/boss/guard
    List<SlotSpec> slots,
    Stance stance,
    List<Identifier> skillDice, // 空槽位即无
    List<DropSpec> drops,
    Optional<Identifier> lootOverride
) {
    public enum Rank implements StringRepresentable {
        NORMAL("normal"), ELITE("elite"), BOSS("boss"), GUARD("guard");
        private final String name;
        Rank(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    /** 姿态：与玩家同一套曲线（怪物简化使用）。 */
    public enum Stance implements StringRepresentable {
        BALANCED("balanced"), BACKWATER("backwater"), GUARD("guard"), TRICKY("tricky");
        private final String name;
        Stance(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    /** 额外掉落（命砂/命尘/精华等）。 */
    public record DropSpec(String item, int min, int max, double chance) {
        public static final Codec<DropSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("item").forGetter(DropSpec::item),
            Codec.intRange(0, 1024).optionalFieldOf("min", 1).forGetter(DropSpec::min),
            Codec.intRange(0, 1024).optionalFieldOf("max", 1).forGetter(DropSpec::max),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("chance", 1.0).forGetter(DropSpec::chance)
        ).apply(i, DropSpec::new));
    }

    public record SlotSpec(int face, String element) {
        public static final Codec<SlotSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 24).fieldOf("face").forGetter(SlotSpec::face),
            Codec.STRING.optionalFieldOf("element", "none").forGetter(SlotSpec::element)
        ).apply(i, SlotSpec::new));
    }

    public static final Codec<PoolProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.fieldOf("id").forGetter(PoolProfile::id),
        Identifier.CODEC.fieldOf("entity").forGetter(PoolProfile::entity),
        Codec.intRange(0, 5).optionalFieldOf("tier", 0).forGetter(PoolProfile::tier),
        StringRepresentable.fromEnum(Rank::values).optionalFieldOf("rank", Rank.NORMAL).forGetter(PoolProfile::rank),
        SlotSpec.CODEC.listOf().fieldOf("slots").forGetter(PoolProfile::slots),
        StringRepresentable.fromEnum(Stance::values).optionalFieldOf("stance", Stance.BALANCED).forGetter(PoolProfile::stance),
        Identifier.CODEC.listOf().optionalFieldOf("skill_dice", List.<Identifier>of()).forGetter(PoolProfile::skillDice),
        DropSpec.CODEC.listOf().optionalFieldOf("drops", List.<DropSpec>of()).forGetter(PoolProfile::drops),
        Identifier.CODEC.optionalFieldOf("loot_override").forGetter(PoolProfile::lootOverride)
    ).apply(i, PoolProfile::new));
}
