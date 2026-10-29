# -*- coding: utf-8 -*-
"""审核修复批 2：F1 共享掷骰、F3 packBonus/comboSkillBonus、F4 破势骰、L1 织机序号。"""

# ---- F1: 共享攻击掷骰实现 ----
p = 'src/main/java/com/srqingchen/dicefate/combat/ContestService.java'
s = open(p, encoding='utf-8').read()

s = s.replace('''            // —— 2. 攻击掷骰（多目标共享） ——
            SharedAttack shared = sharedAttacks.computeIfAbsent(attacker.getUUID(), u -> new SharedAttack[0] == null ? null : null);
            // 直接计算（同 tick 复用见下）
            RollResult atkRoll;
            PoolSnapshot useSnap = atkSnap;
            SharedAttack cached = sharedAttacks.get(attacker.getUUID());
            if (cached != null && cached.tick == tick && cached.roll != null) {
                atkRoll = cached.roll;
                // 共享的定命/冰封已在首次应用；不重复消耗
            } else {
                atkRoll = DiceRoller.roll(atkSnap, rng);
            }''',
'''            // —— 2. 攻击掷骰（同 tick 多目标共享一次攻击掷骰） ——
            SharedAttack cached = sharedAttacks.get(attacker.getUUID());
            boolean firstOfAttack = cached == null || cached.tick() != tick;
            RollResult atkRoll;
            if (firstOfAttack) {
                atkRoll = DiceRoller.roll(atkSnap, rng);
                sharedAttacks.put(attacker.getUUID(),
                    new SharedAttack(tick, atkRoll, atkSnap, null, null, 0, 0, List.of()));
                sharedAttacks.values().removeIf(sa -> sa.tick() < tick - 1);
            } else {
                atkRoll = cached.roll();
            }''')

# 技能段与保底段条件更新（共享攻击下仅首目标触发消耗类修正）
s = s.replace('''            List<Byte> triggers = new ArrayList<>();
            boolean pityUsed = false;
            if (!cashoutUsed && cached == null) {''',
'''            List<Byte> triggers = new ArrayList<>();
            boolean pityUsed = false;
            if (!cashoutUsed && firstOfAttack) {''')

# F3: packBonus —— 由骰包定义提供增伤
s = s.replace('''            double bucket = 0;
            bucket += pattern.damageBonus;''',
'''            double bucket = 0;
            bucket += pattern.damageBonus;
            packBonus = packDamageBonus(attacker, data);''')

# F3: comboSkillBonus —— 加算骰·连（胜利时骰势 +3）
s = s.replace('''            if (outcome.isWin()) {
                int gain = MomentumService.gainFor(outcome) + comboSkillBonus;''',
'''            comboSkillBonus = addDieIsCombo(data) ? 3 : 0;
            if (outcome.isWin()) {
                int gain = MomentumService.gainFor(outcome) + comboSkillBonus;''')

# 工具方法
s = s.replace('''    private static int weaponTotalBonus(ServerPlayer player) {''',
'''    /** F3：骰包增伤（命运包等高阶包 +10%）。 */
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

    private static int weaponTotalBonus(ServerPlayer player) {''')

# F4: 破势骰判定移到 applyPoiseDamage 的破势预判
s = s.replace('''                if (dealt > 0 && mob.isAlive()) {
                    MobFateData mobData = mob.getData(ModAttachments.MOB_FATE);
                    int poise = hit.poiseDamage();
                    // 破势骰：破势瞬间额外命势
                    poise += breakerBonus(hit.attacker(), mobData, mob);
                    int applied = PoiseService.applyPoiseDamage(mob, mobData, poise, hit.attacker());''',
'''                if (dealt > 0 && mob.isAlive()) {
                    MobFateData mobData = mob.getData(ModAttachments.MOB_FATE);
                    int poise = hit.poiseDamage();
                    int applied = PoiseService.applyPoiseDamage(mob, mobData, poise, hit.attacker());''')

s = s.replace('''    /** 破势骰：破势瞬间 +10/15/20 命势、命破 +5/8/10%。 */
    private static int breakerBonus(ServerPlayer player, MobFateData mobData, Mob mob) {
        if (mobData.poise() > 0 || mobData.state() != MobFateData.State.NORMAL) return 0;''',
'''    /** 破势骰：破势瞬间 +10/15/20 命势、命破 +5/8/10%。 */
    private static int breakerBonus(ServerPlayer player, MobFateData mobData, Mob mob, int incomingPoise) {
        if (mobData.state() != MobFateData.State.NORMAL) return 0;
        // F4：预判本次是否破势（单次上限内的伤害将命势打到 ≤0）
        int cap = Math.max(1, (int) (mobData.maxPoise() * (mobData.profile() != null
            && mobData.profile().rank() != com.srqingchen.dicefate.data.PoolProfile.Rank.NORMAL ? 0.25 : 0.5)));
        int effective = Math.min(incomingPoise, cap);
        if (mobData.poise() - effective > 0) return 0;''')

open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('F1/F3/F4 staged')

# F4 落点：在 PoiseService.applyPoiseDamage 里破势前调用 breaker —— 为避免循环依赖，
# 采用回调：PoiseService 增加静态钩子字段由 ContestService 安装。
p = 'src/main/java/com/ssrqingchen' # placeholder guard
p = 'src/main/java/com/srqingchen/dicefate/combat/PoiseService.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''        int before = data.poise();
        int after = Math.max(0, before - amount);
        data.setPoise(after);''',
'''        // F4：破势骰回调（由 ContestService 安装；破势预判命中时追加班势）
        int effective = Math.min(amount, cap);
        if (attacker != null && before - effective <= 0 && state == MobFateData.State.NORMAL && BREAKER_HOOK != null) {
            amount += BREAKER_HOOK.apply(attacker, target);
        }
        int after = Math.max(0, before - amount);
        data.setPoise(after);''')
s = s.replace('''    private static final ResourceKey<DamageType> FATEBREAK_KEY =''',
'''    /** F4：破势骰钩子（服务端安装）。 */
    public static transient java.util.function.BiFunction<ServerPlayer, Mob, Integer> BREAKER_HOOK;

    private static final ResourceKey<DamageType> FATEBREAK_KEY =''')
# 状态变量 state 在方法内不存在——用 data.state()
s = s.replace('if (attacker != null && before - effective <= 0 && state == MobFateData.State.NORMAL && BREAKER_HOOK != null) {',
              'if (attacker != null && before - effective <= 0 && data.state() == MobFateData.State.NORMAL && BREAKER_HOOK != null) {')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('F4 hook added')

# 安装钩子：breakerBonus 保留为钩子实现（签名改 4 参 → 用 2 参闭包）
p = 'src/main/java/com/srqingchen/dicefate/combat/ContestService.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''    private static int breakerBonus(ServerPlayer player, MobFateData mobData, Mob mob, int incomingPoise) {
        if (mobData.state() != MobFateData.State.NORMAL) return 0;
        // F4：预判本次是否破势（单次上限内的伤害将命势打到 ≤0）
        int cap = Math.max(1, (int) (mobData.maxPoise() * (mobData.profile() != null
            && mobData.profile().rank() != com.srqingchen.dicefate.data.PoolProfile.Rank.NORMAL ? 0.25 : 0.5)));
        int effective = Math.min(incomingPoise, cap);
        if (mobData.poise() - effective > 0) return 0;''',
'''    private static int breakerBonus(ServerPlayer player, Mob mob) {
        MobFateData mobData = mob.getData(ModAttachments.MOB_FATE);
        if (mobData.state() != MobFateData.State.NORMAL) return 0;''')
# 安装：静态块
s = s.replace('''    // 静态状态重置（测试用）''',
'''    static {
        // F4：破势骰钩子安装
        PoiseService.BREAKER_HOOK = (player, mob) -> breakerBonus(player, mob);
    }

    // 静态状态重置（测试用）''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('F4 installed')

# ---- L1: 织机序号入 PlayerFateData ----
p = 'src/main/java/com/srqingchen/dicefate/attachment/PlayerFateData.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''    private int nextContestMinusTotal;''',
'''    private int nextContestMinusTotal;
    private int loomCursor;''')
s = s.replace('''    public int nextContestMinusTotal() { return nextContestMinusTotal; }''',
'''    public int nextContestMinusTotal() { return nextContestMinusTotal; }
    public int loomCursor() { return loomCursor; }
    public void setLoomCursor(int v) { loomCursor = ((v % 3) + 3) % 3; }''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)

p = 'src/main/java/com/srqingchen/dicefate/block/StationBlocks.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''            if (sp.isShiftKeyDown()) {
                data.presets().get(loomIndex).copyFrom(data.pool());
                sp.sendOverlayMessage(Component.translatable("message.dicefate.preset_saved", loomIndex + 1));
                loomIndex = (loomIndex + 1) % 3;
            } else {
                data.pool().copyFrom(data.presets().get(loomLoad));
                com.srqingchen.dicefate.network.DFNetwork.sendPool(sp, data);
                sp.sendOverlayMessage(Component.translatable("message.dicefate.preset_loaded", loomLoad + 1));
                loomLoad = (loomLoad + 1) % 3;
            }
            return InteractionResult.CONSUME;
        }

        private int loomIndex = 0;
        private int loomLoad = 0;''',
'''            if (sp.isShiftKeyDown()) {
                int cursor = data.loomCursor();
                data.presets().get(cursor).copyFrom(data.pool());
                sp.sendOverlayMessage(Component.translatable("message.dicefate.preset_saved", cursor + 1));
                data.setLoomCursor(cursor + 1);
            } else {
                int cursor = data.loomCursor();
                data.pool().copyFrom(data.presets().get(cursor));
                com.srqingchen.dicefate.network.DFNetwork.sendPool(sp, data);
                sp.sendOverlayMessage(Component.translatable("message.dicefate.preset_loaded", cursor + 1));
                data.setLoomCursor(cursor + 1);
            }
            return InteractionResult.CONSUME;
        }''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('L1 fixed')
