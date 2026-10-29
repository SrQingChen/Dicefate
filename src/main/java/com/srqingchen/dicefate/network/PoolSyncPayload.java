package com.srqingchen.dicefate.network;

import com.srqingchen.dicefate.DiceFate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 命盘内容同步（S→C）：登录/编辑/切预设时发送，驱动骰池 HUD。 */
public record PoolSyncPayload(
    byte baseSlots,
    List<ItemStack> base,
    ItemStack add,
    ItemStack mul,
    ItemStack skill0,
    ItemStack skill1
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PoolSyncPayload> TYPE =
        new CustomPacketPayload.Type<>(DiceFate.id("pool_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PoolSyncPayload> STREAM_CODEC = StreamCodec.composite(
        net.minecraft.network.codec.ByteBufCodecs.BYTE, PoolSyncPayload::baseSlots,
        ItemStack.OPTIONAL_STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list(8)), PoolSyncPayload::base,
        ItemStack.OPTIONAL_STREAM_CODEC, PoolSyncPayload::add,
        ItemStack.OPTIONAL_STREAM_CODEC, PoolSyncPayload::mul,
        ItemStack.OPTIONAL_STREAM_CODEC, PoolSyncPayload::skill0,
        ItemStack.OPTIONAL_STREAM_CODEC, PoolSyncPayload::skill1,
        PoolSyncPayload::new
    );

    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
