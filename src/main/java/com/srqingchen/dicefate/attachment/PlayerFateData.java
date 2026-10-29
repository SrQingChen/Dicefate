package com.srqingchen.dicefate.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.srqingchen.dicefate.data.PoolProfile;
import com.srqingchen.dicefate.dice.DicePool;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 玩家命数数据（附件，copyOnDeath：死亡保留骰位/图鉴/骰盘）。
 * 全部服务端权威；HUD 相关字段经 FateStatePayload/PoolSyncPayload 同步。
 */
public class PlayerFateData {
    // —— 骰位 ——
    private int baseSlots = 1;                       // 1..6
    private final boolean[] usedExpansion = new boolean[5]; // I..V
    private boolean addSlotUnlocked;
    private boolean mulSlotUnlocked;
    private int skillSlotsUnlocked;

    // —— 命盘 ——
    private final DicePool pool = new DicePool();
    /** 3 套预设（命线织机）。 */
    private final List<DicePool> presets = List.of(new DicePool(), new DicePool(), new DicePool());

    // —— 姿态 ——
    private PoolProfile.Stance stance = PoolProfile.Stance.BALANCED;
    private final Map<String, Integer> stanceMastery = new HashMap<>();
    private long stanceSwapReadyTick;
    private long swapHardUntilTick;

    // —— 骰势 ——
    private int momentum;
    private int momentumOverflow;
    private long lastContestTick = Long.MIN_VALUE;
    private long momentumPeakTick = Long.MIN_VALUE;
    private int momentumFreeNegateUsed;
    private long momentumFreeNegateCdTick;

    // —— 兑现 ——
    private boolean cashoutArmed;
    private long cashoutExpireTick;
    private int cashoutValue;

    // —— 保底 ——
    private int pityStacks;

    // —— 技能骰冷却（技能槽 0/1 readyTick） ——
    private final long[] skillCdUntil = new long[2];

    // —— 骰包 ——
    private Identifier activePack;
    private int packRemaining;

    // —— 定命 ——
    private int lockSlot = -1;
    private int lockValue;
    private boolean lockSpent;
    private int skillLockSlot = -1;
    private boolean skillLockConsumed;

    // —— 骰灵 ——
    private boolean spiritActive;
    private int spiritLevel = 1;

    // —— 命途（玩家命纹） ——
    private Identifier playerRune;

    // —— 图鉴/情报 ——
    private final Set<Identifier> codex = new HashSet<>();
    private final Map<Identifier, Integer> killCounts = new HashMap<>();

    // —— 每场一次命纹 ——
    private boolean rerollRuneUsed;
    private boolean reverseRuneUsed;
    /** 0.2.0：幸运骰坠每场一次标记（战斗窗口内有效）。 */
    private boolean pendantUsedThisBattle;

    // —— 敌方机制临时效果 ——
    private int frozenSlotIdx = -1;
    private int frozenSlotContests;
    private int nextContestMinusTotal;
    private int loomCursor;

    private int unidentifiedOpened;
    private boolean kitGiven;
    private final Map<Identifier, Long> reactionCds = new HashMap<>();
    /** 0.2.0：骰袋储存（6 格备用骰，绑定玩家）。 */
    private final List<ItemStack> pouchStorage = new ArrayList<>(java.util.Collections.nCopies(6, ItemStack.EMPTY));

    // —— 能力解锁与药剂/食物效果（runtime） ——
    private boolean lockUnlocked;        // 定命符解锁
    private int aimCharges;              // 命准药剂：剩余拼点次数（总点数+1）
    private int guardCharges;            // 命硬药剂：剩余拼点次数（减伤+10%）
    private int nextPackBonus;           // 幸运饼干：下一骰包 +1 次
    private int sightTicks;              // 命视药剂：剩余 tick

    public PlayerFateData() {}

    // ==================== 保存结构（group ≤16 参数 → 3 子记录） ====================

    public record BoardSave(int baseSlots, List<Boolean> usedExpansions, boolean addSlot, boolean mulSlot,
                            int skillSlots, PoolCodec pool, List<ItemStack> pouch) {
        public static final BoardSave EMPTY = new BoardSave(1, List.of(), false, false, 0, PoolCodec.EMPTY, List.of());
        public static final MapCodec<BoardSave> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 6).optionalFieldOf("base_slots", 1).forGetter(BoardSave::baseSlots),
            Codec.BOOL.listOf().optionalFieldOf("used_expansion", List.<Boolean>of()).forGetter(BoardSave::usedExpansions),
            Codec.BOOL.optionalFieldOf("add_slot", false).forGetter(BoardSave::addSlot),
            Codec.BOOL.optionalFieldOf("mul_slot", false).forGetter(BoardSave::mulSlot),
            Codec.intRange(0, 2).optionalFieldOf("skill_slots", 0).forGetter(BoardSave::skillSlots),
            PoolCodec.CODEC.optionalFieldOf("pool", PoolCodec.EMPTY).forGetter(BoardSave::pool),
            ItemStack.OPTIONAL_CODEC.sizeLimitedListOf(8).optionalFieldOf("pouch", List.of()).forGetter(BoardSave::pouch)
        ).apply(i, BoardSave::new));
    }

    public record CombatSave(String stance, Map<String, Integer> mastery, int momentum,
                             int momentumOverflow, int pity, long lastContestTick) {
        public static final CombatSave EMPTY = new CombatSave("balanced", Map.of(), 0, 0, 0, Long.MIN_VALUE);
        public static final MapCodec<CombatSave> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("stance", "balanced").forGetter(CombatSave::stance),
            Codec.unboundedMap(Codec.STRING, Codec.intRange(0, 3)).optionalFieldOf("mastery", Map.<String, Integer>of()).forGetter(CombatSave::mastery),
            Codec.intRange(0, 200).optionalFieldOf("momentum", 0).forGetter(CombatSave::momentum),
            Codec.intRange(0, 100).optionalFieldOf("momentum_overflow", 0).forGetter(CombatSave::momentumOverflow),
            Codec.intRange(0, 5).optionalFieldOf("pity", 0).forGetter(CombatSave::pity),
            Codec.LONG.optionalFieldOf("last_contest_tick", Long.MIN_VALUE).forGetter(CombatSave::lastContestTick)
        ).apply(i, CombatSave::new));
    }

    public record JourneySave(String pack, int packLeft, int lockSlot, int lockValue, boolean lockSpent,
                              boolean spiritActive, int spiritLevel, Optional<Identifier> playerRune,
                              boolean lockUnlocked,
                              List<Identifier> codex, Map<Identifier, Integer> kills) {
        public static final JourneySave EMPTY = new JourneySave("", 0, -1, 0, false, false, 1, Optional.empty(), false, List.of(), Map.of());
        public static final MapCodec<JourneySave> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("pack", "").forGetter(JourneySave::pack),
            Codec.intRange(0, 8).optionalFieldOf("pack_left", 0).forGetter(JourneySave::packLeft),
            Codec.intRange(-1, 5).optionalFieldOf("lock_slot", -1).forGetter(JourneySave::lockSlot),
            Codec.intRange(0, 24).optionalFieldOf("lock_value", 0).forGetter(JourneySave::lockValue),
            Codec.BOOL.optionalFieldOf("lock_spent", false).forGetter(JourneySave::lockSpent),
            Codec.BOOL.optionalFieldOf("spirit_active", false).forGetter(JourneySave::spiritActive),
            Codec.intRange(1, 3).optionalFieldOf("spirit_level", 1).forGetter(JourneySave::spiritLevel),
            Identifier.CODEC.optionalFieldOf("player_rune").forGetter(JourneySave::playerRune),
            Codec.BOOL.optionalFieldOf("lock_unlocked", false).forGetter(JourneySave::lockUnlocked),
            Identifier.CODEC.listOf().optionalFieldOf("codex", List.<Identifier>of()).forGetter(JourneySave::codex),
            Codec.unboundedMap(Identifier.CODEC, Codec.intRange(0, 10000)).optionalFieldOf("kills", Map.<Identifier, Integer>of()).forGetter(JourneySave::kills)
        ).apply(i, JourneySave::new));
    }

    public BoardSave boardSave() {
        List<Boolean> used = new ArrayList<>(5);
        for (boolean b : usedExpansion) used.add(b);
        return new BoardSave(baseSlots, used, addSlotUnlocked, mulSlotUnlocked, skillSlotsUnlocked,
            PoolCodec.of(pool), List.copyOf(pouchStorage));
    }

    public CombatSave combatSave() {
        return new CombatSave(stance.getSerializedName(), Map.copyOf(stanceMastery), momentum, momentumOverflow, pityStacks, lastContestTick);
    }

    public JourneySave journeySave() {
        return new JourneySave(activePack == null ? "" : activePack.toString(), packRemaining,
            lockSlot, lockValue, lockSpent, spiritActive, spiritLevel,
            Optional.ofNullable(playerRune), lockUnlocked, List.copyOf(codex), Map.copyOf(killCounts));
    }

    /** 0.2.0：运行时但需防重登刷的字段（技能 CD/反应 CD/药剂次数）。时钟口径为 gameTime（随存档持久）。 */
    public record RuntimeSave(long skillCd0, long skillCd1, Map<String, Long> reactionCds,
                              int aim, int guard) {
        public static final RuntimeSave EMPTY = new RuntimeSave(0, 0, Map.of(), 0, 0);
        public static final MapCodec<RuntimeSave> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("skill_cd_0", 0L).forGetter(RuntimeSave::skillCd0),
            Codec.LONG.optionalFieldOf("skill_cd_1", 0L).forGetter(RuntimeSave::skillCd1),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("reaction_cds", Map.<String, Long>of()).forGetter(RuntimeSave::reactionCds),
            Codec.intRange(0, 64).optionalFieldOf("aim", 0).forGetter(RuntimeSave::aim),
            Codec.intRange(0, 64).optionalFieldOf("guard", 0).forGetter(RuntimeSave::guard)
        ).apply(i, RuntimeSave::new));
    }

    public RuntimeSave runtimeSave() {
        Map<String, Long> cds = new HashMap<>();
        reactionCds.forEach((k, v) -> cds.put(k.toString(), v));
        return new RuntimeSave(skillCdUntil[0], skillCdUntil[1], cds, aimCharges, guardCharges);
    }

    public static final MapCodec<PlayerFateData> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        BoardSave.CODEC.codec().optionalFieldOf("board", BoardSave.EMPTY).forGetter(PlayerFateData::boardSave),
        CombatSave.CODEC.codec().optionalFieldOf("combat", CombatSave.EMPTY).forGetter(PlayerFateData::combatSave),
        JourneySave.CODEC.codec().optionalFieldOf("journey", JourneySave.EMPTY).forGetter(PlayerFateData::journeySave),
        RuntimeSave.CODEC.codec().optionalFieldOf("runtime", RuntimeSave.EMPTY).forGetter(PlayerFateData::runtimeSave)
    ).apply(i, PlayerFateData::new));

    public static final Codec<PlayerFateData> CODEC = MAP_CODEC.codec();

    private PlayerFateData(BoardSave board, CombatSave combat, JourneySave journey, RuntimeSave runtime) {
        baseSlots = board.baseSlots();
        for (int k = 0; k < Math.min(5, board.usedExpansions().size()); k++) usedExpansion[k] = board.usedExpansions().get(k);
        addSlotUnlocked = board.addSlot();
        mulSlotUnlocked = board.mulSlot();
        skillSlotsUnlocked = board.skillSlots();
        board.pool().copyTo(pool);
        for (int k = 0; k < 6; k++) {
            pouchStorage.set(k, k < board.pouch().size() ? board.pouch().get(k) : ItemStack.EMPTY);
        }
        stance = stanceFrom(combat.stance());
        stanceMastery.putAll(combat.mastery());
        momentum = combat.momentum();
        momentumOverflow = combat.momentumOverflow();
        pityStacks = combat.pity();
        // 0.2.0 验收修复 X3：持久化 lastContestTick，重登不再被 30s 衰减档清零骰势
        if (combat.lastContestTick() != Long.MIN_VALUE) lastContestTick = combat.lastContestTick();
        if (!journey.pack().isEmpty()) {
            activePack = Identifier.parse(journey.pack());
            packRemaining = journey.packLeft();
        }
        lockSlot = journey.lockSlot();
        lockValue = journey.lockValue();
        lockSpent = journey.lockSpent();
        spiritActive = journey.spiritActive();
        spiritLevel = journey.spiritLevel();
        journey.playerRune().ifPresent(r -> playerRune = r);
        lockUnlocked = journey.lockUnlocked();
        codex.addAll(journey.codex());
        killCounts.putAll(journey.kills());
        // 0.2.0：恢复运行时字段（防重登刷 CD）
        skillCdUntil[0] = runtime.skillCd0();
        skillCdUntil[1] = runtime.skillCd1();
        runtime.reactionCds().forEach((k, v) -> {
            try { reactionCds.put(Identifier.parse(k), v); } catch (Exception ignored) {}
        });
        aimCharges = runtime.aim();
        guardCharges = runtime.guard();
    }

    private static PoolProfile.Stance stanceFrom(String s) {
        for (PoolProfile.Stance st : PoolProfile.Stance.values()) {
            if (st.getSerializedName().equals(s)) return st;
        }
        return PoolProfile.Stance.BALANCED;
    }

    // ==================== 骰位 ====================
    public int getBaseSlots() { return baseSlots; }

    public boolean useExpansion(int level0) {
        if (level0 < 0 || level0 > 4) return false;
        if (usedExpansion[level0]) return false;
        if (baseSlots >= DicePool.MAX_BASE_SLOTS) return false;
        usedExpansion[level0] = true;
        baseSlots++;
        return true;
    }

    public boolean hasUsedExpansion(int level0) { return usedExpansion[level0]; }
    public boolean isAddSlotUnlocked() { return addSlotUnlocked; }
    public void unlockAddSlot() { addSlotUnlocked = true; }
    public boolean isMulSlotUnlocked() { return mulSlotUnlocked; }
    public void unlockMulSlot() { mulSlotUnlocked = true; }
    public int getSkillSlotsUnlocked() { return skillSlotsUnlocked; }
    public void unlockSkillSlot() { if (skillSlotsUnlocked < 2) skillSlotsUnlocked++; }

    // ==================== 命盘 ====================
    public DicePool pool() { return pool; }
    public List<DicePool> presets() { return presets; }

    // ==================== 姿态 ====================
    public PoolProfile.Stance stance() { return stance; }
    public void setStance(PoolProfile.Stance s) { this.stance = s; }
    public int stanceMastery(PoolProfile.Stance s) { return stanceMastery.getOrDefault(s.getSerializedName(), 0); }
    public void setStanceMastery(PoolProfile.Stance s, int lv) { stanceMastery.put(s.getSerializedName(), Math.max(0, Math.min(3, lv))); }
    public long stanceSwapReadyTick() { return stanceSwapReadyTick; }
    public void setStanceSwapReadyTick(long t) { stanceSwapReadyTick = t; }
    public long swapHardUntilTick() { return swapHardUntilTick; }
    public void setSwapHardUntilTick(long t) { swapHardUntilTick = t; }

    // ==================== 骰势 ====================
    public int getMomentum() { return momentum; }

    /** 设置骰势（超上限部分进溢出缓存；到顶记录峰值 tick）。
     *  0.3.0：上限/溢出缓存接线 DFConfig（原硬编码 100/20 死配置）。 */
    public void setMomentum(int v, long tick) {
        int max = com.srqingchen.dicefate.DFConfig.MOMENTUM_MAX.get();
        int overflowCap = com.srqingchen.dicefate.DFConfig.MOMENTUM_OVERFLOW_CACHE.get();
        if (v > max) momentumOverflow = Math.min(overflowCap, momentumOverflow + (v - max));
        int nv = Math.max(0, Math.min(max, v));
        if (nv >= max) momentumPeakTick = tick;
        this.momentum = nv;
    }

    /** 标记一次拼点发生（攻击/防御/共享攻击实例均调用；衰减 idle 从此起算）。 */
    public void markContest(long tick) { this.lastContestTick = tick; }

    public int getMomentumOverflow() { return momentumOverflow; }
    public void consumeOverflow(int n) { momentumOverflow = Math.max(0, momentumOverflow - n); }
    public long lastContestTick() { return lastContestTick; }
    public void setLastContestTick(long t) { lastContestTick = t; }
    public long momentumPeakTick() { return momentumPeakTick; }
    public boolean momentumFreeNegateReady(long tick) { return momentumFreeNegateUsed == 0 && tick >= momentumFreeNegateCdTick; }
    public void useMomentumFreeNegate() { momentumFreeNegateUsed = 1; }
    /** III 层免扣 CD 结束后重新可用。 */
    public void refreshMomentumFreeNegate(long tick) {
        if (momentumFreeNegateUsed == 1 && tick >= momentumFreeNegateCdTick) momentumFreeNegateUsed = 0;
    }
    public void setMomentumFreeNegateCd(long t) { momentumFreeNegateCdTick = t; momentumFreeNegateUsed = 1; }

    // ==================== 兑现 ====================
    public boolean isCashoutArmed() { return cashoutArmed; }
    public long cashoutExpireTick() { return cashoutExpireTick; }
    public int cashoutValue() { return cashoutValue; }
    public void armCashout(int n, long expireTick) { cashoutArmed = true; cashoutValue = n; cashoutExpireTick = expireTick; }
    public void disarmCashout() { cashoutArmed = false; cashoutValue = 0; }

    // ==================== 保底 ====================
    public int pityStacks() { return pityStacks; }
    public void setPityStacks(int v) { pityStacks = Math.max(0, Math.min(5, v)); }

    // ==================== 技能骰 CD ====================
    public long skillCdUntil(int slot) { return skillCdUntil[slot]; }
    public void setSkillCdUntil(int slot, long tick) { skillCdUntil[slot] = tick; }

    // ==================== 骰包 ====================
    public Identifier activePack() { return activePack; }
    public int packRemaining() { return packRemaining; }
    public void setPack(Identifier id, int duration) { activePack = id; packRemaining = duration; }
    public void decrementPack() { if (packRemaining > 0) packRemaining--; if (packRemaining <= 0) activePack = null; }
    public void clearPack() { activePack = null; packRemaining = 0; }

    // ==================== 定命 ====================
    public int lockSlot() { return lockSlot; }
    public int lockValue() { return lockValue; }
    public boolean isLockSpent() { return lockSpent; }
    public void setLock(int slot, int value) { lockSlot = slot; lockValue = value; lockSpent = false; }
    public void spendLock() { lockSpent = true; lockSlot = -1; lockValue = 0; }
    public void clearLock() { lockSlot = -1; lockValue = 0; lockSpent = false; }
    public int skillLockSlot() { return skillLockSlot; }
    public void setSkillLockSlot(int s) { skillLockSlot = s; skillLockConsumed = false; }
    public void clearSkillLock() { skillLockSlot = -1; skillLockConsumed = false; }
    /** 快照消费定命骰时打标记；结算收尾只清除已消费的（0.2.0 A2 修复）。 */
    public boolean isSkillLockConsumed() { return skillLockConsumed; }
    public void markSkillLockConsumed() { skillLockConsumed = true; }

    // ==================== 骰灵 ====================
    public boolean isSpiritActive() { return spiritActive; }
    public void setSpiritActive(boolean v) { spiritActive = v; }
    public int spiritLevel() { return spiritLevel; }
    public void setSpiritLevel(int lv) { spiritLevel = Math.max(1, Math.min(3, lv)); }

    // ==================== 命途 ====================
    public Identifier playerRune() { return playerRune; }
    public void setPlayerRune(Identifier r) { playerRune = r; }

    // ==================== 图鉴/情报 ====================
    public Set<Identifier> codex() { return codex; }
    public void learnDie(Identifier id) { codex.add(id); }
    public Map<Identifier, Integer> killCounts() { return killCounts; }
    public int addKill(Identifier entity) { return killCounts.merge(entity, 1, Integer::sum); }

    // ==================== 每场一次命纹 ====================
    public boolean isRerollRuneUsed() { return rerollRuneUsed; }
    public void setRerollRuneUsed() { rerollRuneUsed = true; }
    public boolean isReverseRuneUsed() { return reverseRuneUsed; }
    public void setReverseRuneUsed() { reverseRuneUsed = true; }
    public boolean isPendantUsedThisBattle() { return pendantUsedThisBattle; }
    public void markPendantUsed() { pendantUsedThisBattle = true; }

    // ==================== 每场战斗计数（0.3.0） ====================
    /** 定命使用次数（每场上限 = 1 + 定命指环 1；规划 3.6）。 */
    private int locksUsedThisBattle;
    public int locksUsedThisBattle() { return locksUsedThisBattle; }
    public void addLockUsedThisBattle() { locksUsedThisBattle++; }

    /** 咒纹裂面禁用的骰（DieState.uuid，本场；规划 4.4 咒）。 */
    private final java.util.Set<java.util.UUID> crackedDice = new java.util.HashSet<>();
    public boolean isCrackedThisBattle(java.util.UUID uuid) { return uuid != null && crackedDice.contains(uuid); }
    public void markCrackedThisBattle(java.util.UUID uuid) { if (uuid != null) crackedDice.add(uuid); }

    /** 唯一骰每场一次效果标记（双面赌徒翻面/命主复制，0.3.0 F2）。 */
    private boolean twoFacedUsedThisBattle;
    private boolean sovereignUsedThisBattle;
    public boolean isTwoFacedUsedThisBattle() { return twoFacedUsedThisBattle; }
    public void markTwoFacedUsed() { twoFacedUsedThisBattle = true; }
    public boolean isSovereignUsedThisBattle() { return sovereignUsedThisBattle; }
    public void markSovereignUsed() { sovereignUsedThisBattle = true; }

    public void resetBattleOnceFlags() {
        rerollRuneUsed = false; reverseRuneUsed = false; pendantUsedThisBattle = false;
        locksUsedThisBattle = 0; crackedDice.clear();
        twoFacedUsedThisBattle = false; sovereignUsedThisBattle = false;
    }

    // ==================== 敌方机制临时效果 ====================
    public int frozenSlotIdx() { return frozenSlotIdx; }
    public int frozenSlotContests() { return frozenSlotContests; }
    public void freezeSlot(int idx, int contests) { frozenSlotIdx = idx; frozenSlotContests = contests; }
    public void decrementFrozen() { if (frozenSlotContests > 0) frozenSlotContests--; if (frozenSlotContests <= 0) frozenSlotIdx = -1; }
    public int nextContestMinusTotal() { return nextContestMinusTotal; }
    public int loomCursor() { return loomCursor; }
    public void setLoomCursor(int v) { loomCursor = ((v % 3) + 3) % 3; }
    public void setNextContestMinusTotal(int v) { nextContestMinusTotal = v; }

    public int unidentifiedOpened() { return unidentifiedOpened; }
    public boolean isKitGiven() { return kitGiven; }
    public void markKitGiven() { kitGiven = true; }

    // ==================== 骰袋储存（0.2.0） ====================
    public ItemStack pouchAt(int idx) { return idx >= 0 && idx < 6 ? pouchStorage.get(idx) : ItemStack.EMPTY; }
    public void setPouchAt(int idx, ItemStack stack) {
        if (idx >= 0 && idx < 6) pouchStorage.set(idx, stack == null ? ItemStack.EMPTY : stack);
    }
    public List<ItemStack> pouchView() { return List.copyOf(pouchStorage); }
    public boolean hasPouchSpace() {
        for (ItemStack s : pouchStorage) if (s.isEmpty()) return true;
        return false;
    }

    public long reactionCdRemaining(Identifier id, long tick) {
        return Math.max(0, reactionCds.getOrDefault(id, 0L) - tick);
    }
    public void setReactionCd(Identifier id, long readyTick) {
        reactionCds.put(id, readyTick);
    }
    public boolean isLockUnlocked() { return lockUnlocked; }
    public void unlockLock() { lockUnlocked = true; }
    public int aimCharges() { return aimCharges; }
    public void consumeAim() { if (aimCharges > 0) aimCharges--; }
    public void addAimCharges(int n) { aimCharges += n; }
    public int guardCharges() { return guardCharges; }
    public void consumeGuard() { if (guardCharges > 0) guardCharges--; }
    public void addGuardCharges(int n) { guardCharges += n; }
    public int nextPackBonus() { return nextPackBonus; }
    public void setNextPackBonus(int v) { nextPackBonus = v; }
    public int sightTicks() { return sightTicks; }
    public void setSightTicks(int t) { sightTicks = t; }
    public void addUnidentifiedOpened() { unidentifiedOpened++; }
    public void resetUnidentifiedStreak() { unidentifiedOpened = 0; }

    /** DicePool 的编解码包装。 */
    public record PoolCodec(List<ItemStack> base, ItemStack add, ItemStack mul, ItemStack s0, ItemStack s1) {
        public static final PoolCodec EMPTY = new PoolCodec(List.of(), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        public static final Codec<PoolCodec> CODEC = RecordCodecBuilder.create(i -> i.group(
            // 0.3.1 修复：空槽用 OPTIONAL_CODEC——严格 CODEC 拒绝空物品栈（count 0/air），
            // 导致整个 PlayerFateData 附件序列化失败（生产日志"Failed to merge"×5，每次存档丢数据）
            ItemStack.OPTIONAL_CODEC.sizeLimitedListOf(64).optionalFieldOf("base", List.of()).forGetter(PoolCodec::base),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("add", ItemStack.EMPTY).forGetter(PoolCodec::add),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("mul", ItemStack.EMPTY).forGetter(PoolCodec::mul),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("s0", ItemStack.EMPTY).forGetter(PoolCodec::s0),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("s1", ItemStack.EMPTY).forGetter(PoolCodec::s1)
        ).apply(i, PoolCodec::new));

        public static PoolCodec of(DicePool p) {
            return new PoolCodec(List.copyOf(p.base()), p.addDie(), p.mulDie(), p.skill0(), p.skill1());
        }

        public void copyTo(DicePool p) {
            for (int k = 0; k < DicePool.MAX_BASE_SLOTS; k++) {
                p.setBase(k, k < base.size() ? base.get(k) : ItemStack.EMPTY);
            }
            p.setAddDie(add);
            p.setMulDie(mul);
            p.setSkill0(s0);
            p.setSkill1(s1);
        }
    }
}
