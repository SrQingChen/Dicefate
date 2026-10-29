package com.srqingchen.dicefate.network;

import com.srqingchen.dicefate.DiceFate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 0.2.0 骰袋内容同步（S→C，6 格）。 */
public record PouchSyncPayload(List<ItemStack> slots) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PouchSyncPayload> TYPE =
        new CustomPacketPayload.Type<>(DiceFate.id("pouch_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PouchSyncPayload> STREAM_CODEC =
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(6))
            .map(PouchSyncPayload::new, PouchSyncPayload::slots);

    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
