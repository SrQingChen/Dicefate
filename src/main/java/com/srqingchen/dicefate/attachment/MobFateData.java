package com.srqingchen.dicefate.attachment;

import com.srqingchen.dicefate.data.PoolProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 怪物/Boss 命势数据（运行时态，不序列化：卸载即重置，符合"怪物是命盘"的世界观）。
 */
public class MobFateData {
    public enum State { NORMAL, EXHAUSTED, RECOVERING }

    private PoolProfile profile;            // 缓存档案
    private boolean initialized;

    private int poise;
    private int maxPoise;
    private State state = State.NORMAL;
    private int stateTicks;                 // EXHAUSTED 剩余 / RECOVERING 累计
    private int breakCount;                 // 破势次数（上限增幅用）

    private int decree;                     // 命定累计
    private int decreeThreshold;
    private boolean decreeTriggered;        // 本次散竭已触发命破

    private int woundTicks;                 // 命损剩余

    private int contestCount;               // 参与拼点计数（Boss 每第 N 次）
    private long lastCombatTick = Long.MIN_VALUE;
    private boolean outOfCombatResolved;
    /** 0.2.0：上次 PoiseService.tick 的 gameTime（dt 精确扣减）。 */
    private long lastPoiseTick = Long.MIN_VALUE;
    /** 0.2.0：Boss 阶段增益次数（66%/33% 各 +5% 上限）。 */
    private int phaseGains;
    /** 0.2.0：腐蚀（护甲 -15%）到期 gameTime；0=无。 */
    private long corrodeUntil;
    private long corrodeAtkUntil;   // 0.3.0 验收修复④：防御腐蚀（攻击-10%）独立到期，不复用护甲腐蚀字段
    /** 0.2.0：怪物技能骰 CD（dieId path → ready gameTime）。 */
    private final Map<String, Long> skillCdReady = new HashMap<>();

    /** 最近 10s 造成过命势伤害的玩家（多人破势奖励防挂机）。 */
    private final Map<UUID, Long> recentPoiseAttackers = new HashMap<>();

    /** 骰面随档案（冻结/清零不改档案，掷骰时按状态处理）。 */

    public void init(PoolProfile profile, int maxPoise, int decreeThreshold) {
        this.profile = profile;
        this.maxPoise = maxPoise;
        this.poise = maxPoise;
        this.decreeThreshold = decreeThreshold;
        this.initialized = true;
    }

    public boolean isInitialized() { return initialized; }
    public PoolProfile profile() { return profile; }

    public int poise() { return poise; }
    public int maxPoise() { return maxPoise; }
    public void setPoise(int v) { poise = Math.max(0, Math.min(maxPoise, v)); }

    public State state() { return state; }
    public void setState(State s, int ticks) { state = s; stateTicks = ticks; }
    public int stateTicks() { return stateTicks; }
    public void tickState() { if (stateTicks > 0) stateTicks--; }
    /** 0.2.0：按实际经过的 tick 数扣减（修复批量 tick 10 倍时长 bug）。 */
    public void tickState(int dt) { if (stateTicks > 0) stateTicks = Math.max(0, stateTicks - dt); }

    public int breakCount() { return breakCount; }
    public void addBreak() { breakCount++; }

    /** 0.2.0：离场重置（防循环三件套归零）。 */
    public void resetBreaks(int newMaxPoise, int baseDecreeThreshold) {
        breakCount = 0;
        phaseGains = 0;
        maxPoise = newMaxPoise;
        poise = Math.min(poise, maxPoise);
        decreeThreshold = baseDecreeThreshold;
        decree = 0;
    }

    public void setMaxPoise(int v) { maxPoise = Math.max(1, v); }

    public int decree() { return decree; }
    public void addDecree(int v) { decree += v; }
    public void setDecree(int v) { decree = v; }
    public int decreeThreshold() { return decreeThreshold; }
    public void setDecreeThreshold(int v) { decreeThreshold = v; }
    public boolean isDecreeTriggered() { return decreeTriggered; }
    public void setDecreeTriggered(boolean v) { decreeTriggered = v; }

    public int woundTicks() { return woundTicks; }
    public void setWoundTicks(int t) { woundTicks = t; }
    public void tickWound() { if (woundTicks > 0) woundTicks--; }
    public void tickWound(int dt) { if (woundTicks > 0) woundTicks = Math.max(0, woundTicks - dt); }

    /** 0.2.0：破势骰的命破加成百分比（破势瞬间由钩子写入，命破触发时消费）。 */
    private double pendingBreakerPct;
    public void setPendingBreakerPct(double pct) { pendingBreakerPct = pct; }
    public double consumePendingBreakerPct() { double v = pendingBreakerPct; pendingBreakerPct = 0; return v; }

    public int contestCount() { return contestCount; }
    public int incrementContest() { return ++contestCount; }

    public long lastCombatTick() { return lastCombatTick; }
    public void setLastCombatTick(long t) { lastCombatTick = t; outOfCombatResolved = false; }
    public boolean isOutOfCombatResolved() { return outOfCombatResolved; }
    public void markOutOfCombatResolved() { outOfCombatResolved = true; }

    public long lastPoiseTick() { return lastPoiseTick; }
    public void setLastPoiseTick(long t) { lastPoiseTick = t; }

    public int phaseGains() { return phaseGains; }
    public void addPhaseGain() { phaseGains++; }

    public long corrodeUntil() { return corrodeUntil; }
    public void setCorrodeUntil(long t) { corrodeUntil = t; }
    public long corrodeAtkUntil() { return corrodeAtkUntil; }
    public void setCorrodeAtkUntil(long t) { corrodeAtkUntil = t; }

    public long skillCdRemaining(String diePath, long tick) {
        return Math.max(0, skillCdReady.getOrDefault(diePath, 0L) - tick);
    }
    public void setSkillCdReady(String diePath, long readyTick) { skillCdReady.put(diePath, readyTick); }

    public void notePoiseAttacker(UUID player, long tick) { recentPoiseAttackers.put(player, tick); }
    public Map<UUID, Long> recentPoiseAttackers() { return recentPoiseAttackers; }

    public boolean isExhausted() { return state == State.EXHAUSTED; }
    public boolean hasWound() { return woundTicks > 0; }
}
