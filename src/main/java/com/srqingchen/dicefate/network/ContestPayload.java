package com.srqingchen.dicefate.network;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.combat.Outcome;
import com.srqingchen.dicefate.dice.DieElement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼点结果展示包（S→C）。攻击轨/受击轨各持最新一条，直接覆盖。
 * meta 位域：bits0=track；bits1-3=outcome；bits4-7=attackerLuck(+4偏移)；
 * bits8-11=defenderLuck(+4偏移)；bits12-15=目标数(×N 合并显示)。
 */
public record ContestPayload(
    int meta,
    int attackerId,
    int defenderId,
    List<DieEntry> attackerRolls,
    List<DieEntry> defenderRolls,
    int attackerTotal,
    int defenderTotal,
    float finalDamage,
    int poiseDamage,
    int decreeGain,
    List<Byte> triggers
) implements CustomPacketPayload {

    public static int packMeta(int track, Outcome outcome, int atkLuck, int defLuck, int targetCount) {
        return (track & 1)
            | ((outcome.ordinal() & 7) << 1)
            | (((atkLuck + 4) & 15) << 4)
            | (((defLuck + 4) & 15) << 8)
            | ((targetCount & 15) << 12);
    }

    public int track() { return meta & 1; }
    public Outcome outcome() { return Outcome.values()[(meta >> 1) & 7]; }
    public int attackerLuck() { return ((meta >> 4) & 15) - 4; }
    public int defenderLuck() { return ((meta >> 8) & 15) - 4; }
    public int targetCount() { return (meta >> 12) & 15; }

    public enum Trigger {
        NONE, PATTERN_STRAIGHT, PATTERN_TRIPLE, PATTERN_DSTRAIGHT, PATTERN_DTRIPLE,
        REACTION, SYNERGY, SKILL_ECHO, SKILL_PURSUIT, SKILL_WARD, SKILL_BREAKER,
        SKILL_REVERSE, SKILL_GREED, SKILL_LOCK, SKILL_FAVOR,
        PITY, SPIRIT, CASHOUT, LOCK, EXHAUSTED, FATEBREAK, WOUND, POISE_BREAK,
        PERFECT_BLOCK, SHIELD_BREAK, MOMENTUM_LOSS, MOMENTUM_NEGATE, LUCKY
    }

    public record DieEntry(byte face, int value, byte elementOrdinal, byte flags) {
        public static final StreamCodec<RegistryFriendlyByteBuf, DieEntry> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, DieEntry::face,
            ByteBufCodecs.VAR_INT, DieEntry::value,
            ByteBufCodecs.BYTE, DieEntry::elementOrdinal,
            ByteBufCodecs.BYTE, DieEntry::flags,
            DieEntry::new
        );

        public static final byte FLAG_CRASHED = 1, FLAG_PITY = 2, FLAG_NUDGED = 4, FLAG_LOCKED = 8;

        public boolean cracked() { return (flags & FLAG_CRASHED) != 0; }
        public boolean pity() { return (flags & FLAG_PITY) != 0; }
        public boolean nudged() { return (flags & FLAG_NUDGED) != 0; }
        public boolean locked() { return (flags & FLAG_LOCKED) != 0; }
        public DieElement element() { return DieElement.values()[elementOrdinal]; }
    }

    public static final CustomPacketPayload.Type<ContestPayload> TYPE =
        new CustomPacketPayload.Type<>(DiceFate.id("contest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ContestPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ContestPayload::meta,
        ByteBufCodecs.VAR_INT, ContestPayload::attackerId,
        ByteBufCodecs.VAR_INT, ContestPayload::defenderId,
        DieEntry.CODEC.apply(ByteBufCodecs.list(16)), ContestPayload::attackerRolls,
        DieEntry.CODEC.apply(ByteBufCodecs.list(16)), ContestPayload::defenderRolls,
        ByteBufCodecs.VAR_INT, ContestPayload::attackerTotal,
        ByteBufCodecs.VAR_INT, ContestPayload::defenderTotal,
        ByteBufCodecs.FLOAT, ContestPayload::finalDamage,
        ByteBufCodecs.VAR_INT, ContestPayload::poiseDamage,
        ByteBufCodecs.VAR_INT, ContestPayload::decreeGain,
        ByteBufCodecs.BYTE.apply(ByteBufCodecs.list(16)), ContestPayload::triggers,
        ContestPayload::new
    );

    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static List<Byte> triggers(byte... ordinals) {
        List<Byte> out = new ArrayList<>();
        for (byte b : ordinals) out.add(b);
        return out;
    }
}
