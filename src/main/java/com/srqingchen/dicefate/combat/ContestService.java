package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.DFConfig;
import com.srqingchen.dicefate.attachment.MobFateData;
import com.srqingchen.dicefate.attachment.PlayerFateData;
import com.srqingchen.dicefate.combat.DiceRoller.MulRoll;
import com.srqingchen.dicefate.combat.DiceRoller.RollResult;
import com.srqingchen.dicefate.combat.SynergyService.SynergyResult;
import com.srqingchen.dicefate.compat.CuriosCompat;
import com.srqingchen.dicefate.data.DFRegistries;
import com.srqingchen.dicefate.data.PackDefinition;
import com.srqingchen.dicefate.data.PoolProfile;
import com.srqingchen.dicefate.data.ReactionDefinition;
import com.srqingchen.dicefate.dice.DieElement;
import com.srqingchen.dicefate.dice.DieRune;
import com.srqingchen.dicefate.dice.SkillFamily;
import com.srqingchen.dicefate.network.CombatTextPayload;
import com.srqingchen.dicefate.network.ContestPayload;
import com.srqingchen.dicefate.network.DFNetwork;
import com.srqingchen.dicefate.registry.ModAttachments;
import com.srqingchen.dicefate.registry.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 拼点服务（规划 02/03 唯一入口）：同一 tick 内完成双方掷骰、效果与伤害计算。
 * HUD 只做落定展示；数值此刻已定。
 *
 * 0.3.0 重构要点（对照 0.3.0 修复规划批次 B）：
 * - 技能骰移到最终战果之后触发（原用"临时战果"，雷罚/平加会错位）；回响取高后
 *   单次重算总点/牌型/战果（协同计数随装备不随掷值，无需重评）；反应检测在重跑之外；
 * - 兑现：maxAll 保留加算/乘算/协同计数；Miss 不消耗兑现（重新武装）；命定 ×2×scale；
 *   守方归零不加幸运；追击并入 ×scale 乘区；
 * - 定命骰：防御路径消费后清除（原无限生效）；
 * - 破势骰：仅在"将破"时掷骰（自然击破也触发），命破加成 5/8/10%；
 * - 怪物攻击"崩"= Miss（取消事件 0 伤害）；防御骰势 +5/+8/+2；换势硬直受伤 +10%；
 * - 骰纹全量接线（锋/烈/韧/迅/贪/咒裂面）；命纹接线（回响命/逆命/连命）；加算骰·锋；
 * - 骰包按玩家骰位裁剪 + Boss 命压（命运包取高减半、时长 -1）；包骰不参与协同；
 * - Boss 机制计数按攻击实例（横扫不再加速）；sharedAttacks 过期清理。
 */
public final class ContestService {

    /** 多目标共享攻击掷骰缓存：attacker UUID → (gameTime, 纯净掷骰, 目标计数)。 */
    private static final Map<UUID, SharedAttack> sharedAttacks = new HashMap<>();

    private record SharedAttack(long tick, RollResult roll, int targets) {}

    /** 命中后的待结算（LivingDamageEvent.Post 应用命势）。 */
    public record PendingHit(Mob target, ServerPlayer attacker, int poiseDamage, int playerTotal,
                             float decreeScale, Outcome outcome, PatternType pattern,
                             boolean cashoutUsed, float cashoutScale, long createdTick) {}

    /** L2：过期清理（伤害被取消/实体卸载时防滞留；含 sharedAttacks）。 */
    public static void purgePending(long tick) {
        PENDING.removeIf(h -> tick - h.createdTick() > 40);
        sharedAttacks.values().removeIf(sa -> tick - sa.tick() > 100);
    }

    private static final List<PendingHit> PENDING = new ArrayList<>();

    private ContestService() {}

    // =====================================================================
    // 攻击拼点：玩家有源伤害（目标为 Mob 或其他 LivingEntity）
    // =====================================================================

    public static void resolvePlayerAttack(LivingIncomingEventLike event, ServerPlayer attacker,
                                           LivingEntity target, DamageSource src) {
        long tick = attacker.level().getGameTime();
        PlayerFateData data = attacker.getData(ModAttachments.PLAYER_FATE);
        Mob mob = target instanceof Mob m ? m : null;
        boolean targetIsMob = mob != null;
        MobFateData mobData = targetIsMob ? ensureMobData(mob, attacker) : null;
        boolean bossOrElite = targetIsMob && ContestRules.isBossOrElite(mob);
        RandomSource rng = attacker.level().getRandom();
        RecursionGuard.enter();
        try {
            data.markContest(tick);
            if (targetIsMob) mobData.setLastCombatTick(tick);

            // —— 1. 快照（骰包替换 + 命运包取高系数，Boss 命压减半） ——
            double coRoll = 0;
            if (data.activePack() != null) {
                var pack = attacker.level().registryAccess().lookup(DFRegistries.PACK)
                    .flatMap(r -> r.get(ResourceKey.create(DFRegistries.PACK, data.activePack())))
                    .map(h -> h.value()).orElse(null);
                if (pack != null && pack.mode() == PackDefinition.Mode.FATE) {
                    coRoll = pack.coRoll();
                    if (bossOrElite && DFConfig.PACK_BOSS_PRESSURE.get()) coRoll *= 0.5;
                }
            }
            PoolSnapshot atkSnap = SnapshotFactory.playerSnapshot(attacker, data, true, coRoll);
            PoolSnapshot defSnap = targetIsMob
                ? SnapshotFactory.mobSnapshot(mob, mobData, false)
                : SnapshotFactory.genericSnapshot(target);

            // —— 2. 共享攻击掷骰（纯净副本；逐目标 copy） ——
            SharedAttack cached = sharedAttacks.get(attacker.getUUID());
            boolean firstOfAttack = cached == null || cached.tick() != tick;
            RollResult pristine;
            if (firstOfAttack) {
                pristine = DiceRoller.roll(atkSnap, rng);
                sharedAttacks.values().removeIf(sa -> sa.tick() < tick - 1);
            } else {
                pristine = cached.roll();
            }

            // —— 3. 兑现（max vs 0；保留加算/乘算/协同计数——0.3.0 A4） ——
            List<Byte> triggers = new ArrayList<>();
            boolean cashoutUsed = false;
            float cashoutScale = 1.0f;
            int cashoutN = 0;
            if (data.isCashoutArmed() && tick <= data.cashoutExpireTick()) {
                cashoutUsed = true;
                cashoutN = data.cashoutValue();
                cashoutScale = Math.max(0.05f, cashoutN / 100.0f);
                pristine = maxAll(pristine);
                data.disarmCashout();
                triggers.add((byte) ContestPayload.Trigger.CASHOUT.ordinal());
            }

            // —— 4. 掷骰后修正（每攻击实例一次，施加于纯净副本供所有目标共享） ——
            if (firstOfAttack && !cashoutUsed) {
                // 保底取高
                if (PityService.shouldCoRoll(data, bossOrElite, rng)) {
                    RollResult second = DiceRoller.roll(atkSnap, rng);
                    if (totalOf(second) > totalOf(pristine)) {
                        for (DieRollView v : second.baseRolls) v.pityRolled = true;
                        pristine = second;
                        triggers.add((byte) ContestPayload.Trigger.PITY.ordinal());
                    }
                }
                // 幸运骰坠：每场 1 次重掷一颗 1
                if (CuriosCompat.hasLuckyPendant(attacker) && !data.isPendantUsedThisBattle()) {
                    for (DieRollView v : pristine.baseRolls) {
                        if (v.value == 1 && v.face > 1) {
                            v.value = 1 + rng.nextInt(v.face);
                            v.nudged = true;
                            pristine.rawTotal = recomputeBaseTotal(pristine);
                            data.markPendantUsed();
                            triggers.add((byte) ContestPayload.Trigger.LUCKY.ordinal());
                            break;
                        }
                    }
                }
                // 咒纹：8% 裂面（本场禁用；磨损置满）
                applyCursedCrack(attacker, data, pristine, rng);
                // 骰灵自动修正（最低骰 +nudge）
                if (data.isSpiritActive()) {
                    int nudge = SpiritService.tryNudge(attacker, data.spiritLevel(), rng);
                    if (nudge > 0 && !pristine.baseRolls.isEmpty()) {
                        DieRollView lowest = null;
                        for (DieRollView v : pristine.baseRolls) {
                            if (lowest == null || v.value < lowest.value) lowest = v;
                        }
                        lowest.value = Math.min(lowest.face, Math.max(1, lowest.value + nudge));
                        lowest.nudged = true;
                        pristine.rawTotal = recomputeBaseTotal(pristine);
                        triggers.add((byte) ContestPayload.Trigger.SPIRIT.ordinal());
                    }
                }
                // 回响命（命纹）：失败 10% 重掷最低骰（每场 1 次）——技能阶段之前
                if (data.playerRune() != null && data.playerRune().getPath().equals("rune_reroll")
                    && !data.isRerollRuneUsed() && !pristine.baseRolls.isEmpty() && rng.nextDouble() < 0.10) {
                    DieRollView lowest = null;
                    for (DieRollView v : pristine.baseRolls) {
                        if (lowest == null || v.value < lowest.value) lowest = v;
                    }
                    lowest.value = 1 + rng.nextInt(lowest.face);
                    lowest.nudged = true;
                    pristine.rawTotal = recomputeBaseTotal(pristine);
                    data.setRerollRuneUsed();
                    triggers.add((byte) ContestPayload.Trigger.LOCK.ordinal());
                }
                // 写回 lastRoll（定命 3.6 可用化）
                writeBackLastRoll(attacker, data, pristine);
            }
            if (firstOfAttack) {
                sharedAttacks.put(attacker.getUUID(), new SharedAttack(tick, pristine, 1));
            } else {
                int n = cached.targets() + 1;
                sharedAttacks.put(attacker.getUUID(), new SharedAttack(cached.tick(), cached.roll(), n));
            }

            RollResult atkRoll = copyRoll(pristine);
            com.srqingchen.dicefate.util.DFDebug.log("攻击掷骰: 骰数={} 各骰={} 原始总点={}",
                atkRoll.baseRolls.size(),
                atkRoll.baseRolls.stream().map(v -> v.value + "/" + v.face)
                    .collect(java.util.stream.Collectors.joining(",")),
                totalOf(atkRoll));

            // —— 5. 防御掷骰（兑现时目标视为全 0） ——
            RollResult defRoll = cashoutUsed ? zeroAll(defSnap) : DiceRoller.roll(defSnap, rng);

            com.srqingchen.dicefate.util.DFDebug.log("防御掷骰: 骰数={} 各骰={} 总点={}",
                defRoll.baseRolls.size(),
                defRoll.baseRolls.stream().map(v -> v.value + "/" + v.face)
                    .collect(java.util.stream.Collectors.joining(",")),
                totalOf(defRoll));

            // —— 5.5 命主之骰：每场 1 次复制对手最高骰面值（0.3.0 F2） ——
            if (!cashoutUsed && hasUniqueDie(data, "unique_die_fate_sovereign")) {
                applySovereignCopy(data, atkRoll, defRoll);
            }

            // —— 6. Boss 机制（按攻击实例一次：计数与硬币/熔铸不随横扫目标数加速） ——
            if (targetIsMob && firstOfAttack) {
                applyBossMechanics(mob, mobData, atkRoll, defRoll, data, triggers, rng);
            }

            // —— 7. 协同 / 元素反应（骰序；CD 消耗在重跑之外） ——
            SynergyResult syn = SynergyService.evaluate(atkRoll,
                attacker.level().registryAccess().lookupOrThrow(DFRegistries.SYNERGY));
            ReactionDefinition reaction = null;
            if (!syn.monoLock()) {
                reaction = findReaction(attacker, atkRoll, data, syn, tick);
            }

            // —— 8. 总点数 → 战果 → 技能骰（回响后单次重算；0.3.0 A7） ——
            int guardMinus = data.nextContestMinusTotal();
            if (guardMinus != 0) data.setNextContestMinusTotal(0);   // 玩家 debuff：扣攻方总点
            int luckA = LuckService.shift(attacker);
            int luckD = cashoutUsed ? 0 : (targetIsMob ? mobLuck(mob) : 0);   // 兑现归零守方不吃幸运
            int synFlat = syn.flatTotal() + MomentumService.flatTotal(data.getMomentum())
                + (data.aimCharges() > 0 ? 1 : 0) + weaponTotalBonus(attacker) - guardMinus + luckA;
            int atkTotal = Math.max(0, totalOf(atkRoll) + synFlat);
            int defTotal = Math.max(0, totalOf(defRoll) + luckD);
            Outcome outcome = ContestMath.classify(atkTotal, defTotal);
            com.srqingchen.dicefate.util.DFDebug.log("总点判定: A={} D={} 初判战果={}", atkTotal, defTotal, outcome);

            // 技能骰（战果后触发；可能改写掷骰/结算）
            AttackFx fx = new AttackFx();
            skillDiceOnAttack(attacker, data, atkSnap, atkRoll, outcome, fx, triggers, rng, tick);
            if (fx.echoed) {
                atkTotal = Math.max(0, totalOf(atkRoll) + synFlat);
                outcome = ContestMath.classify(atkTotal, defTotal);
            }

            // —— 9. 牌型（回响重掷后的最终值） ——
            PatternType pattern = PatternService.detect(atkRoll.baseRolls);
            if (pattern != PatternType.NONE) {
                triggers.add((byte) (ContestPayload.Trigger.PATTERN_STRAIGHT.ordinal() + pattern.ordinal() - 1));
            }

            double r = ContestMath.ratio(atkTotal, defTotal);
            double rEff = r;
            // 湮灭：平 → 胜 r=0.25
            if (reaction != null && reaction.effect() == ReactionDefinition.Effect.NULLIFY_TIE && outcome == Outcome.TIE) {
                outcome = Outcome.WIN;
                r = 0.25;
                rEff = 0.25;
                triggers.add((byte) ContestPayload.Trigger.REACTION.ordinal());
            }
            // 蒸汽：差率压缩（乘算骰·元放大反应效果）
            double resBoost = mulResonanceBonus(atkRoll);
            if (reaction != null && reaction.effect() == ReactionDefinition.Effect.COMPRESS_MARGIN) {
                rEff = r * (1.0 - Math.min(0.9, reaction.value() * (1 + resBoost)));
            }
            // 逆命骰改写：崩 → 负（技能阶段之后、伤害分支之前）
            if (fx.reversePct > 0 && outcome == Outcome.CRASH) {
                outcome = Outcome.LOSE;
                triggers.add((byte) ContestPayload.Trigger.SKILL_REVERSE.ordinal());
            }
            // 逆命命途：崩 15% 改写为负（40% 伤害，每场 1 次）
            boolean journeyReverse = false;
            if (outcome == Outcome.CRASH && data.playerRune() != null
                && data.playerRune().getPath().equals("rune_reverse")
                && !data.isReverseRuneUsed() && rng.nextDouble() < 0.15) {
                outcome = Outcome.LOSE;
                journeyReverse = true;
                data.setReverseRuneUsed();
                triggers.add((byte) ContestPayload.Trigger.SKILL_REVERSE.ordinal());
            }

            // —— 10. 伤害管线 ——
            float amount = event.getAmount();
            float finalDamage;
            int poiseDamage = 0;
            int decreeGain = 0;
            float decreeScale = 1.0f;

            if (atkTotal <= 0 && defTotal <= 0) {
                // 双方无骰/全碎裂：平，伤害 0（策划 2.4 特殊条款）
                event.setAmount(0);
                finalDamage = 0;
                outcome = Outcome.TIE;
            } else if (outcome == Outcome.CRASH) {
                // 崩：取消事件，伤害 0；冷却照常
                // 0.3.0 C5：Miss 不消耗兑现（双面赌徒交换等 Boss 机制导致的崩）——重新武装
                if (cashoutUsed) {
                    data.armCashout(cashoutN, Math.max(tick, data.cashoutExpireTick()));
                    triggers.remove((Byte) (byte) ContestPayload.Trigger.CASHOUT.ordinal());
                }
                event.setCanceled(true);
                if (firstOfAttack) {
                    MomentumService.applyLoss(attacker, data, outcome, tick);
                    if (!cashoutUsed) PityService.onAttackResult(data, outcome);
                    consumeAttackResources(attacker, data, tick, bossOrElite);
                }
                sendContest(attacker, target, 0, atkRoll, defRoll, atkTotal, defTotal, luckA, luckD,
                    outcome, 0, 0, 0, triggers, sharedTargets(attacker, tick));
                combatText(target, CombatTextPayload.TextType.MISS, 0, (byte) 5);
                afterResolve(attacker, data, tick);
                return;
            } else {
                double dmg;
                if (fx.reversePct > 0 && outcome == Outcome.LOSE) {
                    // 逆命骰：基础伤害 ×param%，不吃曲线/穿透
                    dmg = amount * fx.reversePct / 100.0;
                } else if (journeyReverse) {
                    dmg = amount * 0.40;
                } else {
                    double curve = ContestMath.attackCurve(data.stance(), outcome == Outcome.TIE ? 0 : rEff);
                    curve = ContestMath.withMastery(curve, data.stanceMastery(data.stance()));
                    if (outcome == Outcome.TIE) curve = DFConfig.TIE_COEFFICIENT.get();

                    // 增伤桶（乘法：B × C(r) × (1+桶)，策划 2.5.1）
                    double bucket = 0;
                    bucket += pattern.damageBonus;
                    bucket += packDamageBonus(attacker, data);
                    bucket += syn.damageBonus();
                    if (syn.monoLock()) bucket += 0.15;   // 纯色：元素被动 +15%（简化，0.3.0）
                    bucket += MomentumService.damageBonus(data.getMomentum());
                    bucket += fx.greedBonus;
                    if (outcome.isGreat()) bucket += fierceRuneBonus(atkSnap);   // 烈：大胜 +5%
                    double reactionBonus = reaction != null ? reaction.bonusDamage() : 0;
                    reactionBonus *= (1 + syn.reactionEffectMod() + resBoost);
                    bucket += reactionBonus;
                    bucket += playerRuneDamageBonus(data);
                    bucket = Math.min(bucket, DFConfig.ADD_BUCKET_CAP.get());

                    // 易伤（命损）
                    double vuln = mobData != null && mobData.hasWound() ? DFConfig.WOUND_BONUS.get() : 0;
                    vuln = Math.min(vuln, DFConfig.VULN_CAP.get());

                    // 协同基础伤害修正（四元 -5%）
                    double baseMod = 1 + syn.baseDamageMod();

                    dmg = amount * baseMod * curve * (1 + bucket) * (1 + vuln);

                    // 乘算骰（胜利后放大，分族系数）
                    dmg *= mulAttackMultiplier(atkRoll, outcome, pattern);

                    // 穿透（CombatRules 代偿）
                    double pen = ContestMath.penetration(rEff, outcome)
                        + pattern.pierceBonus
                        + mulPierceBonus(atkRoll, outcome)
                        + pierceRuneBonus(data);
                    pen = ContestMath.clamp(pen, 0, 0.60);
                    dmg = applyPenCompensation(target, dmg, pen);
                }

                // 兑现乘算
                if (cashoutUsed) {
                    double mult = mobData != null && mobData.hasWound()
                        ? DFConfig.CASHOUT_WOUND_DAMAGE_MULT.get() : DFConfig.CASHOUT_DAMAGE_MULT.get();
                    if (bossOrElite && DFConfig.PACK_BOSS_PRESSURE.get()) mult = 1.0 + (mult - 1.0) * 0.5;
                    dmg *= mult * cashoutScale;
                }

                com.srqingchen.dicefate.util.DFDebug.log("伤害管线: 基础={} r={} 战果={} 穿透前伤害={}",
                    amount, String.format("%.2f", r), outcome, String.format("%.1f", dmg));
                // 上限（以原始基础伤害为基）+ 取整
                dmg = Math.min(dmg, amount * DFConfig.MAX_DAMAGE_CAP.get());
                finalDamage = Math.max(1, (float) Math.floor(dmg));
                event.setAmount(finalDamage);

                // 怪物护命骰（守方失败减伤）
                if (targetIsMob && outcome.isWin()) {
                    float before = event.getAmount();
                    float after = mobWardSkill(mob, mobData, rng, tick, before);
                    if (after != before) {
                        event.setAmount(after);
                        finalDamage = after;
                        triggers.add((byte) ContestPayload.Trigger.SKILL_WARD.ordinal());
                    }
                }

                // 爆燃反应：胜利时真伤 + 自损
                if (reaction != null && reaction.effect() == ReactionDefinition.Effect.TRUE_DAMAGE && outcome.isWin()) {
                    triggerDetonate(attacker, target, reaction, bossOrElite, resBoost);
                    triggers.add((byte) ContestPayload.Trigger.REACTION.ordinal());
                }
                // 腐蚀反应：胜利时护甲 -15% 6s（乘算骰·元放大）
                if (reaction != null && reaction.effect() == ReactionDefinition.Effect.CORRODE && outcome.isWin() && targetIsMob) {
                    PoiseService.applyCorrode(mob, mobData, Math.min(0.5, reaction.value() * (1 + resBoost)), 120);
                    triggers.add((byte) ContestPayload.Trigger.REACTION.ordinal());
                }
                // 乘算骰·命：大胜真伤触发
                applyFateMulTrueDamage(attacker, target, atkRoll, outcome, rng, triggers);

                // 唯一骰胜利附带效果（熔核燃烧/霜寂减速/蚀骨减甲/雷罚连锁，0.3.0 F2）
                applyUniqueWinEffects(attacker, data, target, mobData, outcome, finalDamage, rng, triggers);

                // —— 11. 命势伤害（伤害落地后结算，先记 Pending） ——
                if (targetIsMob && mobData.isInitialized()) {
                    double poiseMult = outcome.isWin() ? 1.2 : outcome == Outcome.TIE ? 1.0 : 0.8;
                    poiseMult += syn.poiseBonus() + MomentumService.poiseBonus(data.getMomentum());
                    poiseMult += fx.pursuitBonus;                       // 追击并入乘区（兑现 ×scale 一体化）
                    if (cashoutUsed) poiseMult *= 2 * Math.max(0.05, cashoutScale);
                    poiseMult *= mulPoiseMultiplier(atkRoll, outcome);
                    int flat = pattern.poiseBonus + syn.poiseFlat() + poiseFlatBonus(data, atkSnap, outcome);
                    int base = (int) Math.floor(atkTotal * poiseMult) + flat;
                    poiseDamage = Math.max(0, base);
                    decreeGain = atkTotal;
                    decreeScale = cashoutUsed ? 2.0f * cashoutScale : 1.0f;   // 0.3.0 B4：×2×scale
                }
            }

            // —— 12. 骰势 / 保底 / 每实例消耗 ——
            if (firstOfAttack) {
                // 0.3.0 F1：进度直授（拼点/战果/牌型/反应/协同/四元）
                com.srqingchen.dicefate.util.AdvancementGrant.grant(attacker, "first_contest");
                if (outcome.isWin()) com.srqingchen.dicefate.util.AdvancementGrant.grant(attacker, "first_win");
                if (outcome == Outcome.CRASH) com.srqingchen.dicefate.util.AdvancementGrant.grant(attacker, "first_miss");
                if (pattern == PatternType.STRAIGHT || pattern == PatternType.DESTINY_STRAIGHT) {
                    com.srqingchen.dicefate.util.AdvancementGrant.grant(attacker, "straight");
                }
                if (pattern == PatternType.TRIPLE || pattern == PatternType.DESTINY_TRIPLE) {
                    com.srqingchen.dicefate.util.AdvancementGrant.grant(attacker, "triple");
                }
                if (reaction != null && reaction.effect() == ReactionDefinition.Effect.COMPRESS_MARGIN) {
                    com.srqingchen.dicefate.util.AdvancementGrant.grant(attacker, "steam");
                }
                if (syn.active().size() >= 3) {
                    com.srqingchen.dicefate.util.AdvancementGrant.grant(attacker, "synergy_3");
                }
                int elemKinds = (int) atkRoll.elementCounts.entrySet().stream()
                    .filter(e -> e.getKey().isReal() && e.getValue() > 0).count();
                if (elemKinds >= 4) {
                    com.srqingchen.dicefate.util.AdvancementGrant.grant(attacker, "four_elements");
                }
                int comboGain = addDieIsCombo(data) ? 3 : 0;
                if (outcome.isWin()) {
                    int gain = MomentumService.gainFor(outcome, CuriosCompat.hasGamblerCharm(attacker)) + comboGain;
                    // 连命（命纹）：胜利骰势 +2
                    if (data.playerRune() != null && data.playerRune().getPath().equals("rune_combo")) gain += 2;
                    if (!cashoutUsed) data.setMomentum(data.getMomentum() + gain, tick);
                    // 兑现命中：不获取、不清溢出缓存（0.2.0 A11）
                    // 加算骰·愈：胜利回 1 血（每攻击实例 1 次 ≈ 每拼点 ≤2 上限内）
                    if (addDieIsMend(data)) attacker.heal(1.0F);
                } else if (outcome == Outcome.TIE) {
                    if (!cashoutUsed) data.setMomentum(data.getMomentum() + 2, tick);
                } else {
                    MomentumService.applyLoss(attacker, data, outcome, tick);
                    // 影噬骰：失败时骰势 +5（策划 5.1.6，0.3.0 F2）
                    if (hasUniqueDie(data, "unique_die_umbral_veil")) {
                        data.setMomentum(data.getMomentum() + 5, tick);
                    }
                }
                if (!cashoutUsed) PityService.onAttackResult(data, outcome);
                consumeAttackResources(attacker, data, tick, bossOrElite);
                // 豹子/天命豹子 CD 返还
                refundSkillCdOnTriple(data, pattern, tick);
                // 迅纹：攻击冷却 -3%/层（≤2 层）
                applySwiftCooldown(attacker, atkSnap);
            }

            // —— 13. HUD ——
            com.srqingchen.dicefate.util.DFDebug.always("攻击拼点完成: {} vs {} A={} D={} 战果={} 伤害={} 命势伤害={}",
                attacker.getName().getString(), target.getName().getString(),
                atkTotal, defTotal, outcome, finalDamage, poiseDamage);
            sendContest(attacker, target, 0, atkRoll, defRoll, atkTotal, defTotal, luckA, luckD,
                outcome, finalDamage, poiseDamage, (int) Math.ceil(decreeGain * decreeScale), triggers,
                sharedTargets(attacker, tick));
            combatText(target, CombatTextPayload.TextType.DAMAGE, (int) Math.ceil(finalDamage),
                (byte) (outcome.isGreat() ? 3 : outcome.isWin() ? 2 : outcome == Outcome.TIE ? 1 : 4));

            if (poiseDamage > 0 && targetIsMob) {
                PENDING.add(new PendingHit(mob, attacker, poiseDamage, atkTotal, decreeScale, outcome, pattern,
                    cashoutUsed, cashoutScale, tick));
            }
            afterResolve(attacker, data, tick);
        } finally {
            RecursionGuard.exit();
        }
    }

    /** 结算收尾（每目标）：同步与冰封递减。 */
    // （诊断日志在 resolvePlayerAttack 尾部发送 HUD 时输出）
    private static void afterResolve(ServerPlayer attacker, PlayerFateData data, long tick) {
        data.decrementFrozen();
        DFNetwork.sendFateState(attacker, data, tick);
    }

    /** 每攻击实例一次的资源消耗（A18）：定命、药剂、骰包、磨损。 */
    private static void consumeAttackResources(ServerPlayer attacker, PlayerFateData data, long tick,
                                               boolean bossOrElite) {
        if (data.lockSlot() >= 0) data.spendLock();
        if (data.skillLockSlot() >= 0 && data.isSkillLockConsumed()) data.clearSkillLock();
        data.consumeAim();
        if (data.activePack() != null) {
            data.decrementPack();
            // Boss 命压：骰包持续时间 -1（最低 1 剩余由 decrementPack 的置空保护）
            if (bossOrElite && DFConfig.PACK_BOSS_PRESSURE.get() && data.packRemaining() > 1) {
                data.decrementPack();
            }
        } else {
            addWear(attacker, data);
        }
    }

    private static void addWear(ServerPlayer player, PlayerFateData data) {
        for (ItemStack s : data.pool().allDice()) {
            if (!(s.getItem() instanceof com.srqingchen.dicefate.item.DiceItem dice)) continue;
            var def = com.srqingchen.dicefate.dice.DiceLookup.definition(player.level().registryAccess(), dice.dieId);
            if (def == null) continue;
            var state = s.get(ModDataComponents.DIE_STATE.get());
            int max = def.material().baseWear();
            if (state == null || state.wear() >= max) continue;
            s.set(ModDataComponents.DIE_STATE.get(), state.withWear(state.wear() + 1));
        }
    }

    /** 定命 3.6：把基础骰掷出值写回 DieState.lastRoll（骰包期间跳过）。 */
    private static void writeBackLastRoll(ServerPlayer attacker, PlayerFateData data, RollResult roll) {
        if (data.activePack() != null) return;
        for (DieRollView v : roll.baseRolls) {
            if (v.slotIndex < 0 || v.slotIndex >= 6) continue;
            ItemStack stack = data.pool().baseAt(v.slotIndex);
            if (stack.isEmpty() || !(stack.getItem() instanceof com.srqingchen.dicefate.item.DiceItem)) continue;
            var state = stack.get(ModDataComponents.DIE_STATE.get());
            if (state != null && v.value > 0) {
                stack.set(ModDataComponents.DIE_STATE.get(), state.withLastRoll(v.value));
            }
        }
    }

    /** 咒纹裂面 + 唯一骰掷后修正（每攻击实例一次，0.3.0 F2）。 */
    private static void applyCursedCrack(ServerPlayer attacker, PlayerFateData data, RollResult roll,
                                         RandomSource rng) {
        if (data.activePack() != null) return;
        boolean any = false;
        for (DieRollView v : roll.baseRolls) {
            if (v.slotIndex < 0 || v.slotIndex >= 6) continue;
            ItemStack stack = data.pool().baseAt(v.slotIndex);
            if (stack.isEmpty() || !(stack.getItem() instanceof com.srqingchen.dicefate.item.DiceItem diceItem)) continue;
            var def = com.srqingchen.dicefate.dice.DiceLookup.definition(
                attacker.level().registryAccess(), diceItem.dieId);
            if (def == null) continue;
            var state = stack.get(ModDataComponents.DIE_STATE.get());
            String path = def.id().getPath();
            // 裂面者之骰：掷 1 视为 6；10% 裂面（本场禁用）
            if ("unique_die_cracked_face".equals(path) && v.value == 1 && v.face >= 6) {
                v.value = 6;
                v.nudged = true;
                any = true;
                if (state != null && rng.nextDouble() < 0.10) {
                    data.markCrackedThisBattle(state.uuid());
                    stack.set(ModDataComponents.DIE_STATE.get(), state.withWear(def.material().baseWear()));
                }
            }
            // 双面赌徒之骰：每场 1 次，掷出后翻到对面（f+1-roll）
            if ("unique_die_two_faced".equals(path) && !data.isTwoFacedUsedThisBattle() && v.value > 0) {
                v.value = v.face + 1 - v.value;
                v.nudged = true;
                data.markTwoFacedUsed();
                any = true;
            }
            // 咒纹：8% 裂面（该骰本场禁用；磨损置满显示裂纹）
            if (state != null && state.runeId() == DieRune.CURSED) {
                if (data.isCrackedThisBattle(state.uuid())) {
                    v.value = 0;
                    v.crashed = true;
                    any = true;
                } else if (rng.nextDouble() < 0.08) {
                    data.markCrackedThisBattle(state.uuid());
                    stack.set(ModDataComponents.DIE_STATE.get(), state.withWear(def.material().baseWear()));
                    v.value = 0;
                    v.crashed = true;
                    any = true;
                }
            }
        }
        if (any) roll.rawTotal = recomputeBaseTotal(roll);
    }

    /** 命主之骰：每场 1 次，复制对手最高骰面值代替掷骰（逐目标，在防御掷骰后）。 */
    private static void applySovereignCopy(PlayerFateData data, RollResult atkRoll, RollResult defRoll) {
        if (data.isSovereignUsedThisBattle() || data.activePack() != null) return;
        int bestDef = 0;
        for (DieRollView d : defRoll.baseRolls) bestDef = Math.max(bestDef, d.value);
        if (bestDef <= 0) return;
        DieRollView best = null;
        for (DieRollView v : atkRoll.baseRolls) {
            if (best == null || v.face > best.face) best = v;
        }
        if (best == null) return;
        best.value = Math.min(best.face, bestDef);   // 0.3.0 验收风险⑦：封顶本骰面数
        best.nudged = true;
        atkRoll.rawTotal = recomputeBaseTotal(atkRoll);
        data.markSovereignUsed();
    }

    /** 装备中是否持有指定唯一骰。 */
    private static boolean hasUniqueDie(PlayerFateData data, String path) {
        for (ItemStack s : data.pool().allDice()) {
            if (s.getItem() instanceof com.srqingchen.dicefate.item.DiceItem d
                && path.equals(d.dieId.getPath())) return true;
        }
        return false;
    }

    /** 唯一骰胜利附带效果（0.3.0 F2，策划 5.1.6）：熔核燃烧/霜寂减速/蚀骨减甲/雷罚连锁。 */
    private static void applyUniqueWinEffects(ServerPlayer attacker, PlayerFateData data,
                                              LivingEntity target, MobFateData mobData,
                                              Outcome outcome, float damage, RandomSource rng,
                                              List<Byte> triggers) {
        if (!outcome.isWin()) return;
        if (hasUniqueDie(data, "unique_die_molten_core")) {
            target.igniteForSeconds(2.0F);
        }
        if (hasUniqueDie(data, "unique_die_frost_silence")) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.SLOWNESS, 40, 0));
        }
        if (hasUniqueDie(data, "unique_die_bone_eater") && mobData != null) {
            com.srqingchen.dicefate.combat.PoiseService.applyCorrode(
                (Mob) target, mobData, 0.10, 80);
        }
        if (outcome.isGreat() && hasUniqueDie(data, "unique_die_storm_verdict")
            && damage > 0 && target.level() instanceof ServerLevel level) {
            // 连锁：对目标 4 格内另一名敌对生物造成 20% 伤害（递归保护）
            Mob chain = null;
            double best = 16.0;
            for (Mob m : level.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(4))) {
                if (m == target || !m.isAlive()) continue;
                double d2 = m.distanceToSqr(target);
                if (d2 < best) { best = d2; chain = m; }
            }
            if (chain != null) {
                RecursionGuard.enter();
                try {
                    chain.invulnerableTime = 0;
                    chain.hurt(attacker.damageSources().magic(), Math.max(1, damage * 0.2F));
                } finally {
                    RecursionGuard.exit();
                }
                combatText(chain, CombatTextPayload.TextType.DAMAGE,
                    (int) Math.ceil(damage * 0.2F), (byte) 2);
            }
        }
    }

    // =====================================================================
    // 防御拼点：怪物攻击玩家
    // =====================================================================

    public static void resolveMobAttackOnPlayer(LivingIncomingEventLike event, Mob attacker, ServerPlayer defender,
                                                float amount, DamageSource src) {
        long tick = defender.level().getGameTime();
        PlayerFateData data = defender.getData(ModAttachments.PLAYER_FATE);
        MobFateData mobData = ensureMobData(attacker, defender);
        RandomSource rng = defender.level().getRandom();
        RecursionGuard.enter();
        try {
            data.markContest(tick);
            mobData.setLastCombatTick(tick);

            boolean light = ContestRules.isLightContest(src);
            PoolSnapshot atkSnap = SnapshotFactory.mobSnapshot(attacker, mobData, light);
            PoolSnapshot defSnap = SnapshotFactory.playerSnapshot(defender, data, false, 0);

            RollResult atkRoll = DiceRoller.roll(atkSnap, rng);
            RollResult defRoll = DiceRoller.roll(defSnap, rng);

            List<Byte> triggers = new ArrayList<>();
            // 骰灵自动修正（防御）
            if (data.isSpiritActive()) {
                int nudge = SpiritService.tryNudge(defender, data.spiritLevel(), rng);
                if (nudge > 0 && !defRoll.baseRolls.isEmpty()) {
                    DieRollView lowest = null;
                    for (DieRollView v : defRoll.baseRolls) {
                        if (lowest == null || v.value < lowest.value) lowest = v;
                    }
                    lowest.value = Math.min(lowest.face, Math.max(1, lowest.value + nudge));
                    lowest.nudged = true;
                    defRoll.rawTotal = recomputeBaseTotal(defRoll);
                    triggers.add((byte) ContestPayload.Trigger.SPIRIT.ordinal());
                }
            }

            // 防御协同
            SynergyResult syn = SynergyService.evaluate(defRoll,
                defender.level().registryAccess().lookupOrThrow(DFRegistries.SYNERGY));

            // 壁纹：防御时本骰 +1（最多3层）
            int wallRune = countRune(defSnap, DieRune.WALL);

            int mobTotal = totalOf(atkRoll);
            int defTotal = totalOf(defRoll) + syn.flatTotal() + syn.flatDefense()
                + shieldBonus(defender) + wallRune + (data.aimCharges() > 0 ? 1 : 0);

            int luckM = mobLuck(attacker);
            int luckP = LuckService.shift(defender);
            mobTotal = Math.max(0, mobTotal + luckM);
            defTotal = Math.max(0, defTotal + luckP);

            double q = mobTotal <= 0 ? 1.0 : (double) (defTotal - mobTotal) / mobTotal;

            // —— 怪物技能骰（回响重掷 / 逆命改写） ——
            double mobReversePct = 0;
            boolean mobCrash = defTotal > 0 && mobTotal < DFConfig.MISS_THRESHOLD.get() * defTotal;   // 0.3.0 B7：P=0 不再挡破防判定
            boolean mobLose = !mobCrash && defTotal > mobTotal;
            if (mobCrash || mobLose) {
                RollResult rerolled = mobEchoSkill(attacker, mobData, atkSnap, rng, tick, triggers);
                if (rerolled != null && totalOf(rerolled) > totalOf(atkRoll)) {
                    atkRoll = rerolled;
                    mobTotal = Math.max(0, totalOf(atkRoll) + luckM);
                    q = mobTotal <= 0 ? 1.0 : (double) (defTotal - mobTotal) / mobTotal;
                    mobCrash = defTotal > 0 && mobTotal < DFConfig.MISS_THRESHOLD.get() * defTotal;
                }
            }

            // 换势硬直：受伤 +10%（0.3.0 B6）
            double hardStiff = tick < data.swapHardUntilTick() ? 1.10 : 1.0;

            Outcome defOutcome;

            if (mobCrash) {
                mobReversePct = mobReverseSkill(attacker, mobData, rng, tick, triggers);
            }
            if (mobCrash && mobReversePct <= 0) {
                // 0.3.0 A6：怪物"崩"= Miss——取消事件、0 伤害（策划 2.7.1 攻守对称）
                event.setCanceled(true);
                defOutcome = Outcome.GREAT_WIN;   // 玩家视角：大守（+8 骰势档）
                data.setMomentum(data.getMomentum() + 8, tick);
                sendContest(attacker, defender, 1, atkRoll, defRoll, mobTotal, defTotal, luckM, luckP,
                    defOutcome, 0, 0, 0, triggers, 1);
                combatText(defender, CombatTextPayload.TextType.MISS, 0, (byte) 5);
            } else if (mobCrash) {
                // 怪物逆命骰：崩 → 负，param% 基础伤害
                double dmg = amount * mobReversePct / 100.0 * hardStiff;
                double bucket = reduceBucket(data) + (CuriosCompat.hasGamblerCharm(defender) ? -0.05 : 0);
                dmg *= (1 - Math.min(DFConfig.REDUCE_BUCKET_CAP.get(), Math.max(0, bucket)));
                event.setAmount((float) Math.max(1, dmg));
                defOutcome = Outcome.LOSE;
                sendContest(attacker, defender, 1, atkRoll, defRoll, mobTotal, defTotal, luckM, luckP,
                    defOutcome, event.getAmount(), 0, 0, triggers, 1);
            } else if (!mobCrash && mobReversePct <= 0 && defTotal < DFConfig.MISS_THRESHOLD.get() * mobTotal) {
                // 破防：×1.60，盾牌禁用2s，骰势减半
                double dmg = amount * 1.60 * hardStiff;
                // 护命骰：防御失败 → 受伤 -20/30/40%
                dmg *= wardSkillMult(defender, data, rng, tick, triggers);
                double bucket = reduceBucket(data) + (CuriosCompat.hasGamblerCharm(defender) ? -0.05 : 0);
                dmg *= (1 - Math.min(DFConfig.REDUCE_BUCKET_CAP.get(), Math.max(0, bucket)));
                event.setAmount((float) Math.max(1, dmg));
                int half = data.getMomentum() / 2;
                data.setMomentum(half, tick);
                disableShield(defender, 40);
                triggers.add((byte) ContestPayload.Trigger.SHIELD_BREAK.ordinal());
                sendContest(attacker, defender, 1, atkRoll, defRoll, mobTotal, defTotal, luckM, luckP,
                    Outcome.CRASH, event.getAmount(), 0, 0, triggers, 1);
                defOutcome = Outcome.CRASH;
            } else {
                double t = ContestMath.defenseCurve(data.stance(), q == 0 ? 0 : q);
                // 完美格挡（需有盾；无盾时下限 25%/守势 20%）
                double threshold = ContestMath.perfectBlockThreshold(data.stance()) + shieldThresholdBonus(defender);
                boolean blocking = defender.isBlocking();
                if (q >= threshold) {
                    if (blocking) {
                        boolean boss = ContestRules.isBossOrElite(attacker);
                        if (boss && DFConfig.PACK_BOSS_PRESSURE.get()) {
                            event.setAmount((float) Math.max(1, amount * 0.25 * hardStiff));
                        } else {
                            event.setCanceled(true);
                            // 盾耐久 -1 + 击退攻击者
                            ItemStack shield = defender.getUseItem();
                            if (!shield.isEmpty()) {
                                shield.hurtAndBreak(1, defender, net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                            }
                            knockbackAttacker(defender, attacker);
                        }
                        triggers.add((byte) ContestPayload.Trigger.PERFECT_BLOCK.ordinal());
                        defOutcome = Outcome.DESTINY;
                        data.setMomentum(data.getMomentum() + 8, tick);   // 大守 +8（策划 3.2.2）
                        // 圣辉骰：完美格挡时恢复 2 生命（策划 5.1.6，0.3.0 F2）
                        if (hasUniqueDie(data, "unique_die_radiant_oath")) defender.heal(2.0F);
                    } else {
                        // 无盾：最低 25% 伤害（守势 20%）
                        event.setAmount((float) Math.max(1, amount
                            * (data.stance() == PoolProfile.Stance.GUARD ? 0.20 : 0.25) * hardStiff));
                        defOutcome = defTotal > mobTotal
                            ? (q >= 0.5 ? Outcome.GREAT_WIN : Outcome.WIN)
                            : defTotal == mobTotal ? Outcome.TIE : Outcome.LOSE;
                    }
                } else {
                    double dmg = amount * (q == 0 ? 0.85 : t) * hardStiff;
                    if (q < 0) {
                        dmg *= wardSkillMult(defender, data, rng, tick, triggers);   // 负时护命骰触发
                    }
                    // 乘算骰·壁：防御胜利受伤 -25%×roll/face
                    if (q > 0) dmg *= mulDefenseMultiplier(defRoll);
                    double bucket = reduceBucket(data) + (CuriosCompat.hasGamblerCharm(defender) ? -0.05 : 0);
                    dmg *= (1 - Math.min(DFConfig.REDUCE_BUCKET_CAP.get(), Math.max(0, bucket)));
                    event.setAmount((float) Math.max(1, dmg));
                    defOutcome = defTotal > mobTotal
                        ? (q >= 0.5 ? Outcome.GREAT_WIN : Outcome.WIN)
                        : defTotal == mobTotal ? Outcome.TIE : Outcome.LOSE;
                }
                // 防御骰势（策划 3.2.2 表格值：守胜 +5 / 大守 +8 / 平 +2）
                if (defOutcome == Outcome.WIN) {
                    data.setMomentum(data.getMomentum() + 5, tick);
                } else if (defOutcome == Outcome.GREAT_WIN || defOutcome == Outcome.DESTINY) {
                    data.setMomentum(data.getMomentum() + 8, tick);
                } else if (defOutcome == Outcome.TIE) {
                    data.setMomentum(data.getMomentum() + 2, tick);
                }
                // 腐蚀反应（防御半段）：守胜时攻击者攻击 -10% 4s（策划 4.7）
                if (defOutcome == Outcome.WIN || defOutcome == Outcome.GREAT_WIN || defOutcome == Outcome.DESTINY) {
                    applyDefensiveCorrode(defender, data, defRoll, attacker, syn, tick, triggers);
                }
                sendContest(attacker, defender, 1, atkRoll, defRoll, mobTotal, defTotal, luckM, luckP,
                    defOutcome, event.isCanceled() ? 0 : event.getAmount(), 0, 0, triggers, 1);
            }

            com.srqingchen.dicefate.util.DFDebug.always("防御拼点完成: {} 攻击 {} M={} P={} 战果={} 伤害={}",
                attacker.getName().getString(), defender.getName().getString(),
                mobTotal, defTotal, defOutcome, event.isCanceled() ? 0 : event.getAmount());
            mobData.incrementContest();

            if (!event.isCanceled()) {
                combatText(defender, CombatTextPayload.TextType.DAMAGE,
                    (int) Math.ceil(event.getAmount()), (byte) 0);
            }
            // 防御拼点消耗（对称）：冰封、药剂、骰包、定命（0.3.0：不磨损——磨损只随玩家有源伤害）
            data.decrementFrozen();
            data.consumeAim();
            if (data.activePack() != null) data.decrementPack();
            if (data.lockSlot() >= 0) data.spendLock();
            // 定命骰锁防御消费后清除（0.3.0 A1：原防御路径不清除 → 永久最大面）
            if (data.skillLockSlot() >= 0 && data.isSkillLockConsumed()) data.clearSkillLock();
            writeBackLastRoll(defender, data, defRoll);
            DFNetwork.sendFateState(defender, data, tick);
        } finally {
            RecursionGuard.exit();
        }
    }

    /** 腐蚀防御半段：守胜 → 攻击者攻击 -10% 4s。 */
    private static void applyDefensiveCorrode(ServerPlayer defender, PlayerFateData data, RollResult defRoll,
                                              Mob attacker, SynergyResult syn, long tick, List<Byte> triggers) {
        if (syn.monoLock()) return;
        var reg = defender.level().registryAccess().lookupOrThrow(DFRegistries.REACTION);
        for (DieRollView v : defRoll.baseRolls) {
            if (!v.element.isReal()) continue;
            for (ReactionDefinition def : reg) {
                if (def.effect() != ReactionDefinition.Effect.CORRODE || def.elements().size() != 2) continue;
                DieElement a = DieElement.byName(def.elements().get(0));
                DieElement b = DieElement.byName(def.elements().get(1));
                if (a != v.element && b != v.element) continue;
                DieElement other = a == v.element ? b : a;
                if (defRoll.elementCounts.getOrDefault(other, 0) <= 0 && !addDieIsElement(data)) continue;
                if (data.reactionCdRemaining(def.id(), tick) > 0) continue;
                int cdMod = (int) Math.max(20, def.cooldownTicks() * Math.max(0.2, 1 + syn.reactionCdMod()));
                if (CuriosCompat.hasElementResonator(defender)) cdMod = (int) (cdMod * 0.8);
                data.setReactionCd(def.id(), tick + cdMod);
                AttributeInstance atk = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
                if (atk != null) {
                    var id = com.srqingchen.dicefate.DiceFate.id("corrode_atk/" + attacker.getUUID());
                    atk.removeModifier(id);
                    atk.addTransientModifier(new AttributeModifier(id, -0.10,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    mobDataCorrodeAtkUntil(attacker, tick + 80);
                }
                triggers.add((byte) ContestPayload.Trigger.REACTION.ordinal());
                return;
            }
            break;   // 只看骰序第一颗元素骰
        }
    }

    private static void mobDataCorrodeAtkUntil(Mob mob, long until) {
        var data = mob.getData(ModAttachments.MOB_FATE);
        data.setCorrodeAtkUntil(until);   // 0.3.0 验收修复④：独立字段，不截断护甲腐蚀窗口
    }

    // =====================================================================
    // 事件后处理（伤害落地 → 命势）
    // =====================================================================

    public static void onDamagePost(Mob mob, float dealt) {
        if (PENDING.isEmpty()) return;
        for (PendingHit hit : List.copyOf(PENDING)) {
            if (hit.target() == mob && PENDING.remove(hit)) {
                if (mob.isAlive()) {
                    MobFateData mobData = mob.getData(ModAttachments.MOB_FATE);
                    com.srqingchen.dicefate.util.DFDebug.log("PendingHit 命势结算: mob={} 拟扣={}",
                        mob.getType().toShortString(), hit.poiseDamage());
                    int poise = hit.poiseDamage();
                    int applied = PoiseService.applyPoiseDamage(mob, mobData, poise, hit.attacker());
                    if (mobData.state() == MobFateData.State.EXHAUSTED) {
                        PoiseService.addDecree(mob, mobData,
                            (int) Math.ceil(hit.playerTotal() * hit.decreeScale()), hit.attacker());
                    }
                    combatText(mob, CombatTextPayload.TextType.POISE, applied, (byte) 7);
                }
            }
        }
    }

    public static void clearPendingFor(Mob mob) {
        PENDING.removeIf(p -> p.target() == mob);
    }

    // =====================================================================
    // 辅助：事件视图 / 工具
    // =====================================================================

    /** LivingIncomingDamageEvent 的极简视图，解耦事件类。 */
    public interface LivingIncomingEventLike {
        float getAmount();
        void setAmount(float v);
        void setCanceled(boolean v);
        boolean isCanceled();
    }

    /** 单次攻击的技能骰副作用（局部传递，替代旧静态字段）。 */
    private static final class AttackFx {
        double greedBonus;
        double pursuitBonus;
        double reversePct;
        boolean echoed;
    }

    private static void skillDiceOnAttack(ServerPlayer player, PlayerFateData data, PoolSnapshot snap,
                                          RollResult roll, Outcome outcome,
                                          AttackFx fx, List<Byte> triggers, RandomSource rng, long tick) {
        fx.greedBonus = 0;
        fx.pursuitBonus = 0;
        fx.reversePct = 0;
        fx.echoed = false;
        int luck = LuckService.shift(player);
        for (int s = 0; s < 2; s++) {
            ItemStack sk = data.pool().skillAt(s);
            if (sk.isEmpty() || !(sk.getItem() instanceof com.srqingchen.dicefate.item.DiceItem dice)) continue;
            if (tick < data.skillCdUntil(s)) continue;
            var def = com.srqingchen.dicefate.dice.DiceLookup.definition(player.level().registryAccess(), dice.dieId);
            if (def == null || def.skillFamily() == null) continue;
            SkillFamily fam = def.skillFamily();
            int tier = def.skillTier();
            PoolSnapshot.SkillView view = new PoolSnapshot.SkillView(def.id(), fam, tier, def.face());

            boolean trigger = switch (fam) {
                case ECHO -> !outcome.isWin() && outcome != Outcome.TIE;    // 攻击失败
                case PURSUIT -> outcome.isWin();
                case REVERSE -> outcome == Outcome.CRASH;
                case GREED -> outcome.isWin();
                case LOCK -> !outcome.isWin() && outcome != Outcome.TIE;
                case FAVOR -> !outcome.isWin() && outcome != Outcome.TIE;
                default -> false;
            };
            if (!trigger) continue;
            com.srqingchen.dicefate.util.DFDebug.log("技能骰触发: 槽={} 族={} 阶={}", s, fam, tier);

            int value = DiceRoller.rollSkill(view, false, luck, rng);
            long cd = Math.max(DFConfig.SKILL_CD_MIN_TICKS.get(),
                (long) (fam.baseCd(tier) * (def.face() - value + 1) / (double) def.face()));
            data.setSkillCdUntil(s, tick + cd);

            switch (fam) {
                case ECHO -> {
                    RollResult second = DiceRoller.roll(snap, rng);
                    int t1 = totalOf(roll), t2 = totalOf(second);
                    if (t2 > t1) {
                        roll.baseRolls.clear();
                        roll.baseRolls.addAll(second.baseRolls);
                        roll.rawTotal = recomputeBaseTotal(roll);
                        fx.echoed = true;
                        // 元素/面数/材质计数不变（集合随装备而非掷值）
                    }
                    triggers.add((byte) ContestPayload.Trigger.SKILL_ECHO.ordinal());
                }
                case PURSUIT -> {
                    fx.pursuitBonus = fam.param(tier) / 100.0;
                    triggers.add((byte) ContestPayload.Trigger.SKILL_PURSUIT.ordinal());
                }
                case REVERSE -> {
                    fx.reversePct = fam.param(tier);
                    // 冷却返还 30/45/60%（0.3.0 验收修复⑤：独立数组——param 是伤害百分比 40/55/70）
                    double[] cdRefund = {0.30, 0.45, 0.60};
                    returnAttackCooldown(player, cdRefund[Math.floorMod(tier, 3)]);
                    triggers.add((byte) ContestPayload.Trigger.SKILL_REVERSE.ordinal());
                }
                case GREED -> {
                    int cost = 4 + tier * 2;
                    var food = player.getFoodData();
                    if (food.getFoodLevel() + food.getSaturationLevel() >= cost) {
                        float sat = food.getSaturationLevel();
                        int rem = 0;
                        if (sat >= cost) {
                            food.setSaturation(sat - cost);
                        } else {
                            rem = (int) Math.ceil(cost - sat);
                            food.setSaturation(0);
                            food.setFoodLevel(Math.max(0, food.getFoodLevel() - rem));   // 显式扣饥饿（D5）
                        }
                        fx.greedBonus = fam.param(tier) / 100.0;
                        triggers.add((byte) ContestPayload.Trigger.SKILL_GREED.ordinal());
                    }
                }
                case LOCK -> {
                    int minIdx = -1, minV = Integer.MAX_VALUE;
                    for (DieRollView v : roll.baseRolls) {
                        if (v.value < minV) { minV = v.value; minIdx = v.slotIndex; }
                    }
                    if (minIdx >= 0) data.setSkillLockSlot(minIdx);
                    triggers.add((byte) ContestPayload.Trigger.SKILL_LOCK.ordinal());
                }
                case FAVOR -> {
                    data.setPityStacks(data.pityStacks() + fam.param(tier));
                    triggers.add((byte) ContestPayload.Trigger.SKILL_FAVOR.ordinal());
                }
                default -> {}
            }
            break;   // 同一拼点最多触发 1 颗
        }
    }

    /** 迅纹：攻击冷却 -3%/层（≤2 层）——ticker 前进 3%×层数。 */
    private static void applySwiftCooldown(ServerPlayer player, PoolSnapshot snap) {
        int stacks = Math.min(2, countRune(snap, DieRune.SWIFT));
        if (stacks <= 0) return;
        float delay = player.getCurrentItemAttackStrengthDelay();
        if (delay <= 0) return;
        player.attackStrengthTicker = (int) Math.min(delay,
            player.attackStrengthTicker + delay * 0.03F * stacks);
    }

    /** 逆命骰：冷却返还 pct——ticker 直接推进到 pct 位置。 */
    private static void returnAttackCooldown(ServerPlayer player, double pct) {
        float delay = player.getCurrentItemAttackStrengthDelay();
        if (delay <= 0) return;
        int target = (int) (delay * pct);
        if (target > player.attackStrengthTicker) player.attackStrengthTicker = target;
    }

    private static double wardSkillMult(ServerPlayer player, PlayerFateData data, RandomSource rng,
                                        long tick, List<Byte> triggers) {
        int luck = LuckService.shift(player);
        for (int s = 0; s < 2; s++) {
            ItemStack sk = data.pool().skillAt(s);
            if (sk.isEmpty() || !(sk.getItem() instanceof com.srqingchen.dicefate.item.DiceItem dice)) continue;
            if (tick < data.skillCdUntil(s)) continue;
            var def = com.srqingchen.dicefate.dice.DiceLookup.definition(player.level().registryAccess(), dice.dieId);
            if (def == null || def.skillFamily() != SkillFamily.WARD) continue;
            PoolSnapshot.SkillView view = new PoolSnapshot.SkillView(def.id(), SkillFamily.WARD, def.skillTier(), def.face());
            int value = DiceRoller.rollSkill(view, false, luck, rng);
            long cd = Math.max(DFConfig.SKILL_CD_MIN_TICKS.get(),
                (long) (SkillFamily.WARD.baseCd(def.skillTier()) * (def.face() - value + 1) / (double) def.face()));
            data.setSkillCdUntil(s, tick + cd);
            triggers.add((byte) ContestPayload.Trigger.SKILL_WARD.ordinal());
            return 1.0 - SkillFamily.WARD.param(def.skillTier()) / 100.0;
        }
        return 1.0;
    }

    /** 怪物护命骰：守方（怪物）防御失败时受伤降低。 */
    private static float mobWardSkill(Mob mob, MobFateData mobData, RandomSource rng, long tick, float amount) {
        if (mobData.profile() == null) return amount;
        for (IdentifierHolder skill : asSkillPaths(mobData)) {
            SkillFamily fam = SkillFamily.byName(skill.path());
            if (fam != SkillFamily.WARD) continue;
            if (mobData.skillCdRemaining(skill.path(), tick) > 0) continue;
            int value = mobData.isExhausted() ? 0 : 1 + rng.nextInt(6);
            long cd = mobSkillCd(fam, 0, 6, value);
            mobData.setSkillCdReady(skill.path(), tick + cd);
            float mult = 1.0F - SkillFamily.WARD.param(0) / 100.0F;
            combatText(mob, CombatTextPayload.TextType.SKILL, value, (byte) 6);
            return amount * mult;
        }
        return amount;
    }

    /** 怪物技能骰 CD（散竭掷 0 → 取 baseCd 本身，0.3.0 C1）。 */
    private static long mobSkillCd(SkillFamily fam, int tier, int face, int value) {
        if (value <= 0) return Math.max(DFConfig.SKILL_CD_MIN_TICKS.get(), fam.baseCd(tier));
        return Math.max(DFConfig.SKILL_CD_MIN_TICKS.get(),
            (long) (fam.baseCd(tier) * (face - value + 1) / (double) face));
    }

    /** 怪物回响骰：攻击失败同池重掷取高。返回替换掷骰或 null。 */
    @Nullable
    private static RollResult mobEchoSkill(Mob mob, MobFateData mobData, PoolSnapshot snap,
                                           RandomSource rng, long tick, List<Byte> triggers) {
        if (mobData.profile() == null) return null;
        for (IdentifierHolder skill : asSkillPaths(mobData)) {
            SkillFamily fam = SkillFamily.byName(skill.path());
            if (fam != SkillFamily.ECHO) continue;
            if (mobData.skillCdRemaining(skill.path(), tick) > 0) continue;
            int value = mobData.isExhausted() ? 0 : 1 + rng.nextInt(6);
            mobData.setSkillCdReady(skill.path(), tick + mobSkillCd(fam, 0, 6, value));
            RollResult second = DiceRoller.roll(snap, rng);
            triggers.add((byte) ContestPayload.Trigger.SKILL_ECHO.ordinal());
            combatText(mob, CombatTextPayload.TextType.SKILL, value, (byte) 6);
            return second;
        }
        return null;
    }

    /** 怪物逆命骰：崩 → 负。返回 param% 或 0。 */
    private static double mobReverseSkill(Mob mob, MobFateData mobData, RandomSource rng, long tick, List<Byte> triggers) {
        if (mobData.profile() == null) return 0;
        for (IdentifierHolder skill : asSkillPaths(mobData)) {
            SkillFamily fam = SkillFamily.byName(skill.path());
            if (fam != SkillFamily.REVERSE) continue;
            if (mobData.skillCdRemaining(skill.path(), tick) > 0) continue;
            int value = mobData.isExhausted() ? 0 : 1 + rng.nextInt(6);
            mobData.setSkillCdReady(skill.path(), tick + mobSkillCd(fam, 0, 6, value));
            triggers.add((byte) ContestPayload.Trigger.SKILL_REVERSE.ordinal());
            return fam.param(0);
        }
        return 0;
    }

    private record IdentifierHolder(String path) {}

    private static List<IdentifierHolder> asSkillPaths(MobFateData mobData) {
        List<IdentifierHolder> out = new ArrayList<>();
        if (mobData.profile() == null) return out;
        for (net.minecraft.resources.Identifier id : mobData.profile().skillDice()) {
            out.add(new IdentifierHolder(id.getPath().replace("skill/", "")));
        }
        return out;
    }

    /** 破势骰：将破时掷骰。gap = 当前命势 - 拟扣命势；flat 不足以扭转则不掷（防 CD 白耗）。
     *  返回 [flat 命势平加, 命破 +pct] 或 null（不触发）。 */
    private static double[] breakerBonus(ServerPlayer player, Mob mob, int gap) {
        MobFateData mobData = mob.getData(ModAttachments.MOB_FATE);
        if (mobData.state() != MobFateData.State.NORMAL) return null;
        var data = player.getData(ModAttachments.PLAYER_FATE);
        long tick = player.level().getGameTime();
        for (int s = 0; s < 2; s++) {
            ItemStack sk = data.pool().skillAt(s);
            if (sk.isEmpty() || !(sk.getItem() instanceof com.srqingchen.dicefate.item.DiceItem dice)) continue;
            var def = com.srqingchen.dicefate.dice.DiceLookup.definition(player.level().registryAccess(), dice.dieId);
            if (def == null || def.skillFamily() != SkillFamily.BREAKER) continue;
            if (tick < data.skillCdUntil(s)) continue;
            int flat = SkillFamily.BREAKER.param(def.skillTier());
            // 自然击破（gap<=0）或 flat 恰好能补破（0<gap<=flat）才掷骰
            if (gap > 0 && gap > flat) return null;
            PoolSnapshot.SkillView view = new PoolSnapshot.SkillView(def.id(), SkillFamily.BREAKER, def.skillTier(), def.face());
            int value = DiceRoller.rollSkill(view, false, LuckService.shift(player), player.level().getRandom());
            long cd = Math.max(DFConfig.SKILL_CD_MIN_TICKS.get(),
                (long) (SkillFamily.BREAKER.baseCd(def.skillTier()) * (def.face() - value + 1) / (double) def.face()));
            data.setSkillCdUntil(s, tick + cd);
            combatText(mob, CombatTextPayload.TextType.SKILL, value, (byte) 6);
            double pct = switch (def.skillTier()) { case 0 -> 0.05; case 1 -> 0.08; default -> 0.10; };
            return new double[]{flat, pct};
        }
        return null;
    }

    /** 豹子/天命豹子的技能 CD 返还（10%/20%）。 */
    private static void refundSkillCdOnTriple(PlayerFateData data, PatternType pattern, long tick) {
        double refund = pattern == PatternType.DESTINY_TRIPLE ? 0.20
            : pattern == PatternType.TRIPLE ? 0.10 : 0;
        if (refund <= 0) return;
        for (int s = 0; s < 2; s++) {
            long until = data.skillCdUntil(s);
            if (until > tick) {
                long total = until - tick;
                data.setSkillCdUntil(s, tick + (long) Math.ceil(total * (1 - refund)));
            }
        }
    }

    // ==================== Boss 机制（按攻击实例一次） ====================

    private static void applyBossMechanics(Mob boss, MobFateData data, RollResult atkRoll, RollResult defRoll,
                                           PlayerFateData player, List<Byte> triggers, RandomSource rng) {
        if (data.profile() == null) return;
        String id = data.profile().id().getPath();
        int n = data.contestCount();
        boolean phase2 = boss instanceof com.srqingchen.dicefate.entity.boss.BossEntity be && be.isPhaseTwo();
        switch (id) {
            case "boss_two_faced_gambler" -> {
                int every = phase2 ? 2 : 3;
                if (n > 0 && n % every == 0) {
                    if (rng.nextBoolean()) {
                        var tmp = List.copyOf(atkRoll.baseRolls);
                        atkRoll.baseRolls.clear();
                        atkRoll.baseRolls.addAll(defRoll.baseRolls);
                        defRoll.baseRolls.clear();
                        defRoll.baseRolls.addAll(tmp);
                        atkRoll.rawTotal = recomputeBaseTotal(atkRoll);
                        defRoll.rawTotal = recomputeBaseTotal(defRoll);
                        triggers.add((byte) ContestPayload.Trigger.LUCKY.ordinal());
                    } else {
                        DieRollView fake = new DieRollView(99, 6, 5, DieElement.NONE);
                        defRoll.baseRolls.add(fake);
                        defRoll.rawTotal = recomputeBaseTotal(defRoll);
                    }
                    combatText(boss, CombatTextPayload.TextType.SKILL, 0, (byte) 6);
                }
            }
            case "boss_molten_die_king" -> {
                int every = 4;
                if (n > 0 && n % every == 0 && !atkRoll.baseRolls.isEmpty()) {
                    DieRollView v = atkRoll.baseRolls.get(rng.nextInt(atkRoll.baseRolls.size()));
                    v.value = 1;   // 熔铸为 1
                    atkRoll.rawTotal = recomputeBaseTotal(atkRoll);
                    combatText(boss, CombatTextPayload.TextType.SKILL, 1, (byte) 4);
                }
            }
            case "boss_frost_silence_queen" -> {
                if (n > 0 && n % 3 == 0) {
                    int slot = rng.nextInt(Math.max(1, player.getBaseSlots()));
                    player.freezeSlot(slot, 2);
                    combatText(boss, CombatTextPayload.TextType.SKILL, 0, (byte) 2);
                }
            }
            case "boss_nullface" -> {
                float frac = boss.getHealth() / boss.getMaxHealth();
                if (frac > 0.66f) {
                    if (n > 0 && n % 3 == 0 && !atkRoll.baseRolls.isEmpty()) {
                        DieRollView highest = atkRoll.baseRolls.get(0);
                        for (DieRollView v : atkRoll.baseRolls) if (v.value > highest.value) highest = v;
                        highest.value = 0;
                        highest.crashed = true;
                        atkRoll.rawTotal = recomputeBaseTotal(atkRoll);
                    }
                } else if (frac > 0.33f) {
                    for (DieRollView v : defRoll.baseRolls) {
                        v.value = rng.nextBoolean() ? v.face : 1;
                    }
                    defRoll.rawTotal = recomputeBaseTotal(defRoll);
                } else {
                    for (DieRollView v : atkRoll.baseRolls) {
                        v.value = 1;
                    }
                    atkRoll.rawTotal = recomputeBaseTotal(atkRoll);
                }
            }
            default -> {}
        }
        data.incrementContest();
    }

    /** 乘算骰·命：大胜时 10%×roll/face 概率追加 3% 最大生命真伤。 */
    private static void applyFateMulTrueDamage(ServerPlayer attacker, LivingEntity target,
                                                RollResult atkRoll, Outcome outcome, RandomSource rng,
                                                List<Byte> triggers) {
        if (!outcome.isGreat()) return;
        for (MulRoll mr : atkRoll.mulRollsOut) {
            if ("mul_die_fate".equals(mr.diePath()) && rng.nextDouble() < 0.10 * mr.ratio()) {
                float td = (float) Math.max(1, Math.floor(target.getMaxHealth() * 0.03));
                hurtFatebreak(attacker, target, td);
                triggers.add((byte) ContestPayload.Trigger.FATEBREAK.ordinal());
                combatText(target, CombatTextPayload.TextType.FATEBREAK, (int) td, (byte) 3);
                break;
            }
        }
    }

    /** fatebreak 源真伤（无视护甲/无敌帧，递归保护）。 */
    private static void hurtFatebreak(ServerPlayer attacker, LivingEntity target, float damage) {
        var registry = target.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, com.srqingchen.dicefate.DiceFate.id("fatebreak"));
        Holder<DamageType> type = registry.get(key.identifier()).orElse(null);
        if (type == null) return;
        DamageSource src = new DamageSource(type, attacker, attacker);
        RecursionGuard.enter();
        try {
            target.invulnerableTime = 0;
            target.hurt(src, damage);
            target.invulnerableTime = 0;
        } finally {
            RecursionGuard.exit();
        }
    }

    /** 乘算骰·元：元素反应效果 +0.30×roll/face。 */
    private static double mulResonanceBonus(RollResult roll) {
        for (MulRoll mr : roll.mulRollsOut) {
            if ("mul_die_resonance".equals(mr.diePath())) return 0.30 * mr.ratio();
        }
        return 0;
    }

    // ==================== 元素反应（骰序第一颗满足的元素对，策划 4.5） ====================

    @Nullable
    private static ReactionDefinition findReaction(ServerPlayer player, RollResult roll, PlayerFateData data,
                                                   SynergyResult syn, long tick) {
        var reg = player.level().registryAccess().lookupOrThrow(DFRegistries.REACTION);
        boolean hasElementAdd = addDieIsElement(data);
        for (DieRollView v : roll.baseRolls) {
            if (!v.element.isReal()) continue;
            for (ReactionDefinition def : reg) {
                if (def.elements().size() != 2) continue;
                DieElement a = DieElement.byName(def.elements().get(0));
                DieElement b = DieElement.byName(def.elements().get(1));
                if (a != v.element && b != v.element) continue;
                DieElement other = a == v.element ? b : a;
                int co = roll.elementCounts.getOrDefault(other, 0);
                if (!(co > 0 || (hasElementAdd && co >= 0))) continue;
                if (data.reactionCdRemaining(def.id(), tick) > 0) continue;
                // 反应 CD：双元协同 -20% / 元素共鸣器 -20%
                double cdMod = 1 + syn.reactionCdMod();
                if (CuriosCompat.hasElementResonator(player)) cdMod *= 0.8;
                int cd = Math.max(20, (int) (def.cooldownTicks() * Math.max(0.2, cdMod)));
                data.setReactionCd(def.id(), tick + cd);
                return def;
            }
        }
        return null;
    }

    /** 爆燃反应：目标最大生命%真伤 + 自身失去 2% 最大生命（不致死）；乘算骰·元放大。 */
    private static void triggerDetonate(ServerPlayer attacker, LivingEntity target,
                                        ReactionDefinition reaction, boolean boss, double resBoost) {
        double pct = (boss ? reaction.value() / 2 : reaction.value()) * (1 + resBoost);
        float dmg = (float) Math.max(1, Math.floor(target.getMaxHealth() * pct));
        hurtFatebreak(attacker, target, dmg);
        float self = (float) Math.max(0, Math.floor(attacker.getMaxHealth() * 0.02));
        if (attacker.getHealth() - self < 1.0F) self = Math.max(0, attacker.getHealth() - 1.0F);
        if (self > 0) attacker.hurt(attacker.damageSources().magic(), self);
        combatText(target, CombatTextPayload.TextType.SKILL, (int) dmg, (byte) 3);
    }

    // ==================== 掷骰工具 ====================

    /** 兑现：全部骰子取最大面（保留加算/乘算/协同计数——0.3.0 A4）。 */
    private static RollResult maxAll(RollResult roll) {
        RollResult out = copyRoll(roll);
        for (DieRollView v : out.baseRolls) {
            v.value = Math.max(0, v.face);
            v.lockedUsed = true;
        }
        for (DieRollView v : out.addRolls) {
            v.value = Math.max(0, v.face);
        }
        for (DieRollView v : out.mulRolls) {
            v.value = Math.max(0, v.face);
        }
        // 乘算骰记录按原 diePath 以最大面重建（ratio=1.0）
        for (MulRoll mr : roll.mulRollsOut) {
            out.mulRollsOut.add(new MulRoll(mr.diePath(), mr.face(), mr.face()));
        }
        out.rawTotal = recomputeBaseTotal(out);
        return out;
    }

    private static RollResult zeroAll(PoolSnapshot snap) {
        RollResult out = new RollResult();
        for (PoolSnapshot.SlotView sv : snap.slots) {
            DieRollView nv = new DieRollView(sv.slotIndex, sv.face(), 0, sv.element());
            nv.crashed = true;
            out.baseRolls.add(nv);
        }
        out.rawTotal = 0;
        return out;
    }

    /** 基础骰 Σ（负值计入）；rawTotal 统一 clamp 由 totalOf 处理。 */
    private static int recomputeBaseTotal(RollResult r) {
        int t = 0;
        for (DieRollView v : r.baseRolls) t += v.value;
        return t;
    }

    private static int totalOf(RollResult r) {
        int t = r.rawTotal;
        for (DieRollView v : r.addRolls) t += v.value;
        return Math.max(0, t);
    }

    // ==================== 乘算骰（分族系数，策划 5.1.5） ====================

    /** 攻击侧乘算：烈（伤害）、势（命势）；命（大胜真伤概率）与元（反应放大）单独结算。 */
    private static double mulAttackMultiplier(RollResult roll, Outcome outcome, PatternType pattern) {
        if (!outcome.isWin() || roll.mulRollsOut.isEmpty()) return 1.0;
        double m = 1.0;
        for (MulRoll mr : roll.mulRollsOut) {
            switch (mr.diePath()) {
                case "mul_die_fierce" -> m += 0.35 * mr.ratio();
                default -> {}
            }
        }
        return Math.min(2.5, m);
    }

    private static double mulPoiseMultiplier(RollResult roll, Outcome outcome) {
        if (!outcome.isWin() || roll.mulRollsOut.isEmpty()) return 1.0;
        double m = 1.0;
        for (MulRoll mr : roll.mulRollsOut) {
            if ("mul_die_break".equals(mr.diePath())) m += 0.40 * mr.ratio();
        }
        return m;
    }

    private static double mulPierceBonus(RollResult roll, Outcome outcome) {
        if (!outcome.isWin() || roll.mulRollsOut.isEmpty()) return 0;
        double p = 0;
        for (MulRoll mr : roll.mulRollsOut) {
            if ("mul_die_pierce".equals(mr.diePath())) p += 0.20 * mr.ratio();
        }
        return p;
    }

    /** 防御侧乘算：壁（受伤减少）。 */
    private static double mulDefenseMultiplier(RollResult defRoll) {
        if (defRoll.mulRollsOut.isEmpty()) return 1.0;
        double m = 1.0;
        for (MulRoll mr : defRoll.mulRollsOut) {
            if ("mul_die_bulwark".equals(mr.diePath())) m -= 0.25 * mr.ratio();
        }
        return Math.max(0.3, m);
    }

    // ==================== 穿透代偿（CombatRules 真实公式） ====================

    /** vanilla 26.1 护甲减伤比例（近似复刻 CombatRules.getDamageAfterAbsorb，忽略附魔调整）。 */
    private static double armorFraction(double damage, double armor, double toughness) {
        double tough = 2.0 + toughness / 4.0;
        double real = Mth.clamp(armor - damage / tough, armor * 0.2, 20.0);
        return Mth.clamp(real / 25.0, 0.0, 1.0);
    }

    /**
     * 穿透代偿：目标是 absorb(dmg, armor×(1-pen), tough)，
     * 解 y 使 absorb(y, armor, tough) = 目标（隐式方程迭代 2 轮）。
     */
    private static float applyPenCompensation(LivingEntity target, double dmg, double pen) {
        if (pen <= 0 || !(target instanceof Mob)) return (float) dmg;
        double armor = target.getArmorValue();
        double toughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        double targetFinal = dmg * (1 - armorFraction(dmg, armor * (1 - pen), toughness));
        double y = targetFinal / Math.max(0.2, 1 - armorFraction(dmg, armor, toughness));
        for (int i = 0; i < 2; i++) {
            y = targetFinal / Math.max(0.2, 1 - armorFraction(y, armor, toughness));
        }
        return (float) y;
    }

    // ==================== 平加/减伤桶 ====================

    private static double pierceRuneBonus(PlayerFateData data) {
        if (data.playerRune() != null && data.playerRune().getPath().equals("rune_pierce")) return 0.10;
        return 0;
    }

    private static double playerRuneDamageBonus(PlayerFateData data) {
        if (data.playerRune() == null) return 0;
        return switch (data.playerRune().getPath()) {
            case "rune_greedy" -> 0.05;
            case "rune_sacrifice" -> 0.05;
            default -> 0;
        };
    }

    /** 命势伤害平加：锋纹（胜利，≤3 层）+ 加算骰·锋（胜利 +2）+ 贪纹 -1。 */
    private static int poiseFlatBonus(PlayerFateData data, PoolSnapshot snap, Outcome outcome) {
        if (!outcome.isWin()) return 0;
        int flat = 0;
        if (data.playerRune() != null && data.playerRune().getPath().equals("rune_edge")) flat += 1;
        flat += countRune(snap, DieRune.EDGE);          // 锋：胜利命势 +1/层（≤3）
        if (addDieIsEdge(data)) flat += 2;              // 加算骰·锋：胜利命势 +2（策划 5.1.4）
        flat -= Math.min(3, countRuneNoCap(snap, DieRune.GREEDY));   // 贪：命势 -1/层
        return flat;
    }

    /** 烈纹：大胜伤害 +5%（不叠加）。 */
    private static double fierceRuneBonus(PoolSnapshot snap) {
        for (PoolSnapshot.SlotView sv : snap.slots) {
            if (sv.state != null && sv.state.runeId() == DieRune.FIERCE) return 0.05;
        }
        return 0;
    }

    private static int countRune(PoolSnapshot snap, DieRune rune) {
        int count = 0;
        for (PoolSnapshot.SlotView sv : snap.slots) {
            if (sv.state != null && sv.state.runeId() == rune) count++;
        }
        return Math.min(3, count);
    }

    private static int countRuneNoCap(PoolSnapshot snap, DieRune rune) {
        int count = 0;
        for (PoolSnapshot.SlotView sv : snap.slots) {
            if (sv.state != null && sv.state.runeId() == rune) count++;
        }
        return count;
    }

    /** 韧纹：装备期间护甲 +1（不叠加；由 CombatEvents 周期对账）。 */
    public static void reconcileTough(ServerPlayer player, PlayerFateData data) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) return;
        var id = com.srqingchen.dicefate.DiceFate.id("tough_rune");
        boolean has = false;
        for (ItemStack s : data.pool().allDice()) {
            var state = s.get(ModDataComponents.DIE_STATE.get());
            if (state != null && state.runeId() == DieRune.TOUGH) { has = true; break; }
        }
        boolean present = armor.getModifier(id) != null;
        if (has && !present) {
            armor.addTransientModifier(new AttributeModifier(id, 1.0,
                AttributeModifier.Operation.ADD_VALUE));
        } else if (!has && present) {
            armor.removeModifier(id);
        }
    }

    /** 复制共享掷骰结果（与 Boss 改写/回响重掷隔离）。 */
    private static RollResult copyRoll(RollResult src) {
        RollResult out = new RollResult();
        for (DieRollView v : src.baseRolls) {
            DieRollView nv = new DieRollView(v.slotIndex, v.face, v.value, v.element);
            nv.crashed = v.crashed;
            nv.pityRolled = v.pityRolled;
            nv.nudged = v.nudged;
            nv.lockedUsed = v.lockedUsed;
            out.baseRolls.add(nv);
        }
        for (DieRollView v : src.addRolls) out.addRolls.add(v);
        for (DieRollView v : src.mulRolls) out.mulRolls.add(v);
        out.rawTotal = src.rawTotal;
        out.elementCounts.putAll(src.elementCounts);
        out.triggeredRunes.addAll(src.triggeredRunes);
        out.faceCounts.putAll(src.faceCounts);
        out.materialCounts.putAll(src.materialCounts);
        out.mulRollsOut.addAll(src.mulRollsOut);
        return out;
    }

    /** F：骰包增伤（高阶包 +10%/20%）。 */
    private static double packDamageBonus(ServerPlayer player, PlayerFateData data) {
        if (data.activePack() == null) return 0;
        String path = data.activePack().getPath();
        if (path.equals("pack_epic") || path.equals("pack_fate_major")) return 0.20;
        if (path.equals("pack_rare") || path.equals("pack_fate_minor")) return 0.10;
        if (path.startsWith("pack_")) return 0.05;
        return 0;
    }

    private static boolean addDieIsCombo(PlayerFateData data) {
        return !data.pool().addDie().isEmpty()
            && data.pool().addDie().getItem().builtInRegistryHolder().getRegisteredName().equals("dicefate:add_die_combo");
    }

    private static boolean addDieIsMend(PlayerFateData data) {
        return !data.pool().addDie().isEmpty()
            && data.pool().addDie().getItem().builtInRegistryHolder().getRegisteredName().equals("dicefate:add_die_mend");
    }

    private static boolean addDieIsEdge(PlayerFateData data) {
        return !data.pool().addDie().isEmpty()
            && data.pool().addDie().getItem().builtInRegistryHolder().getRegisteredName().equals("dicefate:add_die_edge");
    }

    private static boolean addDieIsElement(PlayerFateData data) {
        return !data.pool().addDie().isEmpty()
            && data.pool().addDie().getItem().builtInRegistryHolder().getRegisteredName().equals("dicefate:add_die_element");
    }

    private static int weaponTotalBonus(ServerPlayer player) {
        ItemStack weapon = player.getMainHandItem();
        if (weapon.is(com.srqingchen.dicefate.registry.GearItems.DICE_BLADE_BONE.get())) return 1;
        if (weapon.is(com.srqingchen.dicefate.registry.GearItems.DICE_BLADE_IRON.get())) return 1;
        if (weapon.is(com.srqingchen.dicefate.registry.GearItems.DICE_BLADE_MITHRIL.get())) return 2;
        if (weapon.is(com.srqingchen.dicefate.registry.GearItems.DICE_BLADE_FATEGOLD.get())) return 2;
        if (weapon.is(com.srqingchen.dicefate.registry.GearItems.DICE_SLING_MITHRIL.get())) return 1;
        return 0;
    }

    private static int shieldBonus(ServerPlayer player) {
        if (!player.isBlocking()) return 0;
        if (player.getUseItem().is(com.srqingchen.dicefate.registry.GearItems.FATE_SHIELD_BONE.get())) return 2;
        if (player.getUseItem().is(com.srqingchen.dicefate.registry.GearItems.FATE_SHIELD_MITHRIL.get())) return 4;
        if (player.getUseItem().is(com.srqingchen.dicefate.registry.GearItems.FATE_SHIELD_FATEGOLD.get())) return 6;
        return 0;
    }

    private static double shieldThresholdBonus(ServerPlayer player) {
        ItemStack shield = player.getUseItem();
        if (shield.is(com.srqingchen.dicefate.registry.GearItems.FATE_SHIELD_MITHRIL.get())) return -0.1;
        if (shield.is(com.srqingchen.dicefate.registry.GearItems.FATE_SHIELD_FATEGOLD.get())) return -0.15;
        return 0.0;
    }

    private static double reduceBucket(PlayerFateData data) {
        double bucket = 0;
        if (data.guardCharges() > 0) {
            bucket += 0.10;
            data.consumeGuard();
        }
        if (data.playerRune() != null && data.playerRune().getPath().equals("rune_guard")) bucket += 0.05;
        return bucket;
    }

    private static void disableShield(ServerPlayer player, int ticks) {
        player.stopUsingItem();
        var cds = player.getCooldowns();
        cds.addCooldown(net.minecraft.resources.Identifier.withDefaultNamespace("minecraft:shield"), ticks);
        cds.addCooldown(com.srqingchen.dicefate.DiceFate.id("fate_shield_bone"), ticks);
        cds.addCooldown(com.srqingchen.dicefate.DiceFate.id("fate_shield_mithril"), ticks);
        cds.addCooldown(com.srqingchen.dicefate.DiceFate.id("fate_shield_fategold"), ticks);
    }

    private static void knockbackAttacker(ServerPlayer defender, Mob attacker) {
        double dx = attacker.getX() - defender.getX();
        double dz = attacker.getZ() - defender.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.01) dx = 1;
        double kx = dist < 0.01 ? 1 : dx / dist;
        double kz = dist < 0.01 ? 0 : dz / dist;
        attacker.knockback(0.6, -kx, -kz);
    }

    private static int mobLuck(LivingEntity mob) {
        return LuckService.shift(mob);
    }

    private static MobFateData ensureMobData(Mob mob, ServerPlayer observer) {
        MobFateData data = mob.getData(ModAttachments.MOB_FATE);
        if (!data.isInitialized()) {
            PoolProfile profile = SnapshotFactory.profileFor(mob, observer.level().registryAccess());
            if (profile != null) PoiseService.ensureInit(mob, profile);
            else PoiseService.ensureFallback(mob);
        }
        return data;
    }

    private static int sharedTargets(ServerPlayer attacker, long tick) {
        SharedAttack sa = sharedAttacks.get(attacker.getUUID());
        return sa == null ? 1 : Math.max(1, sa.targets());
    }

    private static void sendContest(LivingEntity a, LivingEntity b, int track, RollResult atkRoll, RollResult defRoll,
                                    int atkTotal, int defTotal, int luckA, int luckD, Outcome outcome,
                                    float damage, int poise, int decree, List<Byte> triggers, int targets) {
        List<ContestPayload.DieEntry> atkEntries = new ArrayList<>();
        for (DieRollView v : atkRoll.baseRolls) atkEntries.add(toEntry(v));
        for (DieRollView v : atkRoll.addRolls) atkEntries.add(toEntry(v));
        List<ContestPayload.DieEntry> defEntries = new ArrayList<>();
        for (DieRollView v : defRoll.baseRolls) defEntries.add(toEntry(v));

        ContestPayload payload = new ContestPayload(
            ContestPayload.packMeta(track, outcome, luckA, luckD, targets),
            a.getId(), b.getId(), atkEntries, defEntries,
            atkTotal, defTotal, damage, poise, decree, triggers);
        DFNetwork.sendContest(a, b, payload);
    }

    private static ContestPayload.DieEntry toEntry(DieRollView v) {
        byte flags = 0;
        if (v.crashed) flags |= ContestPayload.DieEntry.FLAG_CRASHED;
        if (v.pityRolled) flags |= ContestPayload.DieEntry.FLAG_PITY;
        if (v.nudged) flags |= ContestPayload.DieEntry.FLAG_NUDGED;
        if (v.lockedUsed) flags |= ContestPayload.DieEntry.FLAG_LOCKED;
        return new ContestPayload.DieEntry((byte) v.face, v.value, (byte) v.element.ordinal(), flags);
    }

    private static void combatText(LivingEntity target, CombatTextPayload.TextType type, int value, byte color) {
        if (target.level() instanceof ServerLevel level) {
            DFNetwork.sendCombatText(level, target, new CombatTextPayload(
                target.getId(), (byte) type.ordinal(), value, color));
        }
    }

    static {
        // 破势骰钩子：gap 感知（0.3.0 A2：将破才掷，自然击破也触发）
        PoiseService.BREAKER_HOOK = (player, mob, gap) -> breakerBonus(player, mob, gap);
    }

    // 静态状态重置（测试用）
    public static void resetStaticState() {
        PENDING.clear();
        sharedAttacks.clear();
    }
}
