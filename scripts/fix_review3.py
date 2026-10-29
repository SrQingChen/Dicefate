# -*- coding: utf-8 -*-
"""终审遗留修复：R2 equipment JSON、F4 破势骰、深拷贝注记、旧残留清理。"""
import json, os, shutil

RES = 'src/main/resources/assets/dicefate'

# ---- R2: equipment JSON 正确格式（Layer 对象数组 + humanoid_baby） ----
for asset in ['dicecaster', 'gambler']:
    info = {"layers": {
        "humanoid": [{"texture": f"dicefate:{asset}"}],
        "humanoid_leggings": [{"texture": f"dicefate:{asset}"}],
        "humanoid_baby": [{"texture": f"dicefate:{asset}"}]}}
    with open(f'{RES}/equipment/{asset}.json', 'w', encoding='utf-8', newline='\n') as f:
        json.dump(info, f, ensure_ascii=False, indent=1)
# 旧残留清理
if os.path.isdir(f'{RES}/textures/equipment'):
    shutil.rmtree(f'{RES}/textures/equipment')
print('R2 equipment JSON fixed')

# ---- F4a: SkillFamily BREAKER 三参构造 ----
p = 'src/main/java/com/srqingchen/dicefate/dice/SkillFamily.java'
s = open(p, encoding='utf-8').read()
s = s.replace('BREAKER("breaker", new int[]{40 * 20, 36 * 20, 32 * 20}),',
              'BREAKER("breaker", new int[]{40 * 20, 36 * 20, 32 * 20}, 10, 15, 20),')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('F4a BREAKER params fixed')

# ---- F4b: PoiseService 触发条件 —— 需先取 bonus 判断"补足破势" ----
p = 'src/main/java/com/srqingchen/dicefate/combat/PoiseService.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''        // F4：破势骰回调（由 ContestService 安装；破势预判命中时追加班势）
        int before = data.poise();
        int effective = Math.min(amount, cap);
        if (attacker != null && before - effective <= 0 && data.state() == MobFateData.State.NORMAL && BREAKER_HOOK != null) {
            amount += BREAKER_HOOK.apply(attacker, target);
        }
        int after = Math.max(0, before - amount);
        data.setPoise(after);''',
'''        // F4：破势骰回调（由 ContestService 安装）——"不加成就破不了、加上恰好破"时触发
        int before = data.poise();
        int effective = Math.min(amount, cap);
        if (attacker != null && data.state() == MobFateData.State.NORMAL && BREAKER_HOOK != null
            && before - effective > 0) {
            Integer bonus = BREAKER_HOOK.apply(attacker, target);
            if (bonus != null && bonus > 0 && before - effective - bonus <= 0) {
                amount += bonus;
            }
        }
        int after = Math.max(0, before - amount);
        data.setPoise(after);''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('F4b trigger condition fixed')

# ---- F4c: 钩子签名 BiFunction→返回 Integer（保持 null 安全）——lambda 已兼容 ----

# ---- 次要注记：共享 RollResult 深拷贝（非首目标复制的骰列与 Boss 改写隔离） ----
p = 'src/main/java/com/srqingchen/dicefate/combat/ContestService.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''            } else {
                atkRoll = cached.roll();
            }''',
'''            } else {
                atkRoll = copyRoll(cached.roll());
            }''')
s = s.replace('''    /** F3：骰包增伤（命运包等高阶包 +10%）。 */''',
'''    /** 复制共享掷骰结果（与 Boss 改写/回响重掷隔离）。 */
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
        out.mulRatios.addAll(src.mulRatios);
        return out;
    }

    /** F3：骰包增伤（命运包等高阶包 +10%/20%）。 */''')
# F4: breakerBonus 改为返回 Integer 兼容
s = s.replace('    private static int breakerBonus(ServerPlayer player, Mob mob) {',
              '    private static Integer breakerBonus(ServerPlayer player, Mob mob) {')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('notes fixed')
