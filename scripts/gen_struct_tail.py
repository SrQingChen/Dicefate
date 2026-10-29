# -*- coding: utf-8 -*-
"""结构生成器尾部：arena + NBT 写出 + worldgen + 战利品（追加到 gen_structures.py）。"""

TAIL = '''

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
'''

s = open('scripts/gen_structures.py', encoding='utf-8').read()
open('scripts/gen_structures.py', 'w', encoding='utf-8', newline='\n').write(s + TAIL)
print('tail appended')
