# -*- coding: utf-8 -*-
"""生成 26.1 物品模型定义 + 方块 blockstate/model JSON。"""
import json, os

RES = 'src/main/resources'
def w(path, obj):
    full = os.path.join(RES, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)

# ============ 物品模型 ============
# 26.1: assets/<ns>/items/<id>.json = {"model": {"type": "minecraft:model", "model": "<ns>:item/<id>"}}
# 模型: assets/<ns>/models/item/<id>.json = {"parent": "minecraft:item/generated", "textures": {"layer0": "<ns>:item/<id>"}}

dice_ids = []
mats_faces = [("bone",4),("bone",6),("bone",8),("iron",6),("iron",8),("iron",10),
    ("mithril",8),("mithril",10),("mithril",12),("fategold",10),("fategold",12),("fategold",20),
    ("starlight",12),("starlight",20),("starlight",24)]
for m, f in mats_faces:
    dice_ids.append(f"die_{m}_d{f}")
for e in ["ember","frost","storm","venom","radiant","umbral"]:
    for suf in ["spark","resonant","origin"]:
        dice_ids.append(f"die_{e}_{suf}")
for fam in ["echo","pursuit","ward","breaker","reverse","greed","lock","favor"]:
    for t in range(1,4):
        dice_ids.append(f"skill_die_{fam}_{t}")
for k in ["edge","wall","poise","element","combo","mend"]:
    dice_ids.append(f"add_die_{k}")
for k in ["fierce","bulwark","pierce","break","resonance","fate"]:
    dice_ids.append(f"mul_die_{k}")
for u in ["cracked_face","two_faced","molten_core","frost_silence","storm_verdict",
          "bone_eater","radiant_oath","umbral_veil","null_face","fate_sovereign"]:
    dice_ids.append(f"unique_die_{u}")
for c in ["greed","blood","fracture","misfortune"]:
    dice_ids.append(f"cursed_die_{c}")

misc_ids = ["blank_die_bone","blank_die_iron","blank_die_mithril","blank_die_fategold","blank_die_starlight",
    "fate_sand","fate_dust","fate_core","starshard","cracked_core","fate_fragment","sovereign_core",
    "element_essence_ember","element_essence_frost","element_essence_storm","element_essence_venom",
    "element_essence_radiant","element_essence_umbral",
    "rune_ink","repair_file","fate_glue","perfect_oil","spirit_feed","appraisal_lens","appraisal_scroll",
    "mirror_shard","slot_shard","slot_expand_1","slot_expand_2","slot_expand_3","slot_expand_4","slot_expand_5",
    "sigil_addition","sigil_multiplication","sigil_echo","sigil_echo_2","respec_stone","fate_lock_talisman",
    "incense","offering_bone","offering_star","offering_core","merchant_token","sovereign_seal",
    "pack_common","pack_fine","pack_rare","pack_epic","pack_fate_minor","pack_fate_major",
    "pack_ember","pack_frost","pack_storm","pack_venom","pack_radiant","pack_umbral",
    "pack_straight","pack_triple","pack_two_faced","pack_null",
    "potion_fate_aim","potion_fate_guard","potion_fate_sight","lucky_cookie","unidentified_die",
    "dice_blade_bone","dice_blade_iron","dice_blade_mithril","dice_blade_fategold",
    "dice_sling_bone","dice_sling_mithril","fate_arrow",
    "fate_shield_bone","fate_shield_mithril","fate_shield_fategold",
    "dicecaster_hood","dicecaster_robe","dicecaster_leggings","dicecaster_boots",
    "gambler_hood","gambler_jacket","gambler_pants","gambler_boots",
    "dice_pouch","lucky_pendant","fate_mirror","spirit_bell","gambler_charm","fate_lock_ring","element_resonator"]
plate_ids = [f"face_plate_d{f}" for f in [4,6,8,10,12,20,24]]

all_items = dice_ids + misc_ids + plate_ids
for iid in all_items:
    # 物品模型定义
    w(f'assets/dicefate/items/{iid}.json',
      {"model": {"type": "minecraft:model", "model": f"dicefate:item/{iid}"}})
    # 模型（手写式 generated；骰子后续替换为 3D 网格模型）
    w(f'assets/dicefate/models/item/{iid}.json',
      {"parent": "minecraft:item/generated", "textures": {"layer0": f"dicefate:item/{iid}"}})

# 方块物品（cube 模型）
block_ids = ["fate_altar","die_engraver","appraisal_table","repair_bench",
             "fate_loom","offering_shrine","merchant_stall","null_portal"]
for bid in block_ids:
    w(f'assets/dicefate/items/{bid}.json',
      {"model": {"type": "minecraft:model", "model": f"dicefate:block/{bid}"}})

# ============ 方块 blockstate + 模型 ============
for bid in block_ids:
    w(f'assets/dicefate/blockstates/{bid}.json', {"variants": {"": {"model": f"dicefate:block/{bid}"}}})
    # 简单立方模型：六面同贴图
    w(f'assets/dicefate/models/block/{bid}.json', {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"dicefate:block/{bid}"}})

print('models done:', len(all_items), 'items,', len(block_ids), 'blocks')
