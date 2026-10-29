# -*- coding: utf-8 -*-
"""复验问题修复：BUG B（MIN_VALUE 溢出）/ BUG C（镜耐久时机）/ 牌型 Trigger 发射。"""
import io

# BUG B: inCombat MIN_VALUE 溢出守卫
f = "src/main/java/com/srqingchen/dicefate/network/ServerActions.java"
s = io.open(f, encoding="utf-8").read()
old = "        boolean inCombat = tick - data.lastContestTick() < 160;"
new = ("        // 0.2.0 验收修复 BUG B：MIN_VALUE（从未拼点）下减法溢出为负会误判战斗中\n"
       "        boolean inCombat = data.lastContestTick() != Long.MIN_VALUE\n"
       "            && tick - data.lastContestTick() < 160;")
assert old in s, "inCombat expr"
s = s.replace(old, new)
old_pouch = "if (player.level().getGameTime() - data.lastContestTick() < 160) { denyInCombat(player); return; }"
new_pouch = ("if (data.lastContestTick() != Long.MIN_VALUE\n"
             "            && player.level().getGameTime() - data.lastContestTick() < 160) { denyInCombat(player); return; }")
n = s.count(old_pouch)
assert n == 2, f"pouch gates: {n}"
s = s.replace(old_pouch, new_pouch)
io.open(f, "w", encoding="utf-8", newline="").write(s)
print("BUG B fixed")

# BUG C: 镜耐久在鉴定成功后才扣
f = "src/main/java/com/srqingchen/dicefate/item/UnidentifiedDieItem.java"
s = io.open(f, encoding="utf-8").read()
old2 = """        boolean free = damageLens(sp);
        if (!free && !consume(sp, ModItems.FATE_SAND.get(), 10)) {
            sp.sendOverlayMessage(Component.translatable("message.dicefate.need_sand"));
            return InteractionResult.FAIL;
        }
        return appraise(sp, stack, free, level);"""
new2 = """        boolean hasLens = hasLens(sp);
        boolean free = hasLens;
        if (!free && !consume(sp, ModItems.FATE_SAND.get(), 10)) {
            sp.sendOverlayMessage(Component.translatable("message.dicefate.need_sand"));
            return InteractionResult.FAIL;
        }
        InteractionResult r = appraise(sp, stack, free, level);
        // 0.2.0 验收修复 BUG C：镜耐久在鉴定成功后才扣（64 次语义不变）
        if (r == InteractionResult.CONSUME && hasLens) damageLens(sp);
        return r;"""
assert old2 in s, "lens use"
s = s.replace(old2, new2)
old3 = """    /** 背包有鉴定镜则消耗 1 耐久返回 true（免费鉴定 64 次语义）。 */
    private static boolean damageLens(ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(ModItems.APPRAISAL_LENS.get())) {
                s.hurtAndBreak(1, sp, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                return true;
            }
        }
        return false;
    }"""
new3 = """    /** 背包是否有鉴定镜。 */
    private static boolean hasLens(ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            if (sp.getInventory().getItem(i).is(ModItems.APPRAISAL_LENS.get())) return true;
        }
        return false;
    }

    /** 消耗鉴定镜 1 耐久（免费鉴定 64 次语义）。 */
    private static void damageLens(ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(ModItems.APPRAISAL_LENS.get())) {
                s.hurtAndBreak(1, sp, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                return;
            }
        }
    }"""
assert old3 in s, "damageLens"
s = s.replace(old3, new3)
io.open(f, "w", encoding="utf-8", newline="").write(s)
print("BUG C fixed")

# 牌型 Trigger 发射 + recountMeta 死代码删除
f = "src/main/java/com/srqingchen/dicefate/combat/ContestService.java"
s = io.open(f, encoding="utf-8").read()
old4 = """            // —— 8. 牌型 / 协同 / 元素反应 ——
            PatternType pattern = PatternService.detect(atkRoll.baseRolls);"""
new4 = """            // —— 8. 牌型 / 协同 / 元素反应 ——
            PatternType pattern = PatternService.detect(atkRoll.baseRolls);
            if (pattern != PatternType.NONE) {
                // 0.2.0：发射牌型 Trigger（HUD 触发词与 PATTERN 音效——复验死路径修复）
                triggers.add((byte) (ContestPayload.Trigger.PATTERN_STRAIGHT.ordinal() + pattern.ordinal() - 1));
            }"""
assert old4 in s, "pattern"
s = s.replace(old4, new4)
old5 = """    /** 回响重掷后重算元素/面数/材质计数。 */
    private static void recountMeta(RollResult roll) {
        // SlotView 信息在重掷结果里不可得；用 DieRollView 的面数/元素近似重建计数
        for (DieRollView v : roll.baseRolls) {
            if (v.element.isReal()) roll.elementCounts.merge(v.element, 1, Integer::sum);
            roll.faceCounts.merge(v.face, 1, Integer::sum);
        }
    }

"""
assert old5 in s, "recountMeta"
s = s.replace(old5, "")
io.open(f, "w", encoding="utf-8", newline="").write(s)
print("pattern trigger + dead code ok")

# HUD 触发范围 1..4
f = "src/main/java/com/srqingchen/dicefate/client/hud/ContestTrackHud.java"
s = io.open(f, encoding="utf-8").read()
old6 = """        for (byte t : payload.triggers()) {
            if (t <= 3) {   // PATTERN_STRAIGHT..PATTERN_DTRIPLE"""
new6 = """        for (byte t : payload.triggers()) {
            if (t >= 1 && t <= 4) {   // PATTERN_STRAIGHT..PATTERN_DTRIPLE"""
assert old6 in s, "hud range"
s = s.replace(old6, new6)
io.open(f, "w", encoding="utf-8", newline="").write(s)
print("hud range ok")
