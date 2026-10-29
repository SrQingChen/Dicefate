package com.srqingchen.dicefate.network;

import com.srqingchen.dicefate.DiceFate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端意图包（C→S）：命盘操作/兑现/骰灵开关/定命。
 * 服务端全量校验（战斗中锁定、槽类型、冷却）。
 */
public record ClientActionPayload(
    byte op,             // Op ordinal
    byte slotIdx,        // 操作槽位
    ItemStack stack      // 涉及的骰子（可为空）
) implements CustomPacketPayload {

    public enum Op {
        EQUIP_BASE, UNEQUIP_BASE, SWAP_BASE,      // slotIdx；SWAP 用 slotIdx=a*10+b
        EQUIP_ADD, UNEQUIP_ADD,
        EQUIP_MUL, UNEQUIP_MUL,
        EQUIP_SKILL, UNEQUIP_SKILL,
        SET_STANCE,                                // stance 用 slotIdx 传 ordinal
        SAVE_PRESET, LOAD_PRESET,                  // slotIdx = 预设号
        LOCK_DIE,                                  // slotIdx = 基础槽
        CASHOUT,                                   // 兑现（V 键）
        SPIRIT_TOGGLE,                             // 骰灵开关（G 键）
        OPEN_CODEX,                                // 打开图鉴
        POUCH_STORE,                               // 0.2.0：骰袋存入（stack，slotIdx=目标位）
        POUCH_TAKE                                 // 0.2.0：骰袋取出（slotIdx）
    }

    public static final CustomPacketPayload.Type<ClientActionPayload> TYPE =
        new CustomPacketPayload.Type<>(DiceFate.id("client_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BYTE, ClientActionPayload::op,
        ByteBufCodecs.BYTE, ClientActionPayload::slotIdx,
        ItemStack.OPTIONAL_STREAM_CODEC, ClientActionPayload::stack,
        ClientActionPayload::new
    );

    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
