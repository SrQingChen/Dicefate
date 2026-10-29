# -*- coding: utf-8 -*-
"""审核问题修复批 1：C1 codec 范围、C2 PENDING 快照、R1 gzip NBT、F2 档案路径、R2 equipment 资产。"""
import gzip, json, os, shutil

# ---- C1: DropSpec 范围 0..1024 ----
p = 'src/main/java/com/srqingchen/dicefate/data/PoolProfile.java'
s = open(p, encoding='utf-8').read()
s = s.replace('Codec.intRange(0, 64).optionalFieldOf("min", 1)', 'Codec.intRange(0, 1024).optionalFieldOf("min", 1)')
s = s.replace('Codec.intRange(0, 64).optionalFieldOf("max", 1)', 'Codec.intRange(0, 1024).optionalFieldOf("max", 1)')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('C1 fixed')

# ---- C2: PENDING 快照迭代 ----
p = 'src/main/java/com/srqingchen/dicefate/combat/ContestService.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''    public static void onDamagePost(Mob mob, float dealt) {
        if (PENDING.isEmpty()) return;
        for (int i = PENDING.size() - 1; i >= 0; i--) {
            PendingHit hit = PENDING.get(i);
            if (hit.target() == mob) {
                PENDING.remove(i);''',
'''    public static void onDamagePost(Mob mob, float dealt) {
        if (PENDING.isEmpty()) return;
        for (PendingHit hit : List.copyOf(PENDING)) {
            if (hit.target() == mob && PENDING.remove(hit)) {''')
# L2: PendingHit 加 tick 过期 + 清理方法
s = s.replace('''    public record PendingHit(Mob target, ServerPlayer attacker, int poiseDamage, int playerTotal,
                             Outcome outcome, PatternType pattern, boolean cashoutUsed, float cashoutScale) {}''',
'''    public record PendingHit(Mob target, ServerPlayer attacker, int poiseDamage, int playerTotal,
                             Outcome outcome, PatternType pattern, boolean cashoutUsed, float cashoutScale,
                             long createdTick) {}

    /** L2：过期清理（伤害被取消/实体卸载时防滞留）。 */
    public static void purgePending(long tick) {
        PENDING.removeIf(h -> tick - h.createdTick() > 40);
    }''')
s = s.replace('PENDING.add(new PendingHit(mob, attacker, poiseDamage, atkTotal, outcome, pattern,\n                    cashoutUsed, cashoutScale));',
              'PENDING.add(new PendingHit(mob, attacker, poiseDamage, atkTotal, outcome, pattern,\n                    cashoutUsed, cashoutScale, tick));')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('C2/L2 fixed')

# L2: tick 清理接入
p = 'src/main/java/com/srqingchen/dicefate/event/CombatEvents.java'
s = open(p, encoding='utf-8').read()
s = s.replace('''        if (tick % 10 != 0) return;''',
'''        if (tick % 10 != 0) return;
        ContestService.purgePending(tick);''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('L2 tick purge wired')

# ---- R1: gzip 6 个 NBT ----
sd = 'src/main/resources/data/dicefate/structure'
for f in os.listdir(sd):
    if not f.endswith('.nbt'):
        continue
    path = os.path.join(sd, f)
    raw = open(path, 'rb').read()
    if raw[:2] == b'\\x1f\\x8b':
        continue
    open(path, 'wb').write(gzip.compress(raw, 6))
    print('gzipped', f)

# ---- F2: 18 个原版档案移入 minecraft/ 子目录 ----
src_dir = 'src/main/resources/data/dicefate/pool_profile'
dst_dir = os.path.join(src_dir, 'minecraft')
os.makedirs(dst_dir, exist_ok=True)
count = 0
for f in list(os.listdir(src_dir)):
    if f.startswith('minecraft_') and f.endswith('.json'):
        path = os.path.join(src_dir, f)
        data = json.load(open(path, encoding='utf-8'))
        entity = data.get('entity', '')
        if not entity.startswith('minecraft:'):
            continue
        # 新 id = 实体全名（minecraft:zombie）
        data['id'] = entity
        new_name = entity.split(':', 1)[1] + '.json'
        with open(os.path.join(dst_dir, new_name), 'w', encoding='utf-8', newline='\n') as out:
            json.dump(data, out, ensure_ascii=False, indent=2)
        os.remove(path)
        count += 1
print('F2 moved', count, 'profiles to minecraft/')

# ---- R2: equipment 资产 + 贴图路径 ----
RES = 'src/main/resources/assets/dicefate'
for asset in ['dicecaster', 'gambler']:
    os.makedirs(f'{RES}/equipment', exist_ok=True)
    json.dump({"layers": {"humanoid": f"dicefate:{asset}",
                          "humanoid_leggings": f"dicefate:{asset}"}},
              open(f'{RES}/equipment/{asset}.json', 'w', encoding='utf-8', newline='\n'), indent=1)
    for layer in ['humanoid', 'humanoid_leggings']:
        d = f'{RES}/textures/entity/equipment/{layer}'
        os.makedirs(d, exist_ok=True)
        shutil.copy(f'{RES}/textures/equipment/humanoid/{asset}.png', f'{d}/{asset}.png')
print('R2 equipment assets done')
