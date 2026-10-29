# -*- coding: utf-8 -*-
"""生成 6 个结构：NBT 模板 + worldgen JSON + 结构箱子战利品。"""
import json, os, struct

RES = 'src/main/resources'
DATA_VERSION = 4790

def wjson(path, obj):
    full = os.path.join(RES, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)

# ================= Python NBT 写入器（未压缩 big-endian） =================
TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE, TAG_BYTE_ARRAY = 0,1,2,3,4,5,6,7
TAG_STRING, TAG_LIST, TAG_COMPOUND = 8,9,10

class NbtWriter:
    def __init__(self):
        self.buf = bytearray()
    def b(self, x): self.buf += struct.pack('>b', x)
    def s(self, x): self.buf += struct.pack('>h', x)
    def i(self, x): self.buf += struct.pack('>i', x)
    def f(self, x): self.buf += struct.pack('>f', x)
    def d(self, x): self.buf += struct.pack('>d', x)
    def str_utf(self, x):
        e = x.encode('utf-8')
        self.buf += struct.pack('>H', len(e)) + e

def write_tag(wr, t, name, value):
    wr.b(t); wr.str_utf(name)
    write_payload(wr, t, value)

def write_payload(wr, t, value):
    if t == TAG_BYTE: wr.b(value)
    elif t == TAG_SHORT: wr.s(value)
    elif t == TAG_INT: wr.i(value)
    elif t == TAG_DOUBLE: wr.d(value)
    elif t == TAG_FLOAT: wr.f(value)
    elif t == TAG_STRING: wr.str_utf(value)
    elif t == TAG_LIST:
        if not value:
            wr.b(TAG_END); wr.i(0); return
        first_t = value[0][0]
        wr.b(first_t); wr.i(len(value))
        for tt, vv in value:
            write_payload(wr, tt, vv)
    elif t == TAG_COMPOUND:
        for name, (tt, vv) in value.items():
            write_tag(wr, tt, name, vv)
        wr.b(TAG_END)
    else:
        raise ValueError(f'unsupported tag {t}')

def save_nbt(path, root_compound):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    wr = NbtWriter()
    wr.b(TAG_COMPOUND); wr.str_utf('')
    write_payload(wr, TAG_COMPOUND, root_compound)
    with open(path, 'wb') as f:
        f.write(bytes(wr.buf))

def ints(lst):  # TAG_Int list payload：逐元素元组
    return [(TAG_INT, v) for v in lst]

def structure_nbt(size, palette, blocks, entities=None):
    root = {
        'size': (TAG_LIST, ints(list(size))),
        'palette': (TAG_LIST, [
            (TAG_COMPOUND, {
                'Name': (TAG_STRING, name),
                **({'Properties': (TAG_COMPOUND, {k: (TAG_STRING, v) for k, v in props.items()})}
                   if props else {})
            }) for name, props in palette
        ]),
        'blocks': (TAG_LIST, [
            (TAG_COMPOUND, dict([
                ('pos', (TAG_LIST, ints(list(pos)))),
                ('state', (TAG_INT, state)),
                *([('nbt', (TAG_COMPOUND, nbt))] if nbt else [])
            ])) for pos, state, nbt in blocks
        ]),
        'entities': (TAG_LIST, [
            (TAG_COMPOUND, {
                'pos': (TAG_LIST, [(TAG_DOUBLE, p) for p in epos]),
                'blockPos': (TAG_LIST, ints(list(bpos))),
                'nbt': (TAG_COMPOUND, nbt),
            }) for epos, bpos, nbt in (entities or [])
        ]),
        'DataVersion': (TAG_INT, DATA_VERSION),
    }
    return root

def chest_nbt(loot_table, seed=None):
    return {'id': (TAG_STRING, 'minecraft:chest'),
            'LootTable': (TAG_STRING, loot_table)}

def boss_nbt(entity_id, health):
    return {'id': (TAG_STRING, entity_id),
            'Health': (TAG_FLOAT, health)}

# ================= 结构 1：命盘祭坛（9×6×9 山地/森林小圣地） =================
def shrine_structure():
    air, stone, brick, chisel, amethyst, altar, shrine_block, lantern, quartz = 0, 1, 2, 3, 4, 5, 6, 7, 8
    palette = [
        ('minecraft:air', {}),
        ('minecraft:stone_bricks', {}),
        ('minecraft:chiseled_stone_bricks', {}),
        ('minecraft:polished_andesite', {}),
        ('minecraft:amethyst_block', {}),
        ('dicefate:fate_altar', {}),
        ('dicefate:offering_shrine', {}),
        ('minecraft:lantern', {'hanging': 'false'}),
        ('minecraft:quartz_pillar', {'axis': 'y'}),
    ]
    blocks = []
    S = 9
    for x in range(S):
        for z in range(S):
            # 平台
            blocks.append(((x, 0, z), brick if (x in (0, 8) or z in (0, 8)) else chisel, None))
            # 四角柱
            if (x, z) in [(0, 0), (8, 0), (0, 8), (8, 8)]:
                for y in range(1, 4):
                    blocks.append(((x, y, z), quartz, None))
                blocks.append(((x, 4, z), amethyst, None))
            # 边墙低栏
            elif x in (0, 8) or z in (0, 8):
                blocks.append(((x, 1, z), stone, None))
            else:
                blocks.append(((x, 1, z), air, None))
                blocks.append(((x, 2, z), air, None))
                blocks.append(((x, 3, z), air, None))
                blocks.append(((x, 4, z), air, None))
                blocks.append(((x, 5, z), air, None))
    # 中央：骰运台 + 供奉龛 + 灯
    blocks.append(((4, 1, 4), altar, None))
    blocks.append(((2, 1, 4), shrine_block, None))
    blocks.append(((6, 1, 4), shrine_block, None))
    blocks.append(((4, 1, 2), lantern and 7, None))
    blocks.append(((4, 1, 6), 7, None))
    return structure_nbt((9, 6, 9), palette, blocks)

# ================= 结构 2：倾颓赌坊（13×8×13 平原废墟 + 箱子+骰商摊位） =================
def casino_ruin_structure():
    air, plank, log, cobble, mossy, chest, stall, bar, glass = 0, 1, 2, 3, 4, 5, 6, 7, 8
    palette = [
        ('minecraft:air', {}),
        ('minecraft:oak_planks', {}),
        ('minecraft:oak_log', {'axis': 'y'}),
        ('minecraft:cobblestone', {}),
        ('minecraft:mossy_cobblestone', {}),
        ('minecraft:chest', {'facing': 'north'}),
        ('dicefate:merchant_stall', {}),
        ('minecraft:oak_stairs', {'facing': 'north', 'half': 'bottom'}),
        ('minecraft:white_stained_glass_pane', {}),
    ]
    blocks = []
    S = 13
    for x in range(S):
        for z in range(S):
            edge = x in (0, S - 1) or z in (0, S - 1)
            # 地板
            floor = mossy if (x + z) % 5 == 0 else cobble
            blocks.append(((x, 0, z), floor, None))
            # 墙
            if edge:
                for y in range(1, 5):
                    if x in (0, S - 1) and z in (0, S - 1):
                        blocks.append(((x, y, z), log if y < 4 else plank, None))
                    elif y == 3:
                        blocks.append(((x, y, z), glass, None))
                    elif (x + z + y) % 4 == 0:
                        blocks.append(((x, y, z), air, None))   # 破损
                    else:
                        blocks.append(((x, y, z), plank, None))
                blocks.append(((x, 5, z), air, None))
                blocks.append(((x, 6, z), air, None))
                blocks.append(((x, 7, z), air, None))
            else:
                for y in range(1, 8):
                    blocks.append(((x, y, z), air, None))
    # 门（南面中央破口）
    for y in range(1, 4):
        blocks.append(((6, y, S - 1), air, None))
        blocks.append(((7, y, S - 1), air, None))
    # 内容：骰商摊位 + 双箱 + 赌桌
    blocks.append(((6, 1, 3), stall, None))
    blocks.append(((3, 1, 3), 5, chest_nbt('dicefate:chests/casino_ruin')))
    blocks.append(((9, 1, 3), 5, chest_nbt('dicefate:chests/casino_ruin')))
    for x in range(5, 8):
        blocks.append(((x, 1, 8), bar, None))
    return structure_nbt((13, 8, 13), palette, blocks)

#

# ================= Boss 竞技场（15×10×15 通用） =================
def arena_structure(floor_block, pillar_block, accent_block, boss_id, boss_hp, loot, facing='north'):
    air, floor, pillar, accent, chest = 0, 1, 2, 3, 4
    palette = [
        ('minecraft:air', {}),
        (floor_block, {}),
        (pillar_block, {}),
        (accent_block, {}),
        ('minecraft:chest', {'facing': facing}),
    ]
    blocks = []
    S, H = 15, 10
    for x in range(S):
        for z in range(S):
            blocks.append(((x, 0, z), floor, None))
            edge = x in (0, S - 1) or z in (0, S - 1)
            for y in range(1, H):
                if edge:
                    if (x in (0, S - 1) and z in (0, S - 1)):
                        blocks.append(((x, y, z), pillar if y < H - 2 else accent, None))
                    elif y in (1, 2, H - 1):
                        blocks.append(((x, y, z), pillar, None))
                    else:
                        blocks.append(((x, y, z), air, None))
                else:
                    blocks.append(((x, y, z), air, None))
            blocks.append(((x, H, z), floor if edge else accent, None))
    for pos in [(2, 1, 2), (12, 1, 2), (2, 1, 12), (12, 1, 12)]:
        blocks.append((pos, 3, None))
    blocks.append(((2, 1, 7), 4, chest_nbt(loot)))
    blocks.append(((12, 1, 7), 4, chest_nbt(loot)))
    entities = [((7.5, 1.0, 7.5), (7, 1, 7), boss_nbt(boss_id, boss_hp))]
    return structure_nbt((S, H + 1, S), palette, blocks, entities)

# ================= 写出 NBT =================
save_nbt(f'{RES}/data/dicefate/structure/fate_shrine.nbt', shrine_structure())
save_nbt(f'{RES}/data/dicefate/structure/casino_ruin.nbt', casino_ruin_structure())
save_nbt(f'{RES}/data/dicefate/structure/molten_arena.nbt',
         arena_structure('minecraft:nether_bricks', 'minecraft:polished_blackstone_bricks',
                         'minecraft:magma_block', 'dicefate:molten_die_king', 340.0,
                         'dicefate:chests/molten_arena'))
save_nbt(f'{RES}/data/dicefate/structure/frost_arena.nbt',
         arena_structure('minecraft:packed_ice', 'minecraft:blue_ice',
                         'minecraft:snow_block', 'dicefate:frost_silence_queen', 300.0,
                         'dicefate:chests/frost_arena'))
save_nbt(f'{RES}/data/dicefate/structure/null_casino_arena.nbt',
         arena_structure('minecraft:purpur_block', 'minecraft:end_stone_bricks',
                         'minecraft:obsidian', 'dicefate:nullface', 520.0,
                         'dicefate:chests/null_casino', 'east'))
save_nbt(f'{RES}/data/dicefate/structure/vault_arena.nbt',
         arena_structure('minecraft:deepslate_bricks', 'minecraft:polished_deepslate',
                         'minecraft:copper_block', 'dicefate:two_faced_gambler', 220.0,
                         'dicefate:chests/vault_arena', 'south'))
print('structure NBTs written')

# ================= worldgen JSON =================
def structure_json(name, start_pool, biome_tag, step, spacing, sep, salt):
    wjson(f'data/dicefate/worldgen/structure/{name}.json', {
        "type": "minecraft:jigsaw",
        "biomes": f"#{biome_tag}",
        "step": step,
        "terrain_adaptation": "beard_thin",
        "start_pool": f"dicefate:{start_pool}",
        "size": 1,
        "start_height": {"absolute": 0},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 80,
        "use_expansion_hack": False})
    wjson(f'data/dicefate/worldgen/template_pool/{start_pool}.json', {
        "fallback": "minecraft:empty",
        "elements": [{
            "weight": 1,
            "element": {
                "element_type": "minecraft:single_pool_element",
                "location": f"dicefate:{name}",
                "processors": "minecraft:empty",
                "projection": "rigid"}}]})
    wjson(f'data/dicefate/worldgen/structure_set/{name}.json', {
        "structures": [{"structure": f"dicefate:{name}", "weight": 1}],
        "placement": {
            "type": "minecraft:random_spread",
            "spacing": spacing,
            "separation": sep,
            "salt": salt}})

structure_json('fate_shrine', 'fate_shrine_start', 'dicefate:has_structure/fate_shrine', 'surface_structures', 24, 8, 11001)
structure_json('casino_ruin', 'casino_ruin_start', 'dicefate:has_structure/casino_ruin', 'surface_structures', 28, 10, 11002)
structure_json('vault_arena', 'vault_arena_start', 'dicefate:has_structure/vault_arena', 'underground_structures', 30, 12, 11003)
structure_json('molten_arena', 'molten_arena_start', 'dicefate:has_structure/molten_arena', 'underground_structures', 32, 12, 11004)
structure_json('frost_arena', 'frost_arena_start', 'dicefate:has_structure/frost_arena', 'surface_structures', 32, 12, 11005)
structure_json('null_casino_arena', 'null_casino_arena_start', 'dicefate:has_structure/null_casino_arena', 'surface_structures', 20, 8, 11006)
print('worldgen JSON written')

def biome_tag(name, values):
    wjson(f'data/dicefate/tags/worldgen/biome/has_structure/{name}.json',
          {"replace": False, "values": values})

biome_tag('fate_shrine', ["minecraft:forest", "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:jagged_peaks", "minecraft:stony_peaks", "minecraft:meadow"])
biome_tag('casino_ruin', ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", "minecraft:desert", "minecraft:snowy_plains"])
biome_tag('vault_arena', ["minecraft:forest", "minecraft:plains", "minecraft:birch_forest", "minecraft:dark_forest", "minecraft:taiga"])
biome_tag('molten_arena', ["minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:warped_forest", "minecraft:basalt_deltas"])
biome_tag('frost_arena', ["minecraft:snowy_plains", "minecraft:frozen_peaks", "minecraft:ice_spikes", "minecraft:snowy_taiga"])
biome_tag('null_casino_arena', ["minecraft:end_highlands", "minecraft:end_midlands", "minecraft:small_end_islands"])
print('biome tags written')

def chest_loot(entries):
    pools = []
    for item, mn, mx, w, chance in entries:
        e = {"type": "minecraft:item", "name": item, "weight": w,
             "functions": [{"function": "minecraft:set_count",
                             "count": {"type": "minecraft:uniform", "min": mn, "max": mx}}]}
        if chance < 1.0:
            e["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
        pools.append({"rolls": 1, "bonus_rolls": 0, "entries": [e]})
    return {"type": "minecraft:chest", "pools": pools}

wjson('data/dicefate/loot_table/chests/casino_ruin.json', chest_loot([
    ("dicefate:fate_sand", 20, 60, 1, 1.0),
    ("dicefate:pack_common", 1, 2, 1, 0.6),
    ("dicefate:pack_fine", 1, 1, 1, 0.3),
    ("dicefate:merchant_token", 1, 2, 1, 0.5),
    ("dicefate:unidentified_die", 1, 1, 1, 0.3),
    ("minecraft:gold_ingot", 1, 3, 1, 0.5)]))
wjson('data/dicefate/loot_table/chests/vault_arena.json', chest_loot([
    ("dicefate:fate_sand", 40, 90, 1, 1.0),
    ("dicefate:slot_shard", 2, 4, 1, 0.8),
    ("dicefate:pack_fate_minor", 1, 1, 1, 0.5),
    ("dicefate:unidentified_die", 1, 2, 1, 0.6),
    ("dicefate:cracked_core", 1, 1, 1, 0.4)]))
wjson('data/dicefate/loot_table/chests/molten_arena.json', chest_loot([
    ("dicefate:fate_sand", 60, 120, 1, 1.0),
    ("dicefate:element_essence_ember", 2, 4, 1, 0.8),
    ("dicefate:starshard", 1, 3, 1, 0.6),
    ("dicefate:pack_ember", 1, 1, 1, 0.5),
    ("dicefate:unidentified_die", 1, 2, 1, 0.5)]))
wjson('data/dicefate/loot_table/chests/frost_arena.json', chest_loot([
    ("dicefate:fate_sand", 60, 120, 1, 1.0),
    ("dicefate:element_essence_frost", 2, 4, 1, 0.8),
    ("dicefate:starshard", 1, 3, 1, 0.6),
    ("dicefate:pack_frost", 1, 1, 1, 0.5),
    ("dicefate:blank_die_starlight", 1, 1, 1, 0.2)]))
wjson('data/dicefate/loot_table/chests/null_casino.json', chest_loot([
    ("dicefate:fate_sand", 150, 300, 1, 1.0),
    ("dicefate:starshard", 3, 6, 1, 1.0),
    ("dicefate:pack_null", 1, 1, 1, 0.4),
    ("dicefate:face_plate_d24", 1, 1, 1, 0.15),
    ("dicefate:unidentified_die", 2, 4, 1, 0.8)]))
print('chest loot written')
