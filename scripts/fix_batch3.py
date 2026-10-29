# -*- coding: utf-8 -*-
"""26.1 API 修复第 3 批。"""
import glob

for p in glob.glob('src/main/java/com/srqingchen/dicefate/**/*.java', recursive=True):
    s = open(p, encoding='utf-8').read()
    o = s
    s = s.replace('net.minecraft.core.component.TooltipDisplay', 'net.minecraft.world.item.component.TooltipDisplay')
    s = s.replace('import net.minecraft.core.component.TooltipDisplay;', 'import net.minecraft.world.item.component.TooltipDisplay;')
    if s != o:
        open(p, 'w', encoding='utf-8', newline='\n').write(s)
        print('TooltipDisplay:', p)

# PoiseService: registry.get(id)
p = 'src/main/java/com/srqingchen/dicefate/combat/PoiseService.java'
s = open(p, encoding='utf-8').read()
s = s.replace('Holder<DamageType> type = registry.getHolderOrThrow(FATEBREAK_KEY);',
              'Holder<DamageType> type = registry.get(FATEBREAK_KEY.location()).orElseThrow();')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('PoiseService holder ok')

# BossEntity: .map(ref -> ref.value())
p = 'src/main/java/com/srqingchen/dicefate/entity/boss/BossEntity.java'
s = open(p, encoding='utf-8').read()
s = s.replace('.map(java.util.function.Supplier::get)', '.map(ref -> ref.value())')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('BossEntity flatMap ok')

# DiceItems.spec cast
p = 'src/main/java/com/srqingchen/dicefate/registry/DiceItems.java'
s = open(p, encoding='utf-8').read()
s = s.replace('return entry.map(h -> h.value().spec).orElse(null);',
              'return entry.map(h -> ((DiceItem) h.value()).spec).orElse(null);')
s = s.replace('return ITEMS.getEntries().stream().map(h -> (DiceItem) h.value());',
              'return ITEMS.getEntries().stream().map(h -> (DiceItem) (Item) h.value());')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('DiceItems cast ok')

# DiceSlingItem: releaseUsing boolean
p = 'src/main/java/com/srqingchen/dicefate/item/DiceSlingItem.java'
s = open(p, encoding='utf-8').read()
s = s.replace('public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {',
              'public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {')
s = s.replace('''        int charge = this.getUseDuration(stack, entity) - timeLeft;
        float power = Math.min(1.0F, charge / 20.0F);
        if (power < 0.25F) return;   // 未蓄满不发

        float damage = maxDamage * power;
        DieProjectile proj = new DieProjectile(level, entity, damage);
        proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0.0F, power * 2.2F, 0.8F);
        level.addFreshEntity(proj);
        level.playSound(null, entity.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.7F, 1.3F);
        stack.hurtAndBreak(1, entity, getBrokenSlot(entity));
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            // 无弹药消耗（骰弹来自命数本身），耐久承担成本
        }
    }''',
'''        int charge = this.getUseDuration(stack, entity) - timeLeft;
        float power = Math.min(1.0F, charge / 20.0F);
        if (power < 0.25F) return false;   // 未蓄满不发

        float damage = maxDamage * power;
        DieProjectile proj = new DieProjectile(level, entity, damage);
        proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0.0F, power * 2.2F, 0.8F);
        level.addFreshEntity(proj);
        level.playSound(null, entity.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.7F, 1.3F);
        stack.hurtAndBreak(1, entity, getBrokenSlot(entity));
        return true;
    }''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('DiceSlingItem ok')
