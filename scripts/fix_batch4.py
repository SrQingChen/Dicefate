# -*- coding: utf-8 -*-
"""26.1 API 修复第 4 批：消息/声音/Key/注册形式。"""
import glob, re

for p in glob.glob('src/main/java/com/srqingchen/dicefate/**/*.java', recursive=True):
    s = open(p, encoding='utf-8').read()
    o = s
    # 消息 API
    s = re.sub(r'\.displayClientMessage\((.+?), true\)', r'.sendOverlayMessage(\1)', s)
    s = re.sub(r'\.displayClientMessage\((.+?), false\)', r'.sendSystemMessage(\1)', s)
    # 客户端判断
    s = re.sub(r'level\.isClientSide(?!\()', 'level.isClientSide()', s)
    s = re.sub(r'this\.level\(\)\.isClientSide(?!\()', 'this.level().isClientSide()', s)
    # ResourceKey.location → identifier
    s = s.replace('.location()', '.identifier()')
    if s != o:
        open(p, 'w', encoding='utf-8', newline='\n').write(s)
        print('msg/client/key:', p)

# 声音：Holder<SoundEvent>.value()
for p in glob.glob('src/main/java/com/srqingchen/dicefate/**/*.java', recursive=True):
    s = open(p, encoding='utf-8').read()
    o = s
    for snd in ['PLAYER_LEVELUP', 'BUNDLE_INSERT', 'CROSSBOW_SHOOT', 'ITEM_BREAK',
                'ARMOR_EQUIP_LEATHER', 'EXPERIENCE_ORB_PICKUP']:
        s = s.replace(f'SoundEvents.{snd},', f'SoundEvents.{snd}.value(),')
    if s != o:
        open(p, 'w', encoding='utf-8', newline='\n').write(s)
        print('sounds:', p)

# PoiseService identifier 修正回退（ResourceKey.identifier 正确；上面全局替换已处理）

# DiceItems cast 简化
p = 'src/main/java/com/srqingchen/dicefate/registry/DiceItems.java'
s = open(p, encoding='utf-8').read()
s = s.replace('return ITEMS.getEntries().stream().map(h -> (DiceItem) (Item) h.value());',
              'return ITEMS.getEntries().stream().map(h -> (DiceItem) h.value());')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('DiceItems cast ok')

# GearItems: 自定义类注册改 Supplier 形式
p = 'src/main/java/com/srqingchen/dicefate/registry/GearItems.java'
s = open(p, encoding='utf-8').read()
s = s.replace('ITEMS.register("dice_sling_bone", p -> new DiceSlingItem(p.durability(180).rarity(Rarity.UNCOMMON), 3.0F));',
              'ITEMS.register("dice_sling_bone", () -> new DiceSlingItem(new Item.Properties().durability(180).rarity(Rarity.UNCOMMON), 3.0F));')
s = s.replace('ITEMS.register("dice_sling_mithril", p -> new DiceSlingItem(p.durability(600).rarity(Rarity.RARE), 5.0F));',
              'ITEMS.register("dice_sling_mithril", () -> new DiceSlingItem(new Item.Properties().durability(600).rarity(Rarity.RARE), 5.0F));')
s = s.replace('ITEMS.register("fate_arrow", FateArrowItem::new, p -> p.stacksTo(64).rarity(Rarity.UNCOMMON));',
              'ITEMS.register("fate_arrow", () -> new FateArrowItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));')
s = s.replace('ITEMS.register("fate_shield_bone", ShieldItem::new, p -> p.durability(120).rarity(Rarity.UNCOMMON));',
              'ITEMS.register("fate_shield_bone", () -> new ShieldItem(new Item.Properties().durability(120).rarity(Rarity.UNCOMMON)));')
s = s.replace('ITEMS.register("fate_shield_mithril", ShieldItem::new, p -> p.durability(240).rarity(Rarity.RARE));',
              'ITEMS.register("fate_shield_mithril", () -> new ShieldItem(new Item.Properties().durability(240).rarity(Rarity.RARE)));')
s = s.replace('ITEMS.register("fate_shield_fategold", ShieldItem::new, p -> p.durability(400).rarity(Rarity.EPIC));',
              'ITEMS.register("fate_shield_fategold", () -> new ShieldItem(new Item.Properties().durability(400).rarity(Rarity.EPIC)));')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('GearItems supplier ok')

# ModItems: 3参 register 同样修（之前生成的 p-> new X(props) 形式实际可匹配 UnaryOperator? 不行——统一改 Supplier）
p = 'src/main/java/com/srqingchen/dicefate/registry/ModItems.java'
s = open(p, encoding='utf-8').read()
s = re.sub(r'ITEMS\.register\("([a-z0-9_]+)", p -> new (\w+)\((p\.[^;]+?)\)\);',
           lambda m: f'ITEMS.register("{m.group(1)}", () -> new {m.group(2)}(new Item.Properties()' + m.group(3)[len("p."):] + '));', s)
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('ModItems supplier ok')

# SlotExpandItem supplier 形式（已经是 p -> new SlotExpandItem(...) 3参？它是 2 参直接 register(path, func<Properties>) 不存在——修）
p = 'src/main/java/com/srqingchen/dicefate/registry/ModItems.java'
s = open(p, encoding='utf-8').read()
s = re.sub(r'ITEMS\.register\("(slot_expand_\d)", p -> new SlotExpandItem\(p\.stacksTo\(1\)\.rarity\(Rarity\.(\w+)\), (\d)\)\);',
           lambda m: f'ITEMS.register("{m.group(1)}", () -> new SlotExpandItem(new Item.Properties().stacksTo(1).rarity(Rarity.{m.group(2)}), {m.group(3)}));', s)
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('SlotExpand supplier ok')
