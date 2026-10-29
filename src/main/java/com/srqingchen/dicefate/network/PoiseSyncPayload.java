package com.srqingchen.dicefate.network;

import com.srqingchen.dicefate.DiceFate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * 附近生物命势同步（S→C）：32 格内战斗生物的命势/命定/命损/散竭，供头顶条带与 HUD。
 */
public record PoiseSyncPayload(List<Entry> entries) implements CustomPacketPayload {

    public record Entry(
        int entityId,
        int poise,
        int maxPoise,
        byte state,           // 0 NORMAL 1 EXHAUSTED 2 RECOVERING
        int decree,
        int decreeThreshold,
        int woundTicks,
        byte rank,            // 0 normal 1 elite 2 boss 3 guard
        int gameTick
    ) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Entry::entityId,
            ByteBufCodecs.VAR_INT, Entry::poise,
            ByteBufCodecs.VAR_INT, Entry::maxPoise,
            ByteBufCodecs.BYTE, Entry::state,
            ByteBufCodecs.VAR_INT, Entry::decree,
            ByteBufCodecs.VAR_INT, Entry::decreeThreshold,
            ByteBufCodecs.VAR_INT, Entry::woundTicks,
            ByteBufCodecs.BYTE, Entry::rank,
            ByteBufCodecs.VAR_INT, Entry::gameTick,
            Entry::new
        );
    }

    public static final CustomPacketPayload.Type<PoiseSyncPayload> TYPE =
        new CustomPacketPayload.Type<>(DiceFate.id("poise_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PoiseSyncPayload> STREAM_CODEC =
        Entry.CODEC.apply(ByteBufCodecs.list(64)).map(PoiseSyncPayload::new, PoiseSyncPayload::entries);

    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
