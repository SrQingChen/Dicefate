# -*- coding: utf-8 -*-
"""生成配方 + Advancement + 战利品表。"""
import json, os

RES = 'src/main/resources'
def w(path, obj):
    full = os.path.join(RES, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)

def shaped(pattern, key, result, count=1):
    return {"type": "minecraft:crafting_shaped", "category": "misc",
            "pattern": pattern, "key": {k: {"item": v} for k, v in key.items()},
            "result": {"id": result, "count": count}}

def shapeless(ing, result, count=1):
    return {"type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [{"item": i} for i in ing],
            "result": {"id": result, "count": count}}

# ============ 配方（规划 5.7 + 材料） ============
recipes = {}
# 骰胚
recipes['blank_die_bone'] = shaped(["BBD", "BDB", "DLD"],
    {"B": "minecraft:bone", "D": "dicefate:fate_dust", "L": "minecraft:string"}, "dicefate:blank_die_bone")
recipes['blank_die_iron'] = shaped(["IID", "IDI", "DRD"],
    {"I": "minecraft:iron_ingot", "D": "dicefate:fate_sand", "R": "minecraft:redstone"}, "dicefate:blank_die_iron")
recipes['blank_die_mithril'] = shaped(["AAD", "GCG", "DLD"],
    {"A": "minecraft:amethyst_shard", "D": "dicefate:fate_dust", "G": "minecraft:gold_ingot",
     "C": "dicefate:fate_core", "L": "minecraft:lapis_lazuli"}, "dicefate:blank_die_mithril")
recipes['blank_die_fategold'] = shaped(["NAD", "ACA", "DSN"],
    {"N": "minecraft:netherite_scrap", "A": "minecraft:amethyst_shard", "D": "dicefate:fate_sand",
     "C": "dicefate:fate_core", "S": "minecraft:nether_star"}, "dicefate:blank_die_fategold")
recipes['blank_die_starlight'] = shaped(["EAD", "ASG", "DAE"],
    {"E": "minecraft:ender_pearl", "A": "minecraft:amethyst_shard", "D": "dicefate:starshard",
     "G": "dicefate:fate_sand", "S": "minecraft:nether_star"}, "dicefate:blank_die_starlight")

# 刻片
plate_map = {"d4": ("minecraft:stone", "minecraft:bone"), "d6": ("minecraft:bone", "minecraft:copper_ingot"),
             "d8": ("minecraft:iron_ingot", "minecraft:gold_ingot"), "d10": ("minecraft:gold_ingot", "minecraft:diamond"),
             "d12": ("minecraft:diamond", "minecraft:amethyst_shard"), "d20": ("minecraft:netherite_scrap", "minecraft:amethyst_shard"),
             "d24": ("dicefate:starshard", "minecraft:dragon_breath")}
for name, (a, b) in plate_map.items():
    recipes[f'face_plate_{name}'] = shaped(["ABA", "BDB", "ABA"],
        {"A": a, "B": b, "D": "dicefate:fate_sand"}, f"dicefate:face_plate_{name}")

# 扩容符
recipes['slot_expand_1'] = shaped(["ASA", "SCS", "AGA"],
    {"A": "minecraft:amethyst_shard", "S": "dicefate:slot_shard", "C": "dicefate:fate_core",
     "G": "minecraft:gold_ingot"}, "dicefate:slot_expand_1")
recipes['slot_expand_2'] = shaped(["ASA", "SCS", "ADA"],
    {"A": "minecraft:amethyst_shard", "S": "dicefate:slot_shard", "C": "dicefate:fate_core",
     "D": "minecraft:diamond"}, "dicefate:slot_expand_2")
recipes['slot_expand_3'] = shaped(["ASA", "NCN", "BFB"],
    {"A": "minecraft:amethyst_shard", "S": "dicefate:slot_shard", "N": "minecraft:netherite_scrap",
     "C": "dicefate:fate_core", "B": "minecraft:blaze_rod", "F": "dicefate:cracked_core"}, "dicefate:slot_expand_3")
recipes['slot_expand_4'] = shaped(["NSN", "SCS", "NDN"],
    {"N": "minecraft:nether_star", "S": "dicefate:slot_shard", "C": "dicefate:fate_core",
     "D": "minecraft:netherite_scrap"}, "dicefate:slot_expand_4")
recipes['slot_expand_5'] = shaped(["NSN", "DCD", "BTB"],
    {"N": "minecraft:nether_star", "S": "dicefate:slot_shard", "C": "dicefate:fate_core",
     "D": "minecraft:dragon_breath", "B": "dicefate:fate_fragment", "T": "minecraft:heart_of_the_sea"}, "dicefate:slot_expand_5")

# 功能方块
recipes['fate_altar'] = shaped(["OAO", "ASA", "OCO"],
    {"O": "minecraft:obsidian", "A": "minecraft:amethyst_shard", "S": "dicefate:fate_sand",
     "C": "dicefate:fate_core"}, "dicefate:fate_altar")
recipes['die_engraver'] = shaped(["III", "ASA", "IBI"],
    {"I": "minecraft:iron_block", "A": "minecraft:amethyst_shard", "S": "dicefate:fate_sand",
     "B": "minecraft:bone"}, "dicefate:die_engraver")
recipes['appraisal_table'] = shaped(["GLG", "IAI", "I I"],
    {"G": "minecraft:glass", "L": "minecraft:lapis_lazuli", "A": "minecraft:amethyst_shard",
     "I": "minecraft:iron_ingot"}, "dicefate:appraisal_table")
recipes['repair_bench'] = shaped(["IFI", "ASA", "I I"],
    {"I": "minecraft:iron_block", "F": "dicefate:repair_file", "S": "dicefate:fate_sand",
     "A": "minecraft:amethyst_shard"}, "dicefate:repair_bench")
recipes['fate_loom'] = shaped(["LSL", "SIS", "LSL"],
    {"L": "minecraft:string", "S": "dicefate:fate_sand", "I": "minecraft:iron_ingot"}, "dicefate:fate_loom")
recipes['offering_shrine'] = shaped(["NIN", "ISI", "NNN"],
    {"N": "minecraft:stone_bricks", "I": "dicefate:incense", "S": "dicefate:fate_sand"}, "dicefate:offering_shrine")
recipes['merchant_stall'] = shaped(["WWW", "S S", "W W"],
    {"W": "minecraft:oak_slab", "S": "dicefate:fate_sand"}, "dicefate:merchant_stall")
recipes['null_portal'] = shaped(["OSO", "ENE", "OPO"],
    {"O": "minecraft:obsidian", "S": "dicefate:starshard", "E": "minecraft:ender_pearl",
     "N": "minecraft:nether_star", "P": "minecraft:purpur_block"}, "dicefate:null_portal")

# 工具/消耗品
recipes['rune_ink'] = shapeless(["minecraft:ink_sac", "dicefate:fate_sand", "minecraft:lapis_lazuli"], "dicefate:rune_ink", 2)
recipes['repair_file'] = shapeless(["minecraft:iron_ingot", "dicefate:fate_sand"], "dicefate:repair_file")
recipes['fate_glue'] = shapeless(["dicefate:fate_sand", "minecraft:bone_meal"], "dicefate:fate_glue")
recipes['perfect_oil'] = shapeless(["dicefate:starshard", "dicefate:fate_core"], "dicefate:perfect_oil")
recipes['spirit_feed'] = shapeless(["minecraft:wheat", "minecraft:wheat", "dicefate:fate_dust"], "dicefate:spirit_feed", 3)
recipes['appraisal_scroll'] = shapeless(["minecraft:paper", "dicefate:fate_dust"], "dicefate:appraisal_scroll")
recipes['incense'] = shapeless(["minecraft:stick", "dicefate:fate_dust", "minecraft:grass_block"], "dicefate:incense", 2) if False else \
    shapeless(["minecraft:stick", "dicefate:fate_dust", "minecraft:wheat"], "dicefate:incense", 2)
recipes['sigil_echo'] = shaped(["SCS", "CFC", "SCS"],
    {"S": "dicefate:fate_sand", "C": "dicefate:fate_core", "F": "dicefate:face_plate_d6"}, "dicefate:sigil_echo")
recipes['fate_lock_talisman'] = shapeless(["dicefate:fate_core", "dicefate:cracked_core"], "dicefate:fate_lock_talisman")
recipes['lucky_cookie'] = shapeless(["minecraft:wheat", "minecraft:sugar", "dicefate:fate_sand"], "dicefate:lucky_cookie", 2)

# 武器/盾
recipes['dice_blade_bone'] = shaped(["B", "B", "S"],
    {"B": "minecraft:bone", "S": "dicefate:fate_sand"}, "dicefate:dice_blade_bone")
recipes['dice_blade_iron'] = shaped(["I", "I", "S"],
    {"I": "minecraft:iron_ingot", "S": "dicefate:fate_sand"}, "dicefate:dice_blade_iron")
recipes['dice_blade_mithril'] = shaped(["A", "A", "S"],
    {"A": "minecraft:amethyst_shard", "S": "dicefate:fate_core"}, "dicefate:dice_blade_mithril")
recipes['dice_blade_fategold'] = shaped(["C", "C", "F"],
    {"C": "dicefate:fate_core", "F": "dicefate:fate_fragment"}, "dicefate:dice_blade_fategold")
recipes['dice_sling_bone'] = shaped(["SL ", " SW", "  L"],
    {"S": "minecraft:string", "L": "minecraft:leather", "W": "minecraft:bone"}, "dicefate:dice_sling_bone")
recipes['dice_sling_mithril'] = shaped(["SL ", " SA", "  L"],
    {"S": "minecraft:string", "L": "minecraft:leather", "A": "minecraft:amethyst_shard"}, "dicefate:dice_sling_mithril")
recipes['fate_arrow'] = shaped(["A", "S", "F"],
    {"A": "minecraft:arrow", "S": "dicefate:fate_sand", "F": "minecraft:flint"}, "dicefate:fate_arrow", 4)
recipes['fate_shield_bone'] = shaped(["BBB", "BSB", " B "],
    {"B": "minecraft:bone", "S": "dicefate:fate_sand"}, "dicefate:fate_shield_bone")
recipes['fate_shield_mithril'] = shaped(["AAA", "ASG", " A "],
    {"A": "minecraft:amethyst_shard", "S": "dicefate:fate_core", "G": "minecraft:gold_ingot"}, "dicefate:fate_shield_mithril")
recipes['fate_shield_fategold'] = shaped(["CCC", "CNC", " C "],
    {"C": "dicefate:fate_core", "N": "minecraft:netherite_scrap"}, "dicefate:fate_shield_fategold")

for name, r in recipes.items():
    w(f'data/dicefate/recipe/{name}.json', r)
print('recipes:', len(recipes))

# ============ Advancement（33） ============
def adv(name, title_zh, title_en, desc_zh, desc_en, icon, parent, crit):
    return {
        "display": {
            "icon": {"id": icon},
            "title": {"translate": f"advancement.dicefate.{name}.title"},
            "description": {"translate": f"advancement.dicefate.{name}.desc"},
            "frame": "task"
        },
        "parent": parent,
        "criteria": crit
    }

advancements = [
 ("root", "命数潮汐", "Tides of Fate", "世界运转在骰池之上", "The world runs on dice pools",
  "dicefate:die_bone_d6", None, {"has_die": {"trigger": "minecraft:inventory_changed",
      "conditions": {"items": [{"items": "#dicefate:dice"}]}}}),
 ("first_contest", "第一次掷命", "First Contest", "完成一次拼点", "Complete a contest",
  "dicefate:fate_sand", "dicefate:root", {"tick": {"trigger": "minecraft:tick"}}),
 ("first_win", "赢了", "Victory", "赢得一次攻击拼点", "Win an attack contest",
  "dicefate:die_bone_d6", "dicefate:first_contest", {"tick": {"trigger": "minecraft:tick"}}),
 ("first_miss", "Miss！", "Miss!", "掷出一次崩", "Roll a crash",
  "dicefate:die_bone_d4", "dicefate:first_contest", {"tick": {"trigger": "minecraft:tick"}}),
 ("first_poise", "命势初裂", "Poise Break", "击破一个生物的命势", "Break a creature's poise",
  "dicefate:fate_core", "dicefate:first_win", {"tick": {"trigger": "minecraft:tick"}}),
 ("first_craft", "自己造一颗", "Die Forger", "用刻骰台造出骰", "Forge a die",
  "dicefate:blank_die_bone", "dicefate:root", {"has_blank": {"trigger": "minecraft:inventory_changed",
      "conditions": {"items": [{"items": "dicefate:blank_die_bone"}]}}}),
 ("expand_1", "第二格", "Second Slot", "使用扩容符 I", "Use Expansion I",
  "dicefate:slot_expand_1", "dicefate:first_craft", {"has": {"trigger": "minecraft:inventory_changed",
      "conditions": {"items": [{"items": "dicefate:slot_expand_1"}]}}}),
 ("expand_3", "四格命盘", "Four Slots", "基础骰位达到 4", "Reach 4 base slots",
  "dicefate:slot_expand_3", "dicefate:expand_1", {"tick": {"trigger": "minecraft:tick"}}),
 ("expand_6", "六格", "Six Slots", "基础骰位达到 6", "Reach 6 base slots",
  "dicefate:slot_expand_5", "dicefate:expand_3", {"tick": {"trigger": "minecraft:tick"}}),
 ("add_slot", "加算", "Addition", "解锁加算槽", "Unlock additive slot",
  "dicefate:sigil_addition", "dicefate:expand_3", {"has": {"trigger": "minecraft:inventory_changed",
      "conditions": {"items": [{"items": "dicefate:sigil_addition"}]}}}),
 ("mult_slot", "乘算", "Multiplication", "解锁乘算槽", "Unlock multiplicative slot",
  "dicefate:sigil_multiplication", "dicefate:expand_3", {"has": {"trigger": "minecraft:inventory_changed",
      "conditions": {"items": [{"items": "dicefate:sigil_multiplication"}]}}}),
 ("skill_slot", "回响", "Echo", "解锁技能槽", "Unlock skill slot",
  "dicefate:sigil_echo", "dicefate:expand_3", {"has": {"trigger": "minecraft:inventory_changed",
      "conditions": {"items": [{"items": "dicefate:sigil_echo"}]}}}),
 ("break_elite", "破势者", "Poise Breaker", "击破精英的命势", "Break an elite's poise",
  "dicefate:cracked_core", "dicefate:first_poise", {"tick": {"trigger": "minecraft:tick"}}),
 ("fatebreak", "命破一击", "Fatebreak", "触发一次命破", "Trigger a fatebreak",
  "minecraft:nether_star", "dicefate:break_elite", {"tick": {"trigger": "minecraft:tick"}}),
 ("fatewound", "命损", "Fate Wound", "在命损窗口击杀", "Kill during fate wound",
  "minecraft:amethyst_shard", "dicefate:fatebreak", {"tick": {"trigger": "minecraft:tick"}}),
 ("break_boss", "赌王破势", "Boss Breaker", "击破 Boss 的命势", "Break a boss's poise",
  "dicefate:fate_fragment", "dicefate:break_elite", {"tick": {"trigger": "minecraft:tick"}}),
 ("straight", "顺子", "Straight", "触发一次顺子", "Roll a straight",
  "dicefate:die_bone_d6", "dicefate:expand_1", {"tick": {"trigger": "minecraft:tick"}}),
 ("triple", "豹子", "Triple", "触发一次豹子", "Roll a triple",
  "dicefate:die_iron_d6", "dicefate:expand_1", {"tick": {"trigger": "minecraft:tick"}}),
 ("steam", "蒸汽", "Steam", "触发一次元素反应", "Trigger a reaction",
  "dicefate:element_essence_ember", "dicefate:expand_3", {"tick": {"trigger": "minecraft:tick"}}),
 ("four_elements", "四元", "Four Elements", "命盘同时有 4 种元素", "Hold 4 elements",
  "dicefate:element_resonator", "dicefate:steam", {"tick": {"trigger": "minecraft:tick"}}),
 ("synergy_3", "共鸣", "Synergy", "同时激活 3 个协同", "3 synergies active",
  "dicefate:die_mithril_d12", "dicefate:expand_3", {"tick": {"trigger": "minecraft:tick"}}),
 ("rune", "命纹", "Fate Rune", "施加一条命纹", "Apply a fate rune",
  "dicefate:rune_ink", "dicefate:first_craft", {"tick": {"trigger": "minecraft:tick"}}),
 ("appraise_1", "鉴定", "Appraisal", "鉴定一颗未鉴定骰", "Appraise a die",
  "dicefate:unidentified_die", "dicefate:root", {"has": {"trigger": "minecraft:inventory_changed",
      "conditions": {"items": [{"items": "dicefate:unidentified_die"}]}}}),
 ("repair_1", "修复", "Repair", "修复一颗磨损骰", "Repair a worn die",
  "dicefate:repair_file", "dicefate:first_craft", {"tick": {"trigger": "minecraft:tick"}}),
 ("blind_box", "盲盒", "Blind Box", "开启未鉴定骰", "Open an unidentified die",
  "dicefate:pack_common", "dicefate:appraise_1", {"tick": {"trigger": "minecraft:tick"}}),
 ("unique_die", "唯一", "Unique", "获得唯一骰", "Obtain a unique die",
  "dicefate:unique_die_cracked_face", "dicefate:blind_box", {"tick": {"trigger": "minecraft:tick"}}),
 ("black_market", "黑市", "Black Market", "用凭证解锁黑市", "Unlock black market",
  "dicefate:merchant_token", "dicefate:appraise_1", {"has": {"trigger": "minecraft:inventory_changed",
      "conditions": {"items": [{"items": "dicefate:merchant_token"}]}}}),
 ("casino", "无面赌场", "Faceless Casino", "进入无面赌场", "Enter the Faceless Casino",
  "dicefate:null_portal", "dicefate:expand_6", {"tick": {"trigger": "minecraft:tick"}}),
 ("four_seals", "四印", "Four Seals", "集齐 4 枚命主之印", "Collect 4 sovereign seals",
  "dicefate:sovereign_seal", "dicefate:casino", {"has": {"trigger": "minecraft:inventory_changed",
      "conditions": {"items": [{"items": "dicefate:sovereign_seal", "count": 4}]}}}),
 ("nullface", "零面者", "Nullface", "击败零面者", "Defeat Nullface",
  "dicefate:sovereign_core", "dicefate:four_seals", {"tick": {"trigger": "minecraft:tick"}}),
 ("d24", "二十四面", "Twenty-Four", "合成 d24", "Craft a d24",
  "dicefate:face_plate_d24", "dicefate:nullface", {"has": {"trigger": "minecraft:inventory_changed",
      "conditions": {"items": [{"items": "dicefate:die_starlight_d24"}]}}}),
 ("full_codex", "全图鉴", "Full Codex", "收集 83 种骰子", "Collect all 83 dice",
  "dicefate:die_starlight_d20", "dicefate:nullface", {"tick": {"trigger": "minecraft:tick"}}),
 ("no_hit_boss", "无伤赌局", "Flawless Gamble", "无伤击败主 Boss", "Beat a boss unharmed",
  "dicefate:die_fategold_d20", "dicefate:break_boss", {"tick": {"trigger": "minecraft:tick"}}),
]
adv_titles = {}
for name, tzh, ten, dzh, den, icon, parent, crit in advancements:
    a = adv(name, tzh, ten, dzh, den, icon, parent, crit)
    if name == "root":
        a["display"]["background"] = "dicefate:textures/gui/advancement_background.png"
        a["display"]["frame"] = "task"
    w(f'data/dicefate/advancement/{name}.json', a)
    adv_titles[f"advancement.dicefate.{name}.title"] = (tzh, ten)
    adv_titles[f"advancement.dicefate.{name}.desc"] = (dzh, den)
print('advancements:', len(advancements))

# 进度文案并入 lang
import json as _j
for lang_file, idx in [('assets/dicefate/lang/zh_cn.json', 0), ('assets/dicefate/lang/en_us.json', 1)]:
    path = os.path.join(RES, lang_file)
    data = _j.load(open(path, encoding='utf-8'))
    for k, pair in adv_titles.items():
        data[k] = pair[idx]
    _j.dump(data, open(path, 'w', encoding='utf-8', newline='\n'), ensure_ascii=False, indent=1)
print('advancement lang merged')

# ============ 物品标签（骰子 tag，root advancement 用） ============
dice_items = []
mats_faces = [("bone",4),("bone",6),("bone",8),("iron",6),("iron",8),("iron",10),
    ("mithril",8),("mithril",10),("mithril",12),("fategold",10),("fategold",12),("fategold",20),
    ("starlight",12),("starlight",20),("starlight",24)]
for m, f in mats_faces:
    dice_items.append(f"dicefate:die_{m}_d{f}")
w('data/dicefate/tags/item/dice.json', {"replace": False, "values": dice_items})

# ============ 方块掉落 ============
for bid in ["fate_altar", "die_engraver", "appraisal_table", "repair_bench",
            "fate_loom", "offering_shrine", "merchant_stall"]:
    w(f'data/dicefate/loot_table/blocks/{bid}.json', {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"dicefate:{bid}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
# null_portal 不掉落
w('data/dicefate/loot_table/blocks/null_portal.json', {"type": "minecraft:block", "pools": []})

# ============ Boss 掉落 ============
boss_drops = {
 "two_faced_gambler": [("dicefate:fate_sand", 40, 80), ("dicefate:fate_fragment", 1, 2),
                        ("dicefate:sigil_addition", 1, 1), ("dicefate:unique_die_two_faced", 1, 1),
                        ("dicefate:skill_die_reverse_2", 1, 1), ("dicefate:pack_fate_minor", 1, 1)],
 "molten_die_king": [("dicefate:fate_sand", 60, 120), ("dicefate:fate_fragment", 1, 2),
                      ("dicefate:sigil_multiplication", 1, 1), ("dicefate:unique_die_molten_core", 1, 1),
                      ("dicefate:skill_die_breaker_3", 1, 1), ("dicefate:starshard", 2, 4)],
 "frost_silence_queen": [("dicefate:fate_sand", 60, 120), ("dicefate:fate_fragment", 1, 2),
                          ("dicefate:unique_die_frost_silence", 1, 1), ("dicefate:blank_die_starlight", 1, 1),
                          ("dicefate:skill_die_ward_3", 1, 1)],
 "nullface": [("dicefate:fate_sand", 150, 300), ("dicefate:sovereign_core", 1, 1),
               ("dicefate:unique_die_null_face", 1, 1), ("dicefate:unique_die_fate_sovereign", 1, 1),
               ("dicefate:blank_die_starlight", 1, 1), ("dicefate:face_plate_d24", 1, 1)],
}
for boss, drops in boss_drops.items():
    w(f'data/dicefate/loot_table/entities/{boss}.json', {
        "type": "minecraft:entity",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": it,
                         "functions": [{"function": "minecraft:set_count",
                                        "count": {"type": "minecraft:uniform", "min": mn, "max": mx}}]}]}
                for it, mn, mx in drops]})

# 投射物无掉落
for proj in ["die_projectile", "fate_arrow"]:
    w(f'data/dicefate/loot_table/entities/{proj}.json', {"type": "minecraft:entity", "pools": []})

print('loot tables done')
