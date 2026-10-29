package com.srqingchen.dicefate.dice;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * 骰子动态状态（物品数据组件）。静态属性走数据包 DieDefinition，这里只存"这颗骰的经历"。
 */
public record DieState(
    int wear,          // 磨损 0..maxWear
    String rune,       // 骰纹 id（""=无）
    UUID uuid,         // 身份（鉴定/定命/图鉴追踪）
    int lastRoll       // 上次拼点掷值（定命用）
) {
    public static final com.mojang.serialization.Codec<DieState> CODEC = RecordCodecBuilder.create(i -> i.group(
        com.mojang.serialization.Codec.intRange(0, 100000).optionalFieldOf("wear", 0).forGetter(DieState::wear),
        com.mojang.serialization.Codec.STRING.optionalFieldOf("rune", "").forGetter(DieState::rune),
        UUIDUtil.CODEC.optionalFieldOf("uuid")
            .xmap(o -> o.orElseGet(UUID::randomUUID), java.util.Optional::of).forGetter(DieState::uuid),
        com.mojang.serialization.Codec.INT.optionalFieldOf("last_roll", 0).forGetter(DieState::lastRoll)
    ).apply(i, DieState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DieState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, DieState::wear,
        ByteBufCodecs.STRING_UTF8, DieState::rune,
        UUIDUtil.STREAM_CODEC, DieState::uuid,
        ByteBufCodecs.VAR_INT, DieState::lastRoll,
        DieState::new
    );

    public DieState {
        if (uuid == null) uuid = UUID.randomUUID();
    }

    /** 全新骰的初始状态。 */
    public static DieState fresh() {
        return new DieState(0, "", UUID.randomUUID(), 0);
    }

    public DieRune runeId() {
        for (DieRune r : DieRune.values()) if (r.getSerializedName().equals(rune)) return r;
        return DieRune.NONE;
    }

    public DieState withWear(int w) { return new DieState(w, rune, uuid, lastRoll); }
    public DieState withRune(String r) { return new DieState(wear, r, uuid, lastRoll); }
    public DieState withLastRoll(int v) { return new DieState(wear, rune, uuid, v); }
}
