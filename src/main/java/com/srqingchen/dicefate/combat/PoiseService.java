package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.DFConfig;
import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.attachment.MobFateData;
import com.srqingchen.dicefate.attachment.PlayerFateData;
import com.srqingchen.dicefate.data.PoolProfile;
import com.srqingchen.dicefate.network.CombatTextPayload;
import com.srqingchen.dicefate.network.DFNetwork;
import com.srqingchen.dicefate.registry.ModAttachments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 命势系统（规划 3.1）：破势 → 命气散竭 → 命定 → 命破 → 命损 → 恢复/防循环。
 * 玩家没有命势条；仅怪物/Boss。
 * 0.2.0：tick 按 dt 精确扣减（修复时长 10 倍 bug）；命定阈值首破不再抬升；
 * 命损=散竭剩余+恢复期+3s；破势骰同时给命破百分比；Boss 阶段上限 +5%；
 * 腐蚀（元素反应）护甲 modifier 到期移除；离场 30s 重置上限/阈值；时钟改 gameTime。
 */
public final class PoiseService {
    /** F4/F9：破势骰钩子（服务端安装，0.3.0 gap 感知）。
     *  gap = 当前命势 − 拟扣命势；gap≤0（自然击破）或 flat 足以补破时才掷骰，防 CD 白耗。
     *  返回 [额外命势平加, 命破 +pct] 或 null（不触发）。 */
    @FunctionalInterface
    public interface BreakerHook {
        double[] onNearBreak(ServerPlayer player, Mob mob, int gap);
    }

    public static transient BreakerHook BREAKER_HOOK;

    private static final ResourceKey<DamageType> FATEBREAK_KEY =
        ResourceKey.create(Registries.DAMAGE_TYPE, DiceFate.id("fatebreak"));

    private PoiseService() {}

    // ==================== 初始化 ====================

    public static MobFateData ensureInit(Mob mob, PoolProfile profile) {
        MobFateData data = mob.getData(ModAttachments.MOB_FATE);
        if (!data.isInitialized()) {
            double mult;
            double flat;
            int threshold;
            switch (profile.rank()) {
                case ELITE -> { mult = DFConfig.POISE_ELITE_MULT.get(); flat = DFConfig.POISE_ELITE_FLAT.get(); threshold = DFConfig.FATEBREAK_THRESHOLD_ELITE.get(); }
                case BOSS, GUARD -> { mult = DFConfig.POISE_BOSS_MULT.get(); flat = 0; threshold = DFConfig.FATEBREAK_THRESHOLD_BOSS.get(); }
                default -> { mult = DFConfig.POISE_NORMAL_MULT.get(); flat = DFConfig.POISE_NORMAL_FLAT.get(); threshold = DFConfig.FATEBREAK_THRESHOLD_NORMAL.get(); }
            }
            int maxPoise = (int) Math.floor(mob.getMaxHealth() * mult + flat);
            data.init(profile, maxPoise, threshold);
        }
        return data;
    }

    /** 无档案生物的兜底（规划 8.10.4）。 */
    public static MobFateData ensureFallback(Mob mob) {
        MobFateData data = mob.getData(ModAttachments.MOB_FATE);
        if (!data.isInitialized()) {
            int maxPoise = (int) Math.floor(mob.getMaxHealth() * DFConfig.POISE_NORMAL_MULT.get() + DFConfig.POISE_NORMAL_FLAT.get());
            data.init(null, maxPoise, DFConfig.FATEBREAK_THRESHOLD_NORMAL.get());
        }
        return data;
    }

    // ==================== 命势伤害 ====================

    /** 应用命势伤害（伤害落地后调用）；attacker 可为 null（元素反应等）。 */
    public static int applyPoiseDamage(Mob target, MobFateData data, int amount, ServerPlayer attacker) {
        if (amount <= 0) return 0;
        if (data.state() == MobFateData.State.RECOVERING) {
            amount = (int) Math.floor(amount * 0.5);   // 回稳期命势伤害 -50%
        }
        if (data.state() == MobFateData.State.EXHAUSTED) {
            // 散竭期间命势已归零：不再扣
            return 0;
        }
        // 单次上限：普通 50% 上限；精英/Boss 25%
        PoolProfile.Rank rank = data.profile() == null ? PoolProfile.Rank.NORMAL : data.profile().rank();
        double capRatio = (rank == PoolProfile.Rank.NORMAL) ? 0.5 : 0.25;
        int cap = (int) Math.max(1, data.maxPoise() * capRatio);
        amount = Math.min(amount, cap);

        // F4：破势骰回调（0.3.0 A2：gap 感知——将破才掷，自然击破也触发；未破攻击不再烧 CD）
        double breakerPct = 0.0;
        int before = data.poise();
        int gap = before - amount;
        if (attacker != null && data.state() == MobFateData.State.NORMAL && BREAKER_HOOK != null
            && gap <= 20) {
            double[] bonus = BREAKER_HOOK.onNearBreak(attacker, target, gap);
            if (bonus != null && bonus.length >= 2) {
                int flat = (int) bonus[0];
                breakerPct = bonus[1];
                if (gap > 0 && flat > 0 && gap - flat <= 0) {
                    amount += flat;
                }
            }
        }
        int after = Math.max(0, before - amount);
        data.setPoise(after);

        if (attacker != null) {
            data.notePoiseAttacker(attacker.getUUID(), target.level().getGameTime());
            data.setLastCombatTick(target.level().getGameTime());
        }

        // 0.3.0 B10：恢复期被打回 0 也可再次触发散竭（防循环由上限递增/阈值递增/回稳-50% 三件套承担）
        com.srqingchen.dicefate.util.DFDebug.log("命势伤害应用: mob={} 前={} 拟扣={} 后={}",
            target.getType().toShortString(), before, amount, after);
        if (after <= 0 && data.state() != MobFateData.State.EXHAUSTED) {
            com.srqingchen.dicefate.util.DFDebug.always("命势击破!: mob={}", target.getType().toShortString());
            onPoiseBreak(target, data, attacker, breakerPct);
        } else if (target.level() instanceof ServerLevel level) {
            DFNetwork.sendPoise(data, target);
        }
        return before - after;
    }

    // ==================== 破势 ====================

    private static void onPoiseBreak(Mob target, MobFateData data, ServerPlayer breaker, double breakerPct) {
        PoolProfile.Rank rank = data.profile() == null ? PoolProfile.Rank.NORMAL : data.profile().rank();
        boolean boss = rank == PoolProfile.Rank.BOSS || rank == PoolProfile.Rank.GUARD;
        int breakSeconds = boss ? DFConfig.BOSS_BREAK_SECONDS.get() : DFConfig.BREAK_SECONDS.get();

        data.addBreak();
        data.setState(MobFateData.State.EXHAUSTED, breakSeconds * 20);
        data.setPoise(0);
        data.setDecree(0);
        data.setDecreeTriggered(false);
        // 阈值递增（+10%/次，封顶 +50%）——0.2.0：首个散竭窗口用基准值，下一次破势才抬升
        double gain = DFConfig.DECREE_THRESHOLD_GAIN.get() * Math.max(0, data.breakCount() - 1);
        double cap = DFConfig.DECREE_THRESHOLD_CAP.get();
        double baseThreshold = boss ? DFConfig.FATEBREAK_THRESHOLD_BOSS.get()
            : rank == PoolProfile.Rank.ELITE ? DFConfig.FATEBREAK_THRESHOLD_ELITE.get()
            : DFConfig.FATEBREAK_THRESHOLD_NORMAL.get();
        data.setDecreeThreshold((int) Math.floor(baseThreshold * Math.min(1 + cap, 1 + gain)));

        applyExhaustModifiers(target, true);

        // 0.3.0 F1：破势进度直授
        if (breaker != null) {
            com.srqingchen.dicefate.util.AdvancementGrant.grant(breaker, "first_poise");
            if (rank == PoolProfile.Rank.ELITE) com.srqingchen.dicefate.util.AdvancementGrant.grant(breaker, "break_elite");
            if (rank == PoolProfile.Rank.BOSS || rank == PoolProfile.Rank.GUARD) {
                com.srqingchen.dicefate.util.AdvancementGrant.grant(breaker, "break_boss");
            }
        }

        // 破势奖励骰势（最近攻击者优先，防挂机）
        if (breaker != null) {
            PlayerFateData pf = breaker.getData(ModAttachments.PLAYER_FATE);
            long tick = breaker.level().getGameTime();
            pf.setMomentum(pf.getMomentum() + 10, tick);
            pf.markContest(tick);
        }

        if (target.level() instanceof ServerLevel level) {
            DFNetwork.sendPoise(data, target);
            DFNetwork.sendCombatText(level, target, new CombatTextPayload(
                target.getId(), (byte) CombatTextPayload.TextType.POISE_BREAK.ordinal(), 0, (byte) 6));
            level.playSound(null, target.blockPosition(),
                com.srqingchen.dicefate.registry.ModSounds.POISE_BREAK.value(),
                net.minecraft.sounds.SoundSource.HOSTILE, 0.7F, 1.0F);
        }

        // 破势骰第二效果：本次命破 +pct —— 存到 MobFateData 由 triggerFatebreak 消费
        if (breakerPct > 0) data.setPendingBreakerPct(breakerPct);
    }

    /** 命气散竭属性 modifier（唯一 ID 含实体，避免多怪覆盖）。 */
    private static void applyExhaustModifiers(Mob target, boolean apply) {
        Identifier atkId = DiceFate.id("exhausted_attack/" + target.getUUID());
        Identifier armId = DiceFate.id("exhausted_armor/" + target.getUUID());
        AttributeInstance atk = target.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance arm = target.getAttribute(Attributes.ARMOR);
        if (atk != null) {
            if (apply) {
                atk.removeModifier(atkId);
                atk.addTransientModifier(new AttributeModifier(atkId, -0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else atk.removeModifier(atkId);
        }
        if (arm != null) {
            if (apply) {
                arm.removeModifier(armId);
                arm.addTransientModifier(new AttributeModifier(armId, -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else arm.removeModifier(armId);
        }
    }

    // ==================== 命定与命破 ====================

    /** 散竭期间有效攻击累计命定；达到阈值触发命破。 */
    public static void addDecree(Mob target, MobFateData data, int gain, ServerPlayer attacker) {
        if (data.state() != MobFateData.State.EXHAUSTED || data.isDecreeTriggered() || gain <= 0) return;
        data.addDecree(gain);
        if (data.decree() >= data.decreeThreshold()) {
            data.setDecreeTriggered(true);
            triggerFatebreak(target, data, attacker);
        }
    }

    private static void triggerFatebreak(Mob target, MobFateData data, ServerPlayer attacker) {
        com.srqingchen.dicefate.util.DFDebug.always("命破触发: mob={}", target.getType().toShortString());
        // 0.3.0 F1：命破进度
        if (attacker != null) com.srqingchen.dicefate.util.AdvancementGrant.grant(attacker, "fatebreak");
        PoolProfile.Rank rank = data.profile() == null ? PoolProfile.Rank.NORMAL : data.profile().rank();
        double pct = switch (rank) {
            case ELITE -> DFConfig.FATEBREAK_PCT_ELITE.get();
            case BOSS, GUARD -> DFConfig.FATEBREAK_PCT_BOSS.get();
            default -> DFConfig.FATEBREAK_PCT_NORMAL.get();
        };
        // 破势骰：本次命破 +5/8/10%
        pct += data.consumePendingBreakerPct();
        float damage = (float) Math.floor(target.getMaxHealth() * pct);

        // 命损：散竭剩余 + 恢复期 + 3s，+15% 受伤
        PoolProfile.Rank rk = data.profile() == null ? PoolProfile.Rank.NORMAL : data.profile().rank();
        boolean boss = rk == PoolProfile.Rank.BOSS || rk == PoolProfile.Rank.GUARD;
        int recovery = (boss ? DFConfig.BOSS_RECOVERY_SECONDS.get() : DFConfig.RECOVERY_SECONDS.get()) * 20;
        int wound = data.stateTicks() + recovery + DFConfig.WOUND_EXTRA_SECONDS.get() * 20;
        data.setWoundTicks(wound);

        // 真伤：无视护甲/无敌帧（0.3.0 C2：缺 damage_type 时安全跳过，不崩服）
        var registry = target.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> type = registry.get(FATEBREAK_KEY.identifier()).orElse(null);
        if (type != null) {
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

        if (target.level() instanceof ServerLevel level) {
            DFNetwork.sendPoise(data, target);
            DFNetwork.sendCombatText(level, target, new CombatTextPayload(
                target.getId(), (byte) CombatTextPayload.TextType.FATEBREAK.ordinal(), (int) damage, (byte) 6));
            level.playSound(null, target.blockPosition(),
                com.srqingchen.dicefate.registry.ModSounds.FATEBREAK.value(),
                net.minecraft.sounds.SoundSource.HOSTILE, 0.8F, 1.0F);
            if (attacker != null) {
                DFNetwork.sendFateState(attacker, attacker.getData(ModAttachments.PLAYER_FATE),
                    level.getGameTime());
            }
        }
    }

    // ==================== 腐蚀（元素反应） ====================

    /** 腐蚀：护甲 -15% 6s（transient modifier，到期由 tick 移除）。 */
    public static void applyCorrode(Mob target, MobFateData data, double fraction, int durationTicks) {
        AttributeInstance arm = target.getAttribute(Attributes.ARMOR);
        if (arm == null) return;
        Identifier id = DiceFate.id("corrode/" + target.getUUID());
        arm.removeModifier(id);
        arm.addTransientModifier(new AttributeModifier(id, -fraction,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        data.setCorrodeUntil(target.level().getGameTime() + durationTicks);
    }

    private static void tickCorrode(Mob mob, MobFateData data, long gameTick) {
        // 0.3.0 验收修复④：护甲腐蚀与防御腐蚀（攻击-10%）分字段到期清理，互不截断
        if (data.corrodeUntil() > 0 && gameTick >= data.corrodeUntil()) {
            AttributeInstance arm = mob.getAttribute(Attributes.ARMOR);
            if (arm != null) arm.removeModifier(DiceFate.id("corrode/" + mob.getUUID()));
            data.setCorrodeUntil(0);
        }
        if (data.corrodeAtkUntil() > 0 && gameTick >= data.corrodeAtkUntil()) {
            AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) atk.removeModifier(DiceFate.id("corrode_atk/" + mob.getUUID()));
            data.setCorrodeAtkUntil(0);
        }
    }

    // ==================== Tick（每 10t 批量，dt 精确扣减） ====================

    public static void tick(Mob mob, long gameTick) {
        MobFateData data = mob.getData(ModAttachments.MOB_FATE);
        if (!data.isInitialized()) return;

        // 0.3.0 C3：dt 上限放宽到 600（玩家离场 2s 以上回来补算，不再无限拖长散竭）
        int dt = data.lastPoiseTick() == Long.MIN_VALUE ? 10 : (int) Mth.clamp(gameTick - data.lastPoiseTick(), 1, 600);
        data.setLastPoiseTick(gameTick);

        // Boss 阶段：66%/33% 血量各 +5% 命势上限（不重置当前命势，计入 +50% 上限）
        tickBossPhase(mob, data);

        // 腐蚀到期
        tickCorrode(mob, data, gameTick);

        // 离场 30s：重置命势上限加成与命定阈值（防循环三件套的归零）
        if (data.state() == MobFateData.State.NORMAL
            && data.lastCombatTick() != Long.MIN_VALUE
            && gameTick - data.lastCombatTick() > 600
            && !data.isOutOfCombatResolved()) {
            PoolProfile.Rank rank = data.profile() == null ? PoolProfile.Rank.NORMAL : data.profile().rank();
            int baseThreshold = (rank == PoolProfile.Rank.BOSS || rank == PoolProfile.Rank.GUARD)
                ? DFConfig.FATEBREAK_THRESHOLD_BOSS.get()
                : rank == PoolProfile.Rank.ELITE ? DFConfig.FATEBREAK_THRESHOLD_ELITE.get()
                : DFConfig.FATEBREAK_THRESHOLD_NORMAL.get();
            data.resetBreaks((int) Math.floor(rawMax(mob, data)), baseThreshold);
            data.markOutOfCombatResolved();
        }

        switch (data.state()) {
            case EXHAUSTED -> {
                data.tickState(dt);
                if (data.stateTicks() <= 0) {
                    // 进入恢复期
                    PoolProfile.Rank rank = data.profile() == null ? PoolProfile.Rank.NORMAL : data.profile().rank();
                    boolean boss = rank == PoolProfile.Rank.BOSS || rank == PoolProfile.Rank.GUARD;
                    int recovery = (boss ? DFConfig.BOSS_RECOVERY_SECONDS.get() : DFConfig.RECOVERY_SECONDS.get()) * 20;
                    data.setState(MobFateData.State.RECOVERING, recovery);
                    applyExhaustModifiers(mob, false);
                }
                if (gameTick % 20 < 10) DFNetwork.sendPoise(data, mob);
            }
            case RECOVERING -> {
                data.tickState(dt);
                PoolProfile.Rank rank = data.profile() == null ? PoolProfile.Rank.NORMAL : data.profile().rank();
                boolean boss = rank == PoolProfile.Rank.BOSS || rank == PoolProfile.Rank.GUARD;
                int total = (boss ? DFConfig.BOSS_RECOVERY_SECONDS.get() : DFConfig.RECOVERY_SECONDS.get()) * 20;
                double frac = 1.0 - data.stateTicks() / (double) Math.max(1, total);
                int target = (int) Math.floor(data.maxPoise() * frac);
                if (target > data.poise()) data.setPoise(target);
                data.tickWound(dt);
                if (data.stateTicks() <= 0) {
                    data.setState(MobFateData.State.NORMAL, 0);
                    data.setWoundTicks(0);
                    // 命势上限 +5%（封顶 +50%）
                    double baseMax = rawMax(mob, data);
                    double cap = DFConfig.POISE_MAX_GAIN_CAP.get();
                    double nextMult = Math.min(1 + cap, 1 + DFConfig.POISE_MAX_GAIN_PER_BREAK.get() * data.breakCount());
                    data.init(data.profile(), (int) Math.floor(baseMax * nextMult), data.decreeThreshold());
                    data.setPoise(data.maxPoise());
                }
                if (gameTick % 20 < 10) DFNetwork.sendPoise(data, mob);
            }
            default -> {
                data.tickWound(dt);
            }
        }
    }

    /** Boss 66%/33% 阶段跨越时命势上限 +5%。 */
    private static void tickBossPhase(Mob mob, MobFateData data) {
        PoolProfile.Rank rank = data.profile() == null ? PoolProfile.Rank.NORMAL : data.profile().rank();
        if (rank != PoolProfile.Rank.BOSS) return;
        float frac = mob.getHealth() / mob.getMaxHealth();
        int phase = frac <= 0.33F ? 2 : frac <= 0.66F ? 1 : 0;
        while (data.phaseGains() < phase && data.phaseGains() < 10) {
            data.addPhaseGain();
            double cap = DFConfig.POISE_MAX_GAIN_CAP.get();
            double mult = Math.min(1 + cap, 1 + DFConfig.POISE_MAX_GAIN_PER_BREAK.get() * data.phaseGains());
            data.setMaxPoise((int) Math.floor(rawMax(mob, data) * mult));
        }
    }

    private static double rawMax(Mob mob, MobFateData data) {
        PoolProfile.Rank rank = data.profile() == null ? PoolProfile.Rank.NORMAL : data.profile().rank();
        return switch (rank) {
            case ELITE -> mob.getMaxHealth() * DFConfig.POISE_ELITE_MULT.get() + DFConfig.POISE_ELITE_FLAT.get();
            case BOSS, GUARD -> mob.getMaxHealth() * DFConfig.POISE_BOSS_MULT.get();
            default -> mob.getMaxHealth() * DFConfig.POISE_NORMAL_MULT.get() + DFConfig.POISE_NORMAL_FLAT.get();
        };
    }

    /** 死亡/卸载清理。 */
    public static void cleanup(Mob mob) {
        MobFateData data = mob.getData(ModAttachments.MOB_FATE);
        if (data.isInitialized() && data.state() == MobFateData.State.EXHAUSTED) {
            applyExhaustModifiers(mob, false);
        }
        AttributeInstance arm = mob.getAttribute(Attributes.ARMOR);
        if (arm != null) arm.removeModifier(DiceFate.id("corrode/" + mob.getUUID()));
    }
}
