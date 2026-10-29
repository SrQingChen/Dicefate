# -*- coding: utf-8 -*-
"""修复第 5 批：声音 Holder/实体 build/导入。"""
import glob

# 1) 纯 SoundEvent 常量去掉 .value()；Holder 常量保留
PLAIN = ['CROSSBOW_SHOOT', 'PLAYER_LEVELUP', 'BUNDLE_INSERT']
for p in glob.glob('src/main/java/com/srqingchen/dicefate/**/*.java', recursive=True):
    s = open(p, encoding='utf-8').read()
    o = s
    for snd in PLAIN:
        s = s.replace(f'SoundEvents.{snd}.value()', f'SoundEvents.{snd}')
    if s != o:
        open(p, 'w', encoding='utf-8', newline='\n').write(s)
        print('plain sound:', p)

# GearItems ArmorMaterial 需要 Holder，还原
p = 'src/main/java/com/srqingchen/dicefate/registry/GearItems.java'
s = open(p, encoding='utf-8').read()
s = s.replace('SoundEvents.ARMOR_EQUIP_LEATHER.value()', 'SoundEvents.ARMOR_EQUIP_LEATHER')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('GearItems armor sound ok')

# 2) EntityType.build(ResourceKey)
p = 'src/main/java/com/srqingchen/dicefate/registry/ModEntities.java'
s = open(p, encoding='utf-8').read()
import re
s = s.replace('import net.minecraft.world.entity.EntityType;',
              'import net.minecraft.core.registries.Registries;\nimport net.minecraft.resources.ResourceKey;\nimport net.minecraft.world.entity.EntityType;')
s = s.replace('.build("two_faced_gambler"));', '.build(key("two_faced_gambler")));')
s = s.replace('.build("molten_die_king"));', '.build(key("molten_die_king")));')
s = s.replace('.build("frost_silence_queen"));', '.build(key("frost_silence_queen")));')
s = s.replace('.build("nullface"));', '.build(key("nullface")));')
s = s.replace('.build("die_projectile"));', '.build(key("die_projectile")));')
s = s.replace('.build("fate_arrow"));', '.build(key("fate_arrow")));')
s = s.replace('''    private ModEntities() {}''',
'''    private static ResourceKey<EntityType<?>> key(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, DiceFate.id(path));
    }

    private ModEntities() {}''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('ModEntities build ok')

# 3) ServerActions import DiceItem
p = 'src/main/java/com/srqingchen/dicefate/network/ServerActions.java'
s = open(p, encoding='utf-8').read()
if 'import com.srqingchen.dicefate.item.DiceItem;' not in s:
    s = s.replace('import com.srqingchen.dicefate.dice.DiceItem;\n', '')
    s = s.replace('import com.srqingchen.dicefate.dice.DiceLookup;',
                  'import com.srqingchen.dicefate.dice.DiceLookup;\nimport com.srqingchen.dicefate.item.DiceItem;')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('ServerActions import ok')

# 4) DiceSlingItem 早退路径补返回值
p = 'src/main/java/com/srqingchen/dicefate/item/DiceSlingItem.java'
s = open(p, encoding='utf-8').read()
if 'if (level.isClientSide()) return;' in s:
    s = s.replace('if (level.isClientSide()) return;', 'if (level.isClientSide()) return false;')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('DiceSling return ok')
