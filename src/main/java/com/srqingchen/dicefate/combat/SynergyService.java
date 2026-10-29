package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.combat.DiceRoller.RollResult;
import com.srqingchen.dicefate.dice.DieElement;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import com.srqingchen.dicefate.data.DFRegistries;
import com.srqingchen.dicefate.data.SynergyDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 协同效应（规划 4.8）：从 RollResult 的面数/元素/材质计数检测激活协同。
 * 返回 (总点数平加, 防御平加, 命势加成%, 伤害加成%, 激活列表)。
 */
public final class SynergyService {
    private SynergyService() {}

    public record SynergyResult(
        int flatTotal,          // 总点数平加（攻防两侧计算各自调用）
        int flatDefense,        // 防御总点数平加（铁骰套）
        double poiseBonus,      // 命势伤害 %（同面三/全六面）
        int poiseFlat,          // 命势伤害平加（骨骰套 +1）
        double damageBonus,     // 伤害 % 入增伤桶（豹群 → 牌型加成部分）
        double reactionCdMod,   // 反应 CD 修正（双元 -20%）
        double reactionEffectMod, // 反应效果修正（双元+5%/四元+10%）
        double baseDamageMod,   // 基础伤害修正（四元 -5%）
        boolean monoLock,       // 纯色：不能触发反应
        List<Identifier> active
    ) {}

    public static SynergyResult evaluate(RollResult r, Registry<SynergyDefinition> registry) {
        int flatTotal = 0, flatDefense = 0, poiseFlat = 0;
        double poiseBonus = 0, damageBonus = 0, reactionCd = 0, reactionEff = 0, baseDmg = 0;
        boolean mono = false;
        List<Identifier> active = new ArrayList<>();

        for (SynergyDefinition def : registry) {
            switch (def.type()) {
                case SAME_FACE -> {
                    int count = r.faceCounts.values().stream().max(Integer::compare).orElse(0);
                    if (count >= def.count()) {
                        int bonusPer = count >= 5 ? (int) def.value2() : (int) def.value();
                        // 0.3.0 B3：每颗同面骰 +bonusPer（3 颗 +3、5 颗各 +2 = +10，策划 4.8）
                        flatTotal += bonusPer * count;
                        active.add(def.id());
                    }
                }
                case ALL_SIX -> {
                    if (!r.faceCounts.isEmpty() && r.faceCounts.containsKey(6) && r.faceCounts.size() == 1) {
                        flatTotal += 1;
                        poiseBonus += def.value() / 100.0;
                        active.add(def.id());
                    }
                }
                case LADDER -> {
                    int kinds = ladderKinds(r.faceCounts);
                    if (kinds >= def.count()) {
                        flatTotal += (int) Math.min(def.value(), kinds - 1);
                        active.add(def.id());
                    }
                }
                case DUAL_RESONANCE -> {
                    if (elementKindCount(r) == 2) {
                        reactionCd -= def.value() / 100.0;
                        reactionEff += def.value2() / 100.0;
                        active.add(def.id());
                    }
                }
                case TETRA -> {
                    if (elementKindCount(r) >= 4) {
                        reactionEff += def.value() / 100.0;
                        baseDmg -= def.value2() / 100.0;
                        active.add(def.id());
                    }
                }
                case MONO -> {
                    int maxElem = r.elementCounts.values().stream().max(Integer::compare).orElse(0);
                    if (maxElem >= def.count()) {
                        mono = true;
                        active.add(def.id());
                    }
                }
                case PACK_TRIPLE -> {
                    // 3+ 同面数且同元素（近似：同面数 ≥3 且存在单一元素 ≥3）
                    int sameFace = r.faceCounts.values().stream().max(Integer::compare).orElse(0);
                    int sameElem = r.elementCounts.values().stream().max(Integer::compare).orElse(0);
                    if (sameFace >= def.count() && sameElem >= def.count()) {
                        damageBonus += def.value() / 100.0;
                        active.add(def.id());
                    }
                }
                case BONE_SET -> {
                    if (r.materialCounts.getOrDefault("bone", 0) >= def.count()) {
                        poiseFlat += 1;
                        active.add(def.id());
                    }
                }
                case IRON_SET -> {
                    if (r.materialCounts.getOrDefault("iron", 0) >= def.count()) {
                        flatDefense += 1;
                        active.add(def.id());
                    }
                }
                case MITHRIL_SET -> {
                    if (r.materialCounts.getOrDefault("mithril", 0) >= def.count()) {
                        flatTotal += 1;
                        active.add(def.id());
                    }
                }
            }
        }
        return new SynergyResult(flatTotal, flatDefense, poiseBonus, poiseFlat, damageBonus,
            reactionCd, reactionEff, baseDmg, mono, active);
    }

    private static int elementKindCount(RollResult r) {
        return (int) r.elementCounts.entrySet().stream()
            .filter(e -> e.getKey().isReal() && e.getValue() > 0).count();
    }

    /** 连续面数种类数。 */
    private static int ladderKinds(Map<Integer, Integer> faceCounts) {
        TreeMap<Integer, Integer> sorted = new TreeMap<>(faceCounts);
        int best = 0, run = 0, prev = -10;
        for (int f : sorted.keySet()) {
            run = (f == prev + 1) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = f;
        }
        return best;
    }
}
