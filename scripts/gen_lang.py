# -*- coding: utf-8 -*-
"""生成 zh_cn / en_us 全量 lang、配方、进度、战利品表。"""
import json, os

RES = 'src/main/resources'
def w(path, obj, base=RES):
    full = os.path.join(base, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)

# ============ 骰名映射 ============
basic_zh = {"bone": "骨骰", "iron": "铁骰", "mithril": "秘银骰", "fategold": "命金骰", "starlight": "星辉骰"}
basic_en = {"bone": "Bone Die", "iron": "Iron Die", "mithril": "Mithril Die", "fategold": "Fate-gold Die", "starlight": "Starlight Die"}
elem_zh = {"ember": "烬火", "frost": "霜冰", "storm": "雷暴", "venom": "蚀毒", "radiant": "辉光", "umbral": "暗影"}
elem_en = {"ember": "Ember", "frost": "Frost", "storm": "Storm", "venom": "Venom", "radiant": "Radiant", "umbral": "Umbral"}
tier_zh = {1: "微光", 2: "共鸣", 3: "命源"}
tier_en = {1: "Spark", 2: "Resonant", 3: "Origin"}
skill_zh = {"echo": "回响骰", "pursuit": "追击骰", "ward": "护命骰", "breaker": "破势骰",
            "reverse": "逆命骰", "greed": "贪命骰", "lock": "定命骰", "favor": "命佑骰"}
skill_en = {"echo": "Echo Die", "pursuit": "Pursuit Die", "ward": "Ward Die", "breaker": "Breaker Die",
            "reverse": "Reverse Die", "greed": "Greed Die", "lock": "Lock Die", "favor": "Favor Die"}
sk3_zh = {1: "·微响/轻追/薄护/裂势/逆流/小贪/定一/微佑".split("/"), }
skill_names_zh = {
 "echo": ["回响骰·微响", "回响骰", "回响骰·命响"],
 "pursuit": ["追击骰·轻追", "追击骰", "追击骰·命追"],
 "ward": ["护命骰·薄护", "护命骰", "护命骰·命护"],
 "breaker": ["破势骰·裂势", "破势骰", "破势骰·命破"],
 "reverse": ["逆命骰·逆流", "逆命骰", "逆命骰·命逆"],
 "greed": ["贪命骰·小贪", "贪命骰", "贪命骰·命贪"],
 "lock": ["定命骰·定一", "定命骰", "定命骰·命定"],
 "favor": ["命佑骰·微佑", "命佑骰", "命佑骰·天佑"]}
skill_names_en = {
 "echo": ["Echo Die I", "Echo Die II", "Echo Die III"],
 "pursuit": ["Pursuit Die I", "Pursuit Die II", "Pursuit Die III"],
 "ward": ["Ward Die I", "Ward Die II", "Ward Die III"],
 "breaker": ["Breaker Die I", "Breaker Die II", "Breaker Die III"],
 "reverse": ["Reverse Die I", "Reverse Die II", "Reverse Die III"],
 "greed": ["Greed Die I", "Greed Die II", "Greed Die III"],
 "lock": ["Lock Die I", "Lock Die II", "Lock Die III"],
 "favor": ["Favor Die I", "Favor Die II", "Favor Die III"]}

items_zh, items_en = {}, {}

# 基础骰 15
faces = [4, 6, 8, 10, 12, 20, 24]
mats = ["bone", "iron", "mithril", "fategold", "starlight"]
combos = [("bone",4),("bone",6),("bone",8),("iron",6),("iron",8),("iron",10),
          ("mithril",8),("mithril",10),("mithril",12),("fategold",10),("fategold",12),("fategold",20),
          ("starlight",12),("starlight",20),("starlight",24)]
for mat, face in combos:
    did = f"die_{mat}_d{face}"
    items_zh[f"item.dicefate.{did}"] = f"{basic_zh[mat]}·d{face}"
    items_en[f"item.dicefate.{did}"] = f"{basic_en[mat]} d{face}"

# 元素骰 18
for e in elem_zh:
    for suf, t in [("spark", 1), ("resonant", 2), ("origin", 3)]:
        did = f"die_{e}_{suf}"
        items_zh[f"item.dicefate.{did}"] = f"{elem_zh[e]}骰·{tier_zh[t]}"
        items_en[f"item.dicefate.{did}"] = f"{elem_en[e]} Die · {tier_en[t]}"

# 技能骰 24
for fam in skill_zh:
    for t in range(1, 4):
        did = f"skill_die_{fam}_{t}"
        items_zh[f"item.dicefate.{did}"] = skill_names_zh[fam][t-1]
        items_en[f"item.dicefate.{did}"] = skill_names_en[fam][t-1]

# 加算/乘算 12
add_zh = {"edge": "加算骰·锋", "wall": "加算骰·壁", "poise": "加算骰·势", "element": "加算骰·元", "combo": "加算骰·连", "mend": "加算骰·愈"}
add_en = {"edge": "Additive Die · Edge", "wall": "Additive Die · Wall", "poise": "Additive Die · Poise",
          "element": "Additive Die · Element", "combo": "Additive Die · Combo", "mend": "Additive Die · Mend"}
for k, v in add_zh.items():
    items_zh[f"item.dicefate.add_die_{k}"] = v
    items_en[f"item.dicefate.add_die_{k}"] = add_en[k]
mul_zh = {"fierce": "乘算骰·烈", "bulwark": "乘算骰·壁", "pierce": "乘算骰·破", "break": "乘算骰·势", "resonance": "乘算骰·元", "fate": "乘算骰·命"}
mul_en = {"fierce": "Multiplicative Die · Fierce", "bulwark": "Multiplicative Die · Bulwark", "pierce": "Multiplicative Die · Pierce",
          "break": "Multiplicative Die · Break", "resonance": "Multiplicative Die · Resonance", "fate": "Multiplicative Die · Fate"}
for k, v in mul_zh.items():
    items_zh[f"item.dicefate.mul_die_{k}"] = v
    items_en[f"item.dicefate.mul_die_{k}"] = mul_en[k]

# 唯一骰 10
uniq = {
 "unique_die_cracked_face": ("裂面者之骰", "Cracked Face Die"),
 "unique_die_two_faced": ("双面赌徒之骰", "Two-Faced Gambler's Die"),
 "unique_die_molten_core": ("熔核骰", "Molten Core Die"),
 "unique_die_frost_silence": ("霜寂骰", "Frost Silence Die"),
 "unique_die_storm_verdict": ("雷罚骰", "Storm Verdict Die"),
 "unique_die_bone_eater": ("蚀骨骰", "Bone Eater Die"),
 "unique_die_radiant_oath": ("圣辉骰", "Radiant Oath Die"),
 "unique_die_umbral_veil": ("影噬骰", "Umbral Veil Die"),
 "unique_die_null_face": ("零面骰", "Null Face Die"),
 "unique_die_fate_sovereign": ("命主之骰", "Fate Sovereign Die")}
for did, (zh, en) in uniq.items():
    items_zh[f"item.dicefate.{did}"] = zh
    items_en[f"item.dicefate.{did}"] = en

# 诅咒骰 4
cursed = {"cursed_die_greed": ("贪骰", "Cursed Die of Greed"), "cursed_die_blood": ("血骰", "Cursed Die of Blood"),
          "cursed_die_fracture": ("裂命骰", "Cursed Die of Fracture"), "cursed_die_misfortune": ("逆运骰", "Cursed Die of Misfortune")}
for did, (zh, en) in cursed.items():
    items_zh[f"item.dicefate.{did}"] = zh
    items_en[f"item.dicefate.{did}"] = en

# 材料/工具/消耗品
misc = {
 "blank_die_bone": ("骨制空骰胚", "Bone Blank Die"), "blank_die_iron": ("铁制空骰胚", "Iron Blank Die"),
 "blank_die_mithril": ("秘银空骰胚", "Mithril Blank Die"), "blank_die_fategold": ("命金空骰胚", "Fate-gold Blank Die"),
 "blank_die_starlight": ("星辉空骰胚", "Starlight Blank Die"),
 "fate_sand": ("命砂", "Fate Sand"), "fate_dust": ("命尘", "Fate Dust"), "fate_core": ("命核", "Fate Core"),
 "starshard": ("星屑", "Starshard"), "cracked_core": ("裂纹核心", "Cracked Core"),
 "fate_fragment": ("命运碎片", "Fate Fragment"), "sovereign_core": ("命主核心", "Sovereign Core"),
 "element_essence_ember": ("烬火精华", "Ember Essence"), "element_essence_frost": ("霜冰精华", "Frost Essence"),
 "element_essence_storm": ("雷暴精华", "Storm Essence"), "element_essence_venom": ("蚀毒精华", "Venom Essence"),
 "element_essence_radiant": ("辉光精华", "Radiant Essence"), "element_essence_umbral": ("暗影精华", "Umbral Essence"),
 "rune_ink": ("命纹墨", "Rune Ink"), "repair_file": ("修复锉", "Repair File"), "fate_glue": ("命胶", "Fate Glue"),
 "perfect_oil": ("完美修复油", "Perfect Oil"), "spirit_feed": ("灵粮", "Spirit Feed"),
 "appraisal_lens": ("鉴定镜", "Appraisal Lens"), "appraisal_scroll": ("鉴定卷轴", "Appraisal Scroll"),
 "mirror_shard": ("命镜碎片", "Mirror Shard"), "slot_shard": ("槽位碎片", "Slot Shard"),
 "slot_expand_1": ("骰位扩容符 I", "Slot Expansion I"), "slot_expand_2": ("骰位扩容符 II", "Slot Expansion II"),
 "slot_expand_3": ("骰位扩容符 III", "Slot Expansion III"), "slot_expand_4": ("骰位扩容符 IV", "Slot Expansion IV"),
 "slot_expand_5": ("骰位扩容符 V", "Slot Expansion V"),
 "sigil_addition": ("加算之印", "Sigil of Addition"), "sigil_multiplication": ("乘算之印", "Sigil of Multiplication"),
 "sigil_echo": ("回响之印", "Sigil of Echo"), "sigil_echo_2": ("回响之印·双", "Sigil of Echo II"),
 "respec_stone": ("洗命石", "Respec Stone"), "fate_lock_talisman": ("定命符", "Fate Lock Talisman"),
 "incense": ("供奉香", "Incense"), "offering_bone": ("祭品·骰骨", "Offering · Bone"),
 "offering_star": ("祭品·星屑", "Offering · Starshard"), "offering_core": ("祭品·命核", "Offering · Core"),
 "merchant_token": ("骰商凭证", "Merchant Token"), "sovereign_seal": ("命主之印", "Sovereign Seal"),
 "pack_common": ("粗制骰包", "Common Dice Pack"), "pack_fine": ("精制骰包", "Fine Dice Pack"),
 "pack_rare": ("秘银骰包", "Mithril Dice Pack"), "pack_epic": ("命金骰包", "Fate-gold Dice Pack"),
 "pack_fate_minor": ("命运包·微光", "Fate Pack · Minor"), "pack_fate_major": ("命运包·辉耀", "Fate Pack · Major"),
 "pack_ember": ("元素包·火", "Element Pack · Ember"), "pack_frost": ("元素包·冰", "Element Pack · Frost"),
 "pack_storm": ("元素包·雷", "Element Pack · Storm"), "pack_venom": ("元素包·毒", "Element Pack · Venom"),
 "pack_radiant": ("元素包·光", "Element Pack · Radiant"), "pack_umbral": ("元素包·暗", "Element Pack · Umbral"),
 "pack_straight": ("顺子包", "Straight Pack"), "pack_triple": ("豹子包", "Triple Pack"),
 "pack_two_faced": ("双面赌徒包", "Two-Faced Pack"), "pack_null": ("零面包", "Null Pack"),
 "potion_fate_aim": ("命准药剂", "Fate Aim Potion"), "potion_fate_guard": ("命硬药剂", "Fate Guard Potion"),
 "potion_fate_sight": ("命视药剂", "Fate Sight Potion"), "lucky_cookie": ("幸运饼干", "Lucky Cookie"),
 "unidentified_die": ("未鉴定骰", "Unidentified Die"),
 "dice_blade_bone": ("骨骰刃", "Bone Dice Blade"), "dice_blade_iron": ("铁骰刃", "Iron Dice Blade"),
 "dice_blade_mithril": ("秘银骰刃", "Mithril Dice Blade"), "dice_blade_fategold": ("命金骰刃", "Fate-gold Dice Blade"),
 "dice_sling_bone": ("骨骰投索", "Bone Dice Sling"), "dice_sling_mithril": ("秘银投索", "Mithril Dice Sling"),
 "fate_arrow": ("命矢", "Fate Arrow"),
 "fate_shield_bone": ("骨盾", "Bone Shield"), "fate_shield_mithril": ("秘银盾", "Mithril Shield"),
 "fate_shield_fategold": ("命金盾", "Fate-gold Shield"),
 "dicecaster_hood": ("骰术师长帽", "Dicecaster Hood"), "dicecaster_robe": ("骰术师长袍", "Dicecaster Robe"),
 "dicecaster_leggings": ("骰术师护腿", "Dicecaster Leggings"), "dicecaster_boots": ("骰术师之靴", "Dicecaster Boots"),
 "gambler_hood": ("赌命者兜帽", "Gambler Hood"), "gambler_jacket": ("赌命者夹克", "Gambler Jacket"),
 "gambler_pants": ("赌命者长裤", "Gambler Pants"), "gambler_boots": ("赌命者之靴", "Gambler Boots"),
 "dice_pouch": ("骰袋", "Dice Pouch"), "lucky_pendant": ("幸运骰坠", "Lucky Dice Pendant"),
 "fate_mirror": ("命盘镜", "Fate Mirror"), "spirit_bell": ("骰灵铃", "Dice Spirit Bell"),
 "gambler_charm": ("赌徒护符", "Gambler Charm"), "fate_lock_ring": ("定命指环", "Fate Lock Ring"),
 "element_resonator": ("元素共鸣器", "Element Resonator"),
 "smoke_test": ("冒烟测试物品", "Smoke Test"),
}
for did, (zh, en) in misc.items():
    items_zh[f"item.dicefate.{did}"] = zh
    items_en[f"item.dicefate.{did}"] = en

# 刻片 7
for f in faces:
    items_zh[f"item.dicefate.face_plate_d{f}"] = f"骰面刻片·d{f}"
    items_en[f"item.dicefate.face_plate_d{f}"] = f"Face Plate d{f}"

# 方块 8
blocks = {"fate_altar": ("骰运台", "Fate Altar"), "die_engraver": ("刻骰台", "Die Engraver"),
          "appraisal_table": ("鉴定台", "Appraisal Table"), "repair_bench": ("修复台", "Repair Bench"),
          "fate_loom": ("命线织机", "Fate Loom"), "offering_shrine": ("供奉龛", "Offering Shrine"),
          "merchant_stall": ("骰商摊位", "Merchant Stall"), "null_portal": ("无面赌场传送门", "Null Portal")}
for bid, (zh, en) in blocks.items():
    items_zh[f"block.dicefate.{bid}"] = zh
    items_en[f"block.dicefate.{bid}"] = en

# 实体 4
entities = {"two_faced_gambler": ("双面赌徒", "Two-Faced Gambler"), "molten_die_king": ("熔面骰王", "Molten Die King"),
            "frost_silence_queen": ("霜寂女王", "Frost Silence Queen"), "nullface": ("零面者", "Nullface")}
for eid, (zh, en) in entities.items():
    items_zh[f"entity.dicefate.{eid}"] = zh
    items_en[f"entity.dicefate.{eid}"] = en

# 创造标签/键位/HUD/界面
common_extra = {
 "itemGroup.dicefate.dice": ("骰子", "Dice"), "itemGroup.dicefate.materials": ("材料与工具", "Materials & Tools"),
 "itemGroup.dicefate.gear": ("装备与方块", "Gear & Blocks"),
 "key.categories.dicefate": ("骰掷命术", "Dicefate"),
 "key.categories.dicefate.main": ("骰掷命术", "Dicefate"),
 "key.dicefate.cashout": ("兑现", "Cash Out"), "key.dicefate.spirit": ("骰灵开关", "Toggle Dice Spirit"),
 "key.dicefate.board": ("打开命盘", "Open Fate Board"),
 "hud.dicefate.attack": ("攻击", "ATK"), "hud.dicefate.defense": ("防御", "DEF"),
 "hud.dicefate.outcome.crash": ("崩", "MISS"), "hud.dicefate.outcome.lose": ("负", "LOSE"),
 "hud.dicefate.outcome.tie": ("平", "TIE"), "hud.dicefate.outcome.narrow": ("惨胜", "SCRAP"),
 "hud.dicefate.outcome.win": ("胜", "WIN"), "hud.dicefate.outcome.great": ("大胜", "GREAT"),
 "hud.dicefate.outcome.destiny": ("天命", "DESTINY"),
 "hud.dicefate.cashout_ready": ("兑现就绪！", "CASH OUT READY!"),
 "screen.dicefate.board": ("命盘", "Fate Board"), "screen.dicefate.backpack": ("背包（点击骰子装入所选骰位）", "Backpack (click a die to equip)"),
 "death.attack.dicefate.fatebreak": ("%1$s 的命数被斩断了", "%1$s's fate was severed"),
 "entity.villager.dicefate.dice_merchant": ("骰商", "Dice Merchant"),
}
for k, (zh, en) in common_extra.items():
    items_zh[k] = zh
    items_en[k] = en

# tooltip
tips = {
 "tooltip.dicefate.die.face": ("面数：%s", "Faces: %s"),
 "tooltip.dicefate.die.add": ("加算骰：参与总点数比较", "Additive: joins total comparison"),
 "tooltip.dicefate.die.mul": ("乘算骰：胜利后放大收益", "Multiplicative: boosts after victory"),
 "tooltip.dicefate.die.skill": ("技能骰：触发条件满足时自动生效", "Skill die: triggers automatically"),
 "tooltip.dicefate.die.unique": ("唯一骰（命盘限 1）", "Unique die (limit 1 per board)"),
 "tooltip.dicefate.die.cursed": ("诅咒骰：力量与代价同在", "Cursed die: power at a price"),
 "tooltip.dicefate.die.wear": ("磨损：%s / %s", "Wear: %s / %s"),
 "tooltip.dicefate.die.common": ("凡品", "Common"), "tooltip.dicefate.die.uncommon": ("良品", "Uncommon"),
 "tooltip.dicefate.die.rare": ("珍品", "Rare"), "tooltip.dicefate.die.epic": ("史诗", "Epic"),
 "tooltip.dicefate.die.legendary": ("传说", "Legendary"), "tooltip.dicefate.die.fated": ("命定", "Fated"),
 "tooltip.dicefate.die.element.ember": ("元素：烬火", "Element: Ember"),
 "tooltip.dicefate.die.element.frost": ("元素：霜冰", "Element: Frost"),
 "tooltip.dicefate.die.element.storm": ("元素：雷暴", "Element: Storm"),
 "tooltip.dicefate.die.element.venom": ("元素：蚀毒", "Element: Venom"),
 "tooltip.dicefate.die.element.radiant": ("元素：辉光", "Element: Radiant"),
 "tooltip.dicefate.die.element.umbral": ("元素：暗影", "Element: Umbral"),
 "tooltip.dicefate.rune.edge": ("骰纹·锋：胜利时命势伤害 +1", "Rune · Edge: +1 poise damage on win"),
 "tooltip.dicefate.rune.wall": ("骰纹·壁：防御时本骰 +1", "Rune · Wall: +1 when defending"),
 "tooltip.dicefate.rune.steady": ("骰纹·稳：掷出 1 视为 2", "Rune · Steady: 1 counts as 2"),
 "tooltip.dicefate.rune.fierce": ("骰纹·烈：大胜时伤害 +5%", "Rune · Fierce: +5% damage on great win"),
 "tooltip.dicefate.rune.tough": ("骰纹·韧：护甲 +1", "Rune · Tough: +1 armor"),
 "tooltip.dicefate.rune.swift": ("骰纹·迅：攻击冷却 -3%", "Rune · Swift: -3% attack cooldown"),
 "tooltip.dicefate.rune.greedy": ("骰纹·贪：点数 +1，命势 -1", "Rune · Greedy: +1 pip, -1 poise"),
 "tooltip.dicefate.rune.cursed": ("骰纹·咒：点数 +2，8% 裂面", "Rune · Cursed: +2 pips, 8% crack"),
 "tooltip.dicefate.pack": ("骰子包：临时替换战斗骰池", "Dice pack: temporarily replaces pool"),
 "tooltip.dicefate.unidentified": ("右键鉴定（10 命砂）", "Right-click to appraise (10 fate sand)"),
 "tooltip.dicefate.item.aim": ("命准：3 次拼点总点数 +1", "Fate Aim: +1 total for 3 contests"),
 "tooltip.dicefate.item.guard": ("命硬：3 次拼点减伤 +10%", "Fate Guard: -10% damage for 3 contests"),
 "tooltip.dicefate.item.sight": ("命视：3 分钟揭示 32 格内敌人", "Fate Sight: reveal enemies 3 min"),
 "tooltip.dicefate.item.cookie": ("下一个骰包 +1 次使用", "Next dice pack +1 use"),
 "tooltip.dicefate.item.sigil_add": ("使用：解锁加算槽", "Use: unlock additive slot"),
 "tooltip.dicefate.item.sigil_mul": ("使用：解锁乘算槽", "Use: unlock multiplicative slot"),
 "tooltip.dicefate.item.sigil_skill1": ("使用：解锁技能槽 1", "Use: unlock skill slot 1"),
 "tooltip.dicefate.item.sigil_skill2": ("使用：解锁技能槽 2", "Use: unlock skill slot 2"),
 "tooltip.dicefate.item.lock_talisman": ("使用：解锁定命能力", "Use: unlock Fate Lock"),
 "tooltip.dicefate.item.respec": ("使用：重置命途与精通", "Use: reset fate rune & mastery"),
}
for k, (zh, en) in tips.items():
    items_zh[k] = zh
    items_en[k] = en

# 消息
msgs = {
 "message.dicefate.expand_used": ("该级扩容符已用过", "This expansion was already used"),
 "message.dicefate.expand_success": ("骰位扩容！当前基础骰位：%s", "Expanded! Base slots: %s"),
 "message.dicefate.expand_full": ("骰位已达上限（6）", "Slot cap reached (6)"),
 "message.dicefate.use_fail": ("现在无法使用", "Cannot use now"),
 "message.dicefate.pack_used": ("骰包生效，剩余 %s 次拼点", "Pack active, %s contests left"),
 "message.dicefate.pack_conflict": ("骰包与定命互斥", "Pack conflicts with Fate Lock"),
 "message.dicefate.locked_die": ("定命中的骰不可卸下", "Locked die cannot be removed"),
 "message.dicefate.no_last_roll": ("该骰没有上一次掷值", "No last roll on this die"),
 "message.dicefate.duplicate_limit": ("同名骰已达上限", "Duplicate limit reached"),
 "message.dicefate.stance_cd": ("姿态切换冷却中", "Stance on cooldown"),
 "message.dicefate.preset_saved": ("预设 %s 已保存", "Preset %s saved"),
 "message.dicefate.preset_loaded": ("预设 %s 已装载", "Preset %s loaded"),
 "message.dicefate.cashout_fail": ("骰势未满，无法兑现", "Momentum not full"),
 "message.dicefate.need_sand": ("命砂不足", "Not enough fate sand"),
 "message.dicefate.need_dust": ("命尘不足", "Not enough fate dust"),
 "message.dicefate.need_blank": ("手持骰胚", "Hold a blank die"),
 "message.dicefate.need_plate": ("背包内没有骰面刻片", "No face plate in inventory"),
 "message.dicefate.die_forged": ("铸造了 %s", "Forged %s"),
 "message.dicefate.repaired": ("修复完成", "Repaired"),
 "message.dicefate.rune_applied": ("施加骰纹：%s", "Rune applied: %s"),
 "message.dicefate.fate_rune_applied": ("命途已改写", "Fate rune rewritten"),
 "message.dicefate.shrine_blessed": ("供奉得到回应", "Offering accepted"),
 "message.dicefate.appraised": ("鉴定结果：%s", "Appraised: %s"),
}
for k, (zh, en) in msgs.items():
    items_zh[k] = zh
    items_en[k] = en

w('assets/dicefate/lang/zh_cn.json', items_zh)
w('assets/dicefate/lang/en_us.json', items_en)
print('lang keys:', len(items_zh))
