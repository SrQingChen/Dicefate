package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.combat.PoolSnapshot.SkillView;
import com.srqingchen.dicefate.combat.PoolSnapshot.SlotView;
import com.srqingchen.dicefate.dice.DieElement;
import com.srqingchen.dicefate.dice.DieRune;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 掷骰器（规划 2.3）：单槽结算顺序 = 锁定值 → 散竭 0 → 掷骰 → 骰纹触发 → 特殊面值。
 * 牌型在全部落定后检测；元素计数供反应检测。
 * 0.2.0：诅咒骰负值计入总点（最后统一 clamp≥0）；乘算骰记录骰种（分族系数）；
 * recordRune 实装；rollSkill 用实际骰面并支持幸运修正。
 */
public final class DiceRoller {
    private DiceRoller() {}

    /** 乘算骰单颗记录（分族系数结算用）。 */
    public record MulRoll(String diePath, int face, int value) {
        public double ratio() { return value / (double) Math.max(1, face); }
    }

    /** 一次掷骰的结果。 */
    public static final class RollResult {
        public final List<DieRollView> baseRolls = new ArrayList<>();
        public final List<DieRollView> addRolls = new ArrayList<>();
        public final List<DieRollView> mulRolls = new ArrayList<>();
        /** 基础骰原始总点（0.2.0 X1：不含加算骰；加算由 totalOf 单计）。 */
        public int rawTotal;
        /** 元素计数（基础槽）。 */
        public final Map<DieElement, Integer> elementCounts = new EnumMap<>(DieElement.class);
        /** 触发的骰纹（按骰序，含重复槽位触发）。 */
        public final List<DieRune> triggeredRunes = new ArrayList<>();
        /** 面数→已装备数量（协同用）。 */
        public final Map<Integer, Integer> faceCounts = new HashMap<>();
        /** 材质→数量（套装协同用）。 */
        public final Map<String, Integer> materialCounts = new HashMap<>();
        /** 乘算骰记录（diePath + roll/face）。 */
        public final List<MulRoll> mulRollsOut = new ArrayList<>();

        public int combatDieCount() { return baseRolls.size(); }
    }

    /** 掷整个快照（不含技能骰——技能骰按触发单独掷）。 */
    public static RollResult roll(PoolSnapshot snap, RandomSource rng) {
        RollResult r = new RollResult();
        int total = 0;

        // 基础槽
        for (SlotView sv : snap.slots) {
            DieRollView view = rollSlot(sv, snap.exhausted, rng);
            // 命运包取高：按 coRollChance 双掷取高（0.3.0 B8）
            if (snap.coRollChance > 0 && !snap.exhausted && sv.forcedValue < 0 && !sv.frozen
                && rng.nextDouble() < snap.coRollChance) {
                DieRollView second = rollSlot(sv, snap.exhausted, rng);
                if (second.value > view.value) {
                    second.pityRolled = true;
                    view = second;
                }
            }
            r.baseRolls.add(view);
            countMeta(r, sv);
            if (view.triggeredRune != null && view.triggeredRune != DieRune.NONE
                && r.triggeredRunes.size() < 8) r.triggeredRunes.add(view.triggeredRune);
            total += view.value;   // 负值（诅咒/裂面）可拉低，总点在 rawTotal 处 clamp≥0
        }
        // 加算槽（值入 addRolls，由 ContestService.totalOf 单计）
        for (SlotView sv : snap.addSlots) {
            DieRollView view = rollSlot(sv, snap.exhausted, rng);
            if (snap.coRollChance > 0 && !snap.exhausted && sv.forcedValue < 0 && !sv.frozen
                && rng.nextDouble() < snap.coRollChance) {
                DieRollView second = rollSlot(sv, snap.exhausted, rng);
                if (second.value > view.value) {
                    second.pityRolled = true;
                    view = second;
                }
            }
            r.addRolls.add(view);
            countMeta(r, sv);
        }
        // 乘算槽：不入总点
        for (SlotView sv : snap.mulSlots) {
            DieRollView view = rollSlot(sv, snap.exhausted, rng);
            r.mulRolls.add(view);
            if (view.value > 0) {
                String path = sv.def == null ? "mul_die" : sv.def.id().getPath();
                r.mulRollsOut.add(new MulRoll(path, sv.face(), view.value));
            }
        }
        r.rawTotal = Math.max(0, total);
        return r;
    }

    private static void countMeta(RollResult r, SlotView sv) {
        if (sv.def == null) return;
        // 0.3.0 B17：骰包骰/怪物兜底骰不参与面数/材质协同（防"铁骰套/全六面"被包骰凑出）；
        // 元素计数保留——元素包的用途就是凑反应（策划 3.8.1），验收风险⑤修正
        String path = sv.def.id().getPath();
        boolean synthetic = "pack_die".equals(path) || "mob_die".equals(path);
        if (sv.element().isReal()) r.elementCounts.merge(sv.element(), 1, Integer::sum);
        if (synthetic) return;
        r.faceCounts.merge(sv.face(), 1, Integer::sum);
        r.materialCounts.merge(sv.def.material().getSerializedName(), 1, Integer::sum);
    }

    /** 单槽结算。 */
    public static DieRollView rollSlot(SlotView sv, boolean poolExhausted, RandomSource rng) {
        DieRollView view = new DieRollView(sv.slotIndex, sv.face(), 0, sv.element());

        // 散竭/冻结 → 0（碎裂）
        if (poolExhausted || sv.frozen) {
            view.value = 0;
            view.crashed = true;
            return view;
        }
        // 强制值（定命 / 兑现最大 / 归零）
        if (sv.forcedValue >= 0) {
            view.value = sv.forcedValue;
            view.lockedUsed = true;
            applyWearCrack(sv, view);
            return view;
        }

        int face = sv.face();
        int v;
        if (sv.def != null && sv.def.hasCustomFaces()) {
            // 特殊面值骰：按面序随机（如 0/0/0/6/6/6、-1/2/3/4/5/8）
            v = sv.def.faceValueAt(rng.nextInt(face));
        } else {
            v = 1 + rng.nextInt(face);
        }

        // 唯一骰·雷罚：最大点 +2（策划 5.1.6，0.3.0 F2）
        if (sv.def != null && "unique_die_storm_verdict".equals(sv.def.id().getPath()) && v == face) {
            v = face + 2;
        }
        // 骰纹触发（按骰序；稳：掷 1 视为 2；贪/咒：点数上浮）
        if (sv.state != null) {
            DieRune rune = sv.state.runeId();
            switch (rune) {
                case STEADY -> { if (v == 1) v = 2; }
                case GREEDY -> v = Math.min(face, v + 1);
                case CURSED -> v = Math.min(face, v + 2);
                default -> {}
            }
            if (rune != DieRune.NONE) view.triggeredRune = rune;
        }

        view.value = v;
        applyWearCrack(sv, view);
        return view;
    }

    /** 磨损裂面：完全裂 → 点数 -1，最低 1（负值面不进一步下调）。 */
    private static void applyWearCrack(SlotView sv, DieRollView view) {
        if (sv.cracked() && view.value > 1) view.value -= 1;
    }

    /** 技能骰掷骰：返回点数（散竭 → 0 → CD 最大）。
     *  0.2.0：用实际骰面（skill.face），不再按 tier 硬编码。 */
    public static int rollSkill(SkillView skill, boolean exhausted, RandomSource rng) {
        if (exhausted) return 0;
        int face = Math.max(2, skill.face());
        return 1 + rng.nextInt(face);
    }

    /** 技能骰掷骰 + 幸运修正（规划 2.5.5：点数加 luckShift 后 clamp 1..face）。 */
    public static int rollSkill(SkillView skill, boolean exhausted, int luckShift, RandomSource rng) {
        int v = rollSkill(skill, exhausted, rng);
        if (exhausted) return 0;
        return Math.max(1, Math.min(skill.face(), v + luckShift));
    }
}
