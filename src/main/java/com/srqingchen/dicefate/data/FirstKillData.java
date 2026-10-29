package com.srqingchen.dicefate.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.srqingchen.dicefate.DiceFate;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Boss 首杀记录（0.3.0 F5，世界级 SavedData）：
 * 首杀掉落（加算/乘算之印、唯一骰、专属技能骰等）只在首次击杀时由 LivingDropsEvent 代码补发；
 * loot_table 只保留常规掉落。
 */
public class FirstKillData extends SavedData {

    public final Set<Identifier> killedBosses = new HashSet<>();

    public static final Codec<FirstKillData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.listOf().fieldOf("killed").forGetter(d -> List.copyOf(d.killedBosses))
    ).apply(i, FirstKillData::new));

    public static final SavedDataType<FirstKillData> TYPE =
        new SavedDataType<>(DiceFate.id("first_kills"), FirstKillData::new, CODEC, null);

    public FirstKillData() {}

    public FirstKillData(List<Identifier> killed) {
        killedBosses.addAll(killed);
    }

    public static FirstKillData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** 记录首杀；返回 true 当且仅当这是首次。 */
    public boolean markFirstKill(Identifier boss) {
        setDirty();
        return killedBosses.add(boss);
    }

    public boolean hasKilled(Identifier boss) {
        return killedBosses.contains(boss);
    }
}
