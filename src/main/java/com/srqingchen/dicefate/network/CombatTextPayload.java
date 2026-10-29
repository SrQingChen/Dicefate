package com.srqingchen.dicefate.network;

import com.srqingchen.dicefate.DiceFate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 实体飘字（S→C）：不创建实体，客户端渲染浮动文本。 */
public record CombatTextPayload(
    int entityId,
    byte kind,       // TextType ordinal
    int value,       // 数值（伤害/层数）
    byte color       // 0 白 1 黄 2 绿 3 金 4 红 5 灰 6 紫红 7 青绿
) implements CustomPacketPayload {

    public enum TextType {
        DAMAGE, MISS, POISE, POISE_BREAK, FATEBREAK, WOUND, EXHAUSTED,
        PATTERN, REACTION, SKILL, MOMENTUM, CASHOUT, LOCK, SPIRIT, PITY, SHIELD
    }

    public static final CustomPacketPayload.Type<CombatTextPayload> TYPE =
        new CustomPacketPayload.Type<>(DiceFate.id("combat_text"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CombatTextPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, CombatTextPayload::entityId,
        ByteBufCodecs.BYTE, CombatTextPayload::kind,
        ByteBufCodecs.VAR_INT, CombatTextPayload::value,
        ByteBufCodecs.BYTE, CombatTextPayload::color,
        CombatTextPayload::new
    );

    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
