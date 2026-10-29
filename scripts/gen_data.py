# -*- coding: utf-8 -*-
"""生成全部数据包 JSON：骰子定义/生物档案/骰包/反应/协同/命纹/伤害类型/标签。"""
import json, os

BASE = 'src/main/resources/data/dicefate'

def w(path, obj):
    full = os.path.join(BASE, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)

# ============ 1. 骰子定义（83） ============
basic = [
 ("die_bone_d4",4,"bone","BASIC","bone","common",None),
 ("die_bone_d6",6,"bone","BASIC","bone","common",None),
 ("die_bone_d8",8,"bone","BASIC","bone","uncommon",None),
 ("die_iron_d6",6,"iron","BASIC","iron","uncommon",None),
 ("die_iron_d8",8,"iron","BASIC","iron","uncommon",None),
 ("die_iron_d10",10,"iron","BASIC","iron","rare",None),
 ("die_mithril_d8",8,"mithril","BASIC","mithril","rare",None),
 ("die_mithril_d10",10,"mithril","BASIC","mithril","rare",None),
 ("die_mithril_d12",12,"mithril","BASIC","mithril","epic",None),
 ("die_fategold_d10",10,"fategold","BASIC","fategold","epic",None),
 ("die_fategold_d12",12,"fategold","BASIC","fategold","epic",None),
 ("die_fategold_d20",20,"fategold","BASIC","fategold","legendary",None),
 ("die_starlight_d12",12,"starlight","BASIC","starlight","legendary",None),
 ("die_starlight_d20",20,"starlight","BASIC","starlight","legendary",None),
 ("die_starlight_d24",24,"starlight","BASIC","starlight","fated",None),
]
for did, face, mat, kind, material, rarity, elem in basic:
    o = {"id": f"dicefate:{did}", "face": face, "kind": kind.lower(), "material": material, "rarity": rarity}
    w(f'die/{did}.json', o)

for e in ["ember","frost","storm","venom","radiant","umbral"]:
    for suffix, face, rar in [("spark",8,"uncommon"),("resonant",12,"epic"),("origin",20,"legendary")]:
        w(f'die/die_{e}_{suffix}.json', {
            "id": f"dicefate:die_{e}_{suffix}", "face": face, "kind": "element",
            "material": "elemental", "rarity": rar, "element": e})

skills = ["echo","pursuit","ward","breaker","reverse","greed","lock","favor"]
for fam in skills:
    for tier in range(3):
        face = [6,12,20][tier]
        rar = ["uncommon","epic","legendary"][tier]
        w(f'die/skill_die_{fam}_{tier+1}.json', {
            "id": f"dicefate:skill_die_{fam}_{tier+1}", "face": face, "kind": "skill",
            "material": "skill", "rarity": rar,
            "skill": f"dicefate:skill/{fam}", "skill_tier": tier})

for name, face, rar in [("edge",6,"rare"),("wall",6,"rare"),("poise",8,"epic"),
                          ("element",8,"epic"),("combo",6,"rare"),("mend",8,"epic")]:
    w(f'die/add_die_{name}.json', {"id": f"dicefate:add_die_{name}", "face": face,
        "kind": "add", "material": "elemental", "rarity": rar})

for name, face, rar in [("fierce",8,"epic"),("bulwark",8,"epic"),("pierce",8,"epic"),
                          ("break",10,"epic"),("resonance",10,"epic"),("fate",12,"legendary")]:
    w(f'die/mul_die_{name}.json', {"id": f"dicefate:mul_die_{name}", "face": face,
        "kind": "mul", "material": "elemental", "rarity": rar})

uniques = {
 "unique_die_cracked_face": (6, "epic"), "unique_die_two_faced": (8, "epic"),
 "unique_die_molten_core": (12, "epic", "ember"), "unique_die_frost_silence": (12, "epic", "frost"),
 "unique_die_storm_verdict": (12, "epic", "storm"), "unique_die_bone_eater": (10, "epic", "venom"),
 "unique_die_radiant_oath": (10, "epic", "radiant"), "unique_die_umbral_veil": (10, "epic", "umbral"),
 "unique_die_null_face": (6, "legendary"), "unique_die_fate_sovereign": (20, "fated"),
}
for uid, spec in uniques.items():
    face, rar = spec[0], spec[1]
    o = {"id": f"dicefate:{uid}", "face": face, "kind": "unique", "material": "unique", "rarity": rar}
    if len(spec) > 2: o["element"] = spec[2]
    if uid.endswith("null_face"): o["face_values"] = [0, 0, 0, 6, 6, 6]
    w(f'die/{uid}.json', o)

cursed = {"cursed_die_greed": 12, "cursed_die_blood": 10,
          "cursed_die_fracture": 20, "cursed_die_misfortune": 8}
for cid, face in cursed.items():
    w(f'die/{cid}.json', {"id": f"dicefate:{cid}", "face": face, "kind": "cursed",
        "material": "cursed", "rarity": "epic"})

print('die defs done')

# ============ 2. 生物档案（18 原版 + 4 Boss + 4 守卫） ============
def profile(pid, entity, tier, rank, slots, stance, drops, skill=None):
    o = {"id": f"dicefate:{pid}", "entity": entity, "tier": tier, "rank": rank,
         "slots": [{"face": f, **({"element": e} if e else {})} for f, e in slots],
         "stance": stance, "drops": drops}
    if skill: o["skill_dice"] = skill
    w(f'pool_profile/{pid}.json', o)

SAND = lambda mn, mx, ch=1.0: {"item": "fate_sand", "min": mn, "max": mx, "chance": ch}
DUST = lambda mn, mx, ch=0.7: {"item": "fate_dust", "min": mn, "max": mx, "chance": ch}

profile("minecraft_zombie", "minecraft:zombie", 0, "normal", [(4,None)], "balanced",
        [SAND(1,3), DUST(1,2)])
profile("minecraft_skeleton", "minecraft:skeleton", 0, "normal", [(6,None)], "backwater",
        [SAND(1,3), DUST(1,2)])
profile("minecraft_spider", "minecraft:spider", 0, "normal", [(4,None)], "tricky",
        [SAND(1,2), DUST(1,1)])
profile("minecraft_silverfish", "minecraft:silverfish", 0, "normal", [(4,None)], "balanced",
        [DUST(1,1)])
profile("minecraft_slime", "minecraft:slime", 0, "normal", [(4,None)], "balanced",
        [DUST(1,2), {"item": "cracked_core", "min": 1, "max": 1, "chance": 0.10}])
profile("minecraft_husk", "minecraft:husk", 1, "normal", [(6,None)], "backwater",
        [SAND(2,3), DUST(1,2)])
profile("minecraft_stray", "minecraft:stray", 1, "normal", [(6,"frost")], "guard",
        [SAND(1,3), {"item": "element_essence_frost", "min": 1, "max": 1, "chance": 0.4}])
profile("minecraft_cave_spider", "minecraft:cave_spider", 1, "normal", [(4,"venom"),(4,"venom")], "tricky",
        [{"item": "element_essence_venom", "min": 1, "max": 1, "chance": 0.5}])
profile("minecraft_pillager", "minecraft:pillager", 1, "normal", [(6,None),(8,None)], "balanced",
        [SAND(2,4)])
profile("minecraft_vindicator", "minecraft:vindicator", 1, "elite", [(6,None),(8,None)], "backwater",
        [SAND(4,8), {"item": "fate_core", "min": 1, "max": 1, "chance": 0.35}])
profile("minecraft_evoker", "minecraft:evoker", 2, "elite", [(8,"storm"),(8,"storm")], "guard",
        [SAND(4,8), {"item": "fate_core", "min": 1, "max": 1, "chance": 0.5},
         {"item": "element_essence_storm", "min": 1, "max": 1, "chance": 0.6}])
profile("minecraft_blaze", "minecraft:blaze", 2, "normal", [(8,"ember"),(8,"ember")], "backwater",
        [{"item": "element_essence_ember", "min": 1, "max": 2, "chance": 0.8}, {"item": "starshard", "min": 1, "max": 1, "chance": 0.2}])
profile("minecraft_wither_skeleton", "minecraft:wither_skeleton", 2, "normal", [(8,"umbral"),(8,"umbral")], "backwater",
        [{"item": "element_essence_umbral", "min": 1, "max": 1, "chance": 0.6}, SAND(2,4)])
profile("minecraft_enderman", "minecraft:enderman", 2, "normal", [(8,"umbral"),(10,"umbral"),(10,"umbral")], "tricky",
        [{"item": "element_essence_umbral", "min": 1, "max": 1, "chance": 0.5},
         {"item": "starshard", "min": 1, "max": 2, "chance": 0.5}])
profile("minecraft_shulker", "minecraft:shulker", 3, "elite", [(10,"radiant"),(10,"radiant"),(10,"radiant")], "guard",
        [{"item": "element_essence_radiant", "min": 1, "max": 1, "chance": 0.6},
         {"item": "starshard", "min": 1, "max": 1, "chance": 0.4}])
profile("minecraft_piglin_brute", "minecraft:piglin_brute", 3, "elite", [(10,None),(10,None),(12,None)], "backwater",
        [SAND(8,16), {"item": "fate_core", "min": 1, "max": 1, "chance": 0.6}])
profile("minecraft_elder_guardian", "minecraft:elder_guardian", 3, "elite", [(10,None),(12,None),(12,None)], "guard",
        [SAND(8,16), {"item": "fate_core", "min": 1, "max": 1, "chance": 0.8},
         {"item": "starshard", "min": 1, "max": 2, "chance": 0.6}])
profile("minecraft_illusioner", "minecraft:illusioner", 3, "elite", [(10,None),(10,None),(10,None)], "tricky",
        [SAND(8,16), {"item": "fate_core", "min": 1, "max": 1, "chance": 0.5},
         {"item": "mirror_shard", "min": 1, "max": 1, "chance": 0.4}])

# Bosses
profile("boss_two_faced_gambler", "dicefate:two_faced_gambler", 2, "boss",
        [(8,None),(8,None),(10,None),(10,None)], "tricky",
        [SAND(40,80), {"item": "fate_fragment", "min": 1, "max": 2, "chance": 1.0}],
        ["dicefate:skill/reverse"])
profile("boss_molten_die_king", "dicefate:molten_die_king", 3, "boss",
        [(10,"ember"),(10,"ember"),(12,"ember"),(12,"ember"),(12,"ember")], "backwater",
        [SAND(60,120), {"item": "fate_fragment", "min": 1, "max": 2, "chance": 1.0},
         {"item": "starshard", "min": 2, "max": 4, "chance": 1.0}])
profile("boss_frost_silence_queen", "dicefate:frost_silence_queen", 4, "boss",
        [(8,"frost"),(10,"frost"),(12,"frost"),(12,"frost"),(12,"frost")], "guard",
        [SAND(60,120), {"item": "fate_fragment", "min": 1, "max": 2, "chance": 1.0},
         {"item": "starshard", "min": 2, "max": 4, "chance": 1.0}])
profile("boss_nullface", "dicefate:nullface", 5, "boss",
        [(12,None),(12,None),(12,None),(12,None),(20,None),(20,None)], "balanced",
        [SAND(150,300), {"item": "fate_fragment", "min": 2, "max": 4, "chance": 1.0},
         {"item": "sovereign_core", "min": 1, "max": 1, "chance": 1.0}])

print('profiles done')

# ============ 3. 骰包 ============
packs = [
 ("pack_common", 3, "replace", 0, [(6,None),(6,None)]),
 ("pack_fine", 3, "replace", 0, [(6,None),(8,None),(8,None)]),
 ("pack_rare", 3, "replace", 0, [(8,None),(8,None),(10,None),(10,None)]),
 ("pack_epic", 3, "replace", 0, [(10,None),(10,None),(12,None),(12,None),(12,None)]),
 ("pack_fate_minor", 3, "fate", 0.5, [(6,None),(8,None),(8,None)]),
 ("pack_fate_major", 2, "fate", 0.75, [(8,None),(10,None),(10,None),(12,None)]),
 ("pack_ember", 4, "replace", 0, [(8,"ember"),(8,"ember"),(10,"ember")]),
 ("pack_frost", 4, "replace", 0, [(8,"frost"),(8,"frost"),(10,"frost")]),
 ("pack_storm", 4, "replace", 0, [(8,"storm"),(8,"storm"),(10,"storm")]),
 ("pack_venom", 4, "replace", 0, [(8,"venom"),(8,"venom"),(10,"venom")]),
 ("pack_radiant", 4, "replace", 0, [(8,"radiant"),(10,"radiant"),(10,"radiant")]),
 ("pack_umbral", 4, "replace", 0, [(8,"umbral"),(10,"umbral"),(10,"umbral")]),
 ("pack_straight", 2, "replace", 0, [(4,None),(6,None),(8,None),(10,None),(12,None)]),
 ("pack_triple", 2, "replace", 0, [(8,None),(8,None),(8,None),(8,None)]),
 ("pack_two_faced", 2, "replace", 0, [(6,None),(8,None),(12,None),(20,None)]),
 ("pack_null", 2, "replace", 0, [(6,None),(6,None),(6,None),(6,None)]),
]
for pid, dur, mode, co, slots in packs:
    w(f'pack/{pid}.json', {
        "id": f"dicefate:{pid}", "duration": dur, "mode": mode, "co_roll": co,
        "slots": [{"face": f, **({"element": e} if e else {})} for f, e in slots]})

# ============ 4. 反应 ============
w('reaction/steam.json', {"id": "dicefate:steam", "elements": ["ember", "frost"],
    "effect": "compress_margin", "value": 0.5, "bonus_damage": 0.10, "cooldown": 160})
w('reaction/detonate.json', {"id": "dicefate:detonate", "elements": ["ember", "storm"],
    "effect": "true_damage", "value": 0.03, "bonus_damage": 0.0, "cooldown": 200})
w('reaction/corrode.json', {"id": "dicefate:corrode", "elements": ["venom", "storm"],
    "effect": "corrode", "value": 0.15, "bonus_damage": 0.0, "cooldown": 160})
w('reaction/annihilate.json', {"id": "dicefate:annihilate", "elements": ["radiant", "umbral"],
    "effect": "nullify_tie", "value": 0.0, "bonus_damage": 0.0, "cooldown": 300})

# ============ 5. 协同 ============
synergies = [
 ("same_face_three", "same_face", 3, 1, 2),
 ("all_six", "all_six", 6, 0.10, 0),
 ("ladder", "ladder", 3, 2, 0),
 ("dual_resonance", "dual_resonance", 2, 0.20, 0.05),
 ("tetra", "tetra", 4, 0.10, 0.05),
 ("mono", "mono", 3, 0.15, 0),
 ("pack_triple", "pack_triple", 3, 0.20, 0),
 ("bone_set", "bone_set", 3, 1, 0),
 ("iron_set", "iron_set", 3, 1, 0),
 ("mithril_set", "mithril_set", 3, 1, 0),
]
for sid, typ, count, v, v2 in synergies:
    w(f'synergy/{sid}.json', {"id": f"dicefate:{sid}", "type": typ, "count": count,
        "value": v, "value2": v2})

# ============ 6. 命纹 ============
runes = [
 ("rune_steady", "die", "min_as_two", 2, False, 20, "offering_bone"),
 ("rune_edge", "die", "poise_plus", 1, False, 30, "offering_bone"),
 ("rune_wall", "die", "def_plus", 1, False, 30, "offering_bone"),
 ("rune_pierce", "die", "pierce_plus", 0.10, False, 40, "offering_star"),
 ("rune_combo", "die", "momentum_plus", 2, False, 30, "offering_bone"),
 ("rune_reroll", "die", "reroll_once", 0.10, True, 60, "offering_core"),
 ("rune_reverse", "player", "null_to_loss", 0.15, True, 80, "offering_core"),
 ("rune_guard", "player", "damage_reduce", 0.05, False, 50, "offering_star"),
 ("rune_hunt", "player", "kill_heal", 2, False, 40, "offering_bone"),
 ("rune_sacrifice", "player", "break_frenzy", 0.05, False, 60, "offering_core"),
 ("rune_greedy", "die", "greedy", 0.05, False, 40, "offering_star"),
 ("rune_pity", "player", "pity_step", 0.01, False, 80, "offering_core"),
]
for rid, scope, effect, v, consumed, cost, offering in runes:
    w(f'fate_rune/{rid}.json', {"id": f"dicefate:{rid}", "scope": scope, "effect": effect,
        "value": v, "consumed": consumed, "cost_sand": cost, "offering": f"dicefate:{offering}"})

# ============ 7. 命破伤害类型 ============
w('../dicefate/damage_type/fatebreak.json', {
    "message_id": "dicefate.fatebreak",
    "exhaustion": 0.0,
    "scaling": "never"})

print('all data JSON done')
