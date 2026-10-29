# -*- coding: utf-8 -*-
"""修复 26.1 Item.Properties 必须 setId：把 Supplier 注册路径改为 id 注入路径。"""
import re, glob

changed = []

# ---- 1. 所有 ITEMS.register("name", () -> new X(new Item.Properties()...)) ----
pat = re.compile(r'ITEMS\.register\("([a-z0-9_]+)", \(\) -> new (\w+)\(new Item\.Properties\(\)')
for path in glob.glob('src/main/java/com/srqingchen/dicefate/registry/*.java'):
    s = open(path, encoding='utf-8').read()
    o = s
    s = pat.sub(r'ITEMS.register("\1", id -> new \2(IdProps.item(id)', s)
    if s != o:
        # 补 import
        if 'import com.srqingchen.dicefate.registry.IdProps;' not in s and 'class IdProps' not in s:
            s = s.replace('import com.srqingchen.dicefate.DiceFate;',
                          'import com.srqingchen.dicefate.DiceFate;', 1)
        open(path, 'w', encoding='utf-8', newline='\n').write(s)
        changed.append(path)

# ---- 2. DiceItems 的 register helper ----
p = 'src/main/java/com/srqingchen/dicefate/registry/DiceItems.java'
s = open(p, encoding='utf-8').read()
s = s.replace(
    'return ITEMS.register(path, id -> new DiceItem(id, new net.minecraft.world.item.Item.Properties().stacksTo(1), spec));',
    'return ITEMS.register(path, id -> new DiceItem(id, IdProps.item(id).stacksTo(1), spec));')
open(p, 'w', encoding='utf-8', newline='\n').write(s)

# ---- 3. ModBlocks.BLOCK_ITEMS ----
p = 'src/main/java/com/srqingchen/dicefate/block/FateBlocks.java'
s = open(p, encoding='utf-8').read()
s = s.replace(
    '''        com.srqingchen.dicefate.registry.ModBlocks.BLOCK_ITEMS.register(name,
            () -> new net.minecraft.world.item.BlockItem(block.get(),
                new net.minecraft.world.item.Item.Properties().stacksTo(64)));''',
    '''        com.srqingchen.dicefate.registry.ModBlocks.BLOCK_ITEMS.register(name,
            id -> new net.minecraft.world.item.BlockItem(block.get(),
                com.srqingchen.dicefate.registry.IdProps.item(id).stacksTo(64)));''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)

print('converted:', len(changed), 'files')
for c in changed:
    print('  ', c)

# ---- 4. 残留检查 ----
left = []
for path in glob.glob('src/main/java/com/srqingchen/dicefate/**/*.java', recursive=True):
    s = open(path, encoding='utf-8').read()
    for m in re.finditer(r'\.register\("[a-z0-9_]+", \(\) ->', s):
        left.append((path, m.group(0)))
print('remaining Supplier-path registers:', len(left))
for l in left[:10]:
    print('  ', l)
