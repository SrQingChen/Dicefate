package com.srqingchen.dicefate.network;

import com.srqingchen.dicefate.DiceFate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 玩家命数状态同步（S→C）：骰势/姿态/技能CD/骰包/兑现/骰灵。变化时或每 10t。
 */
public record FateStatePayload(Bars bars, Meta meta, Extras extras) implements CustomPacketPayload {

    public record Bars(int momentum, int momentumOverflow, boolean cashoutArmed,
                       int cashoutValue, long cashoutExpire) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Bars> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Bars::momentum,
            ByteBufCodecs.VAR_INT, Bars::momentumOverflow,
            ByteBufCodecs.BOOL, Bars::cashoutArmed,
            ByteBufCodecs.VAR_INT, Bars::cashoutValue,
            ByteBufCodecs.VAR_LONG, Bars::cashoutExpire,
            Bars::new
        );
    }

    public record Meta(byte stanceOrdinal, byte stanceMastery, long skillCd0, long skillCd1,
                       byte baseSlots, boolean addUnlocked, boolean mulUnlocked, byte skillSlots,
                       boolean inCombat) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Meta> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, Meta::stanceOrdinal,
            ByteBufCodecs.BYTE, Meta::stanceMastery,
            ByteBufCodecs.VAR_LONG, Meta::skillCd0,
            ByteBufCodecs.VAR_LONG, Meta::skillCd1,
            ByteBufCodecs.BYTE, Meta::baseSlots,
            ByteBufCodecs.BOOL, Meta::addUnlocked,
            ByteBufCodecs.BOOL, Meta::mulUnlocked,
            ByteBufCodecs.BYTE, Meta::skillSlots,
            ByteBufCodecs.BOOL, Meta::inCombat,
            Meta::new
        );
    }

    public record Extras(String pack, int packRemaining, boolean spiritActive, byte spiritLevel,
                         int lockSlot, int lockValue, long gameTick) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Extras> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Extras::pack,
            ByteBufCodecs.VAR_INT, Extras::packRemaining,
            ByteBufCodecs.BOOL, Extras::spiritActive,
            ByteBufCodecs.BYTE, Extras::spiritLevel,
            ByteBufCodecs.VAR_INT, Extras::lockSlot,
            ByteBufCodecs.VAR_INT, Extras::lockValue,
            ByteBufCodecs.VAR_LONG, Extras::gameTick,
            Extras::new
        );
    }

    public static final CustomPacketPayload.Type<FateStatePayload> TYPE =
        new CustomPacketPayload.Type<>(DiceFate.id("fate_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FateStatePayload> STREAM_CODEC = StreamCodec.composite(
        Bars.CODEC, FateStatePayload::bars,
        Meta.CODEC, FateStatePayload::meta,
        Extras.CODEC, FateStatePayload::extras,
        FateStatePayload::new
    );

    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
