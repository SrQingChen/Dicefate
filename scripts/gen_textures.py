# -*- coding: utf-8 -*-
"""
生成全套 32×32 主题贴图（暗金 #D9A441 / 玉髓白 #E8E4D8 / 青绿 #6FB3A8）。
- 骰子物品图标（83）：多面体剪影 + 面数徽记
- 材料/工具/包/装备（97）
- 方块贴图（8）
- 进度背景、Boss 占位贴图
"""
import os, math, random
from PIL import Image, ImageDraw, ImageFilter

TEX = 'src/main/resources/assets/dicefate/textures'
os.makedirs(f'{TEX}/item', exist_ok=True)
os.makedirs(f'{TEX}/block', exist_ok=True)
os.makedirs(f'{TEX}/gui', exist_ok=True)
os.makedirs(f'{TEX}/entity', exist_ok=True)
os.makedirs(f'{TEX}/entity/equipment/humanoid', exist_ok=True)
os.makedirs(f'{TEX}/equipment/humanoid', exist_ok=True)

S = 32
GOLD = (217, 164, 65)
GOLD_D = (160, 114, 32)
GOLD_L = (240, 208, 120)
WHITE = (232, 228, 216)
TEAL = (111, 179, 168)
TEAL_D = (62, 122, 114)
INK = (26, 22, 30)
INK_L = (44, 38, 52)
RED = (183, 74, 58)

ELEMENT_COLORS = {
    "ember": (196, 90, 40), "frost": (110, 160, 210), "storm": (200, 180, 70),
    "venom": (120, 160, 60), "radiant": (235, 215, 120), "umbral": (110, 80, 150)}

def base(bg=INK):
    img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    return img, d

def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)

def poly_shadow(img, points, fill, outline=None):
    d = ImageDraw.Draw(img)
    d.polygon(points, fill=fill, outline=outline)

def digit(d, x, y, num, c, scale=1):
    """3x5 微型数字字体。"""
    FONT = {
        '0': ["111","101","101","101","111"], '1': ["010","110","010","010","111"],
        '2': ["111","001","111","100","111"], '3': ["111","001","111","001","111"],
        '4': ["101","101","111","001","001"], '5': ["111","100","111","001","111"],
        '6': ["111","100","111","101","111"], '7': ["111","001","010","010","010"],
        '8': ["111","101","111","101","111"], '9': ["111","101","111","001","111"]}
    for i, ch in enumerate(str(num)):
        glyph = FONT.get(ch, FONT['0'])
        for gy, row in enumerate(glyph):
            for gx, px in enumerate(row):
                if px == '1':
                    d.rectangle([x + i * 4 * scale + gx * scale, y + gy * scale,
                                 x + i * 4 * scale + gx * scale + scale - 1, y + gy * scale + scale - 1], fill=c)

# ================= 骰子物品图标：按面数画多面体剪影 =================
def dice_icon(faces, mat_color, accent=None, badge=None):
    img, d = base()
    c = mat_color
    ac = accent or WHITE
    cx, cy = 16, 17
    if faces == 4:      # 四面体
        pts = [(16, 4), (4, 26), (28, 26)]
        poly_shadow(img, pts, shade(c, 1.0), shade(c, 0.5))
        poly_shadow(img, [(16, 4), (16, 26), (28, 26)], shade(c, 0.75))
        poly_shadow(img, [(16, 4), (4, 26), (16, 26)], shade(c, 0.6))
    elif faces == 6:    # 立方体（等距）
        poly_shadow(img, [(7, 9), (16, 5), (25, 9), (25, 22), (16, 27), (7, 22)], shade(c, 0.85), shade(GOLD_D, 1))
        poly_shadow(img, [(16, 5), (25, 9), (27, 6), (18, 2)], shade(c, 1.15))
        poly_shadow(img, [(25, 9), (27, 6), (27, 19), (25, 22)], shade(c, 0.55))
        d.rectangle([10, 12, 13, 15], fill=INK)   # 正面点数窗
        d.rectangle([11, 13, 12, 14], fill=ac)
    elif faces == 8:    # 八面体
        poly_shadow(img, [(16, 3), (27, 16), (16, 29), (5, 16)], shade(c, 0.9), shade(GOLD_D, 1))
        poly_shadow(img, [(16, 3), (27, 16), (16, 16)], shade(c, 1.1))
        poly_shadow(img, [(16, 3), (16, 16), (5, 16)], shade(c, 0.7))
        poly_shadow(img, [(5, 16), (16, 16), (16, 29)], shade(c, 0.6))
        poly_shadow(img, [(16, 16), (27, 16), (16, 29)], shade(c, 0.45))
    elif faces == 10:   # 十面体（风筝面近似）
        poly_shadow(img, [(16, 3), (27, 13), (23, 26), (9, 26), (5, 13)], shade(c, 0.9), shade(GOLD_D, 1))
        poly_shadow(img, [(16, 3), (27, 13), (16, 15)], shade(c, 1.1))
        poly_shadow(img, [(16, 3), (16, 15), (5, 13)], shade(c, 0.7))
        poly_shadow(img, [(5, 13), (16, 15), (9, 26)], shade(c, 0.6))
        poly_shadow(img, [(16, 15), (27, 13), (23, 26)], shade(c, 0.5))
        poly_shadow(img, [(9, 26), (16, 15), (23, 26)], shade(c, 0.42))
    elif faces == 12:   # 十二面体（五边形房）
        poly_shadow(img, [(16, 3), (27, 11), (23, 25), (9, 25), (5, 11)], shade(c, 0.95), shade(GOLD_D, 1))
        poly_shadow(img, [(16, 3), (27, 11), (18, 13), (14, 13)], shade(c, 1.15))
        poly_shadow(img, [(5, 11), (14, 13), (9, 25)], shade(c, 0.62))
        poly_shadow(img, [(27, 11), (23, 25), (18, 13)], shade(c, 0.5))
        poly_shadow(img, [(9, 25), (14, 13), (18, 13), (23, 25)], shade(c, 0.4))
        poly_shadow(img, [(16, 3), (14, 13), (5, 11)], shade(c, 0.75))
    elif faces == 20:   # 二十面体（三角拼面）
        poly_shadow(img, [(16, 2), (28, 12), (24, 27), (8, 27), (4, 12)], shade(c, 0.95), shade(GOLD_D, 1))
        poly_shadow(img, [(16, 2), (28, 12), (21, 12), (16, 10)], shade(c, 1.15))
        poly_shadow(img, [(16, 2), (16, 10), (11, 12), (4, 12)], shade(c, 0.8))
        poly_shadow(img, [(4, 12), (11, 12), (10, 21), (8, 27)], shade(c, 0.6))
        poly_shadow(img, [(28, 12), (24, 27), (21, 20), (21, 12)], shade(c, 0.45))
        poly_shadow(img, [(8, 27), (10, 21), (16, 24), (22, 21), (24, 27)], shade(c, 0.35))
        poly_shadow(img, [(11, 12), (16, 10), (21, 12), (16, 16)], shade(c, 1.05))
        poly_shadow(img, [(11, 12), (16, 16), (10, 21)], shade(c, 0.7))
        poly_shadow(img, [(21, 12), (21, 20), (16, 16)], shade(c, 0.55))
        poly_shadow(img, [(16, 16), (21, 20), (16, 24), (10, 21)], shade(c, 0.5))
    elif faces == 24:   # 二十四面体（星形六边形）
        pts = []
        for i in range(12):
            ang = math.pi / 2 + i * math.pi / 6
            r = 13 if i % 2 == 0 else 8
            pts.append((16 + r * math.cos(ang), 16 + r * math.sin(ang)))
        poly_shadow(img, pts, shade(c, 0.9), shade(GOLD_D, 1))
        poly_shadow(img, [pts[i] for i in [0, 1, 11]], shade(c, 1.15))
        poly_shadow(img, [pts[i] for i in [3, 4, 5]], shade(c, 0.6))
        poly_shadow(img, [pts[i] for i in [6, 7, 8]], shade(c, 0.5))
    # 面数徽记
    digit(ImageDraw.Draw(img), 11, 24, faces, shade(GOLD_L, 1.05))
    # 徽记（元素/技能角标）
    if badge:
        d2 = ImageDraw.Draw(img)
        d2.ellipse([24, 22, 31, 29], fill=badge, outline=INK)
    return img

MAT_COLORS = {"bone": (216, 208, 188), "iron": (188, 192, 200), "mithril": (150, 195, 205),
              "fategold": GOLD, "starlight": (235, 235, 245)}

mats_faces = [("bone",4),("bone",6),("bone",8),("iron",6),("iron",8),("iron",10),
    ("mithril",8),("mithril",10),("mithril",12),("fategold",10),("fategold",12),("fategold",20),
    ("starlight",12),("starlight",20),("starlight",24)]
for m, f in mats_faces:
    dice_icon(f, MAT_COLORS[m]).save(f'{TEX}/item/die_{m}_d{f}.png')

for e, col in ELEMENT_COLORS.items():
    for suf, faces in [("spark", 8), ("resonant", 12), ("origin", 20)]:
        img = dice_icon(faces, col, accent=WHITE, badge=GOLD)
        img.save(f'{TEX}/item/die_{e}_{suf}.png')

# 技能骰：同形状+紫青色调
skill_cols = {"echo": TEAL, "pursuit": (200, 120, 60), "ward": (100, 140, 200),
              "breaker": (170, 80, 90), "reverse": (150, 110, 190), "greed": (190, 160, 60),
              "lock": (120, 130, 150), "favor": (210, 190, 120)}
for fam, col in skill_cols.items():
    for t, faces in [(1, 6), (2, 12), (3, 20)]:
        img = dice_icon(faces, col, accent=WHITE, badge=GOLD)
        d = ImageDraw.Draw(img)
        d.rectangle([0, 0, 31, 2], fill=shade(GOLD, 0.9 if t > 1 else 0.6))
        if t == 3:
            d.rectangle([0, 29, 31, 31], fill=shade(GOLD, 0.9))
        img.save(f'{TEX}/item/skill_die_{fam}_{t}.png')

# 加算（青绿+）/乘算（暗金×）
add_cols = {"edge": (200, 150, 90), "wall": (140, 160, 190), "poise": (170, 100, 120),
            "element": TEAL, "combo": (190, 170, 90), "mend": (150, 190, 140)}
for k, c in add_cols.items():
    img = dice_icon(6, c, accent=WHITE)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 8, 8], fill=TEAL_D, outline=TEAL)
    img.save(f'{TEX}/item/add_die_{k}.png')
mul_cols = {"fierce": (200, 100, 60), "bulwark": (110, 140, 190), "pierce": (190, 170, 90),
            "break": (180, 120, 150), "resonance": TEAL, "fate": GOLD}
for k, c in mul_cols.items():
    img = dice_icon(8, c, accent=WHITE)
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 8, 8], fill=GOLD_D, outline=GOLD)
    img.save(f'{TEX}/item/mul_die_{k}.png')

# 唯一骰
uniq_specs = {"cracked_face": (6, WHITE, RED), "two_faced": (8, (190, 170, 200), None),
              "molten_core": (12, ELEMENT_COLORS['ember'], GOLD), "frost_silence": (12, ELEMENT_COLORS['frost'], WHITE),
              "storm_verdict": (12, ELEMENT_COLORS['storm'], WHITE), "bone_eater": (10, (210, 200, 170), None),
              "radiant_oath": (10, ELEMENT_COLORS['radiant'], WHITE), "umbral_veil": (10, ELEMENT_COLORS['umbral'], None),
              "null_face": (6, INK_L, WHITE), "fate_sovereign": (20, GOLD, WHITE)}
for u, (f, c, ac) in uniq_specs.items():
    img = dice_icon(f, c, accent=ac or GOLD_L, badge=GOLD)
    if u == 'cracked_face':
        d = ImageDraw.Draw(img)
        d.line([6, 6, 14, 20, 10, 28], fill=RED, width=1)
    if u == 'null_face':
        d = ImageDraw.Draw(img)
        d.text((13, 13), "0", fill=WHITE)
    img.save(f'{TEX}/item/unique_die_{u}.png')

# 诅咒骰（暗紫+裂纹）
for c_name, f in [("greed", 12), ("blood", 10), ("fracture", 20), ("misfortune", 8)]:
    img = dice_icon(f, (110, 70, 120), accent=RED, badge=RED)
    d = ImageDraw.Draw(img)
    d.line([8, 8, 12, 16, 9, 24], fill=(90, 50, 100), width=1)
    d.line([20, 6, 23, 14, 19, 20], fill=(90, 50, 100), width=1)
    img.save(f'{TEX}/item/cursed_die_{c_name}.png')

print('dice icons done:', 83)

# ================= 材料图标 =================
def simple_icon(draw_fn):
    img, d = base()
    draw_fn(img, d)
    return img

def sand_icon(img, d):
    for i in range(18):
        x = random.Random(i).randint(6, 26)
        y = random.Random(i + 50).randint(8, 24)
        c = [GOLD, GOLD_L, GOLD_D][i % 3]
        d.ellipse([x, y, x + 2, y + 2], fill=c)
    d.ellipse([12, 12, 20, 20], outline=GOLD_L)
simple_icon(sand_icon).save(f'{TEX}/item/fate_sand.png')

def dust_icon(img, d):
    for i in range(26):
        x = random.Random(i * 3).randint(5, 27)
        y = random.Random(i * 7 + 1).randint(5, 27)
        d.point((x, y), fill=[WHITE, GOLD_L, TEAL][i % 3])
    d.ellipse([10, 10, 22, 22], outline=TEAL)
simple_icon(dust_icon).save(f'{TEX}/item/fate_dust.png')

def core_icon(img, d, c=TEAL):
    d.polygon([(16, 3), (27, 10), (27, 22), (16, 29), (5, 22), (5, 10)], fill=shade(c, 0.45), outline=c)
    d.polygon([(16, 7), (23, 12), (23, 20), (16, 25), (9, 20), (9, 12)], fill=shade(c, 0.8), outline=shade(WHITE, 0.9))
    d.ellipse([13, 13, 19, 19], fill=WHITE)
simple_icon(lambda i, d: core_icon(i, d, TEAL)).save(f'{TEX}/item/fate_core.png')
simple_icon(lambda i, d: core_icon(i, d, (150, 120, 160))).save(f'{TEX}/item/cracked_core.png')
simple_icon(lambda i, d: core_icon(i, d, GOLD)).save(f'{TEX}/item/sovereign_core.png')

def shard_icon(img, d, c):
    d.polygon([(16, 2), (22, 12), (19, 29), (12, 26), (9, 10)], fill=shade(c, 0.85), outline=shade(c, 1.1))
    d.line([16, 5, 15, 26], fill=shade(WHITE, 0.85))
simple_icon(lambda i, d: shard_icon(i, d, (235, 235, 245))).save(f'{TEX}/item/starshard.png')
simple_icon(lambda i, d: shard_icon(i, d, (210, 120, 200))).save(f'{TEX}/item/fate_fragment.png')
simple_icon(lambda i, d: shard_icon(i, d, TEAL)).save(f'{TEX}/item/mirror_shard.png')
simple_icon(lambda i, d: shard_icon(i, d, (200, 180, 120))).save(f'{TEX}/item/slot_shard.png')

for e, c in ELEMENT_COLORS.items():
    simple_icon(lambda i, d, cc=c: (
        d.ellipse([6, 6, 26, 26], fill=shade(cc, 0.5), outline=cc),
        d.ellipse([11, 11, 21, 21], fill=shade(cc, 0.9)),
        d.point((16, 16), fill=WHITE))).save(f'{TEX}/item/element_essence_{e}.png')

# 骰胚（未刻面的立方/坯料）
for m, c in MAT_COLORS.items():
    def blank_fn(img, d, cc=c):
        d.polygon([(8, 10), (16, 6), (24, 10), (24, 21), (16, 26), (8, 21)], fill=shade(cc, 0.55), outline=GOLD_D)
        d.polygon([(16, 6), (24, 10), (26, 8), (18, 3)], fill=shade(cc, 0.8))
        d.polygon([(24, 10), (26, 8), (26, 19), (24, 21)], fill=shade(cc, 0.35))
        d.text((13, 13), "?", fill=INK)
    simple_icon(blank_fn).save(f'{TEX}/item/blank_die_{m}.png')

# 刻片
for f in [4, 6, 8, 10, 12, 20, 24]:
    def plate_fn(img, d, ff=f):
        d.polygon([(8, 4), (24, 4), (28, 12), (24, 28), (8, 28), (4, 12)], fill=shade(TEAL, 0.4), outline=GOLD)
        digit(d, 11, 13, ff, WHITE)
    simple_icon(plate_fn).save(f'{TEX}/item/face_plate_d{f}.png')

print('material icons done')

# ================= 工具/消耗品 =================
def flask_icon(c, liquid):
    def fn(img, d):
        d.rectangle([13, 4, 19, 8], fill=shade(WHITE, 0.9))
        d.polygon([(11, 8), (21, 8), (25, 22), (21, 27), (11, 27), (7, 22)], fill=shade(c, 0.35), outline=WHITE)
        d.polygon([(10, 18), (22, 18), (24, 22), (21, 26), (11, 26), (8, 22)], fill=liquid)
    return fn
simple_icon(flask_icon((150, 190, 200), GOLD)).save(f'{TEX}/item/potion_fate_aim.png')
simple_icon(flask_icon((150, 190, 200), TEAL)).save(f'{TEX}/item/potion_fate_guard.png')
simple_icon(flask_icon((150, 190, 200), (235, 220, 120))).save(f'{TEX}/item/potion_fate_sight.png')

def cookie_icon(img, d):
    d.ellipse([5, 5, 27, 27], fill=(200, 160, 90), outline=(160, 120, 60))
    for x, y in [(10, 10), (18, 12), (12, 19), (21, 20)]:
        d.ellipse([x, y, x + 3, y + 3], fill=(90, 60, 40))
simple_icon(cookie_icon).save(f'{TEX}/item/lucky_cookie.png')

def pack_icon(c, mark):
    def fn(img, d):
        d.rectangle([6, 10, 26, 28], fill=shade(c, 0.5), outline=GOLD)
        d.rectangle([6, 6, 26, 10], fill=shade(c, 0.8), outline=GOLD)
        d.line([16, 6, 16, 28], fill=shade(GOLD_D, 1))
        d.text((13, 16), mark, fill=WHITE)
    return fn
packs_marks = {"pack_common": ((120, 110, 100), "C"), "pack_fine": ((140, 150, 160), "F"),
    "pack_rare": (TEAL_D, "M"), "pack_epic": (GOLD_D, "G"),
    "pack_fate_minor": ((150, 120, 60), "☆"), "pack_fate_major": (GOLD_D, "★"),
    "pack_straight": ((100, 140, 120), "S"), "pack_triple": ((140, 100, 110), "T"),
    "pack_two_faced": ((120, 100, 150), "2"), "pack_null": (INK_L, "0")}
for pid, (c, m) in packs_marks.items():
    simple_icon(pack_icon(c, m)).save(f'{TEX}/item/{pid}.png')
for e in ELEMENT_COLORS:
    simple_icon(pack_icon(ELEMENT_COLORS[e], "E")).save(f'{TEX}/item/pack_{e}.png')

def tool_icon(kind):
    def fn(img, d):
        if kind == 'file':
            d.rectangle([13, 4, 19, 26], fill=(170, 140, 100), outline=GOLD_D)
            d.line([10, 8, 22, 8], fill=TEAL); d.line([10, 14, 22, 14], fill=TEAL); d.line([10, 20, 22, 20], fill=TEAL)
        elif kind == 'glue':
            d.polygon([(10, 6), (22, 6), (24, 24), (8, 24)], fill=(220, 210, 190), outline=GOLD)
            d.text((13, 12), "G", fill=TEAL_D)
        elif kind == 'oil':
            d.rectangle([12, 4, 20, 9], fill=WHITE)
            d.polygon([(9, 9), (23, 9), (26, 25), (6, 25)], fill=(250, 230, 160), outline=GOLD)
        elif kind == 'feed':
            d.ellipse([6, 10, 26, 26], fill=(210, 180, 100), outline=(160, 130, 60))
            d.polygon([(10, 12), (16, 4), (22, 12)], fill=TEAL)
        elif kind == 'ink':
            d.rectangle([9, 6, 23, 24], fill=INK_L, outline=GOLD)
            d.ellipse([13, 12, 19, 18], fill=TEAL)
        elif kind == 'lens':
            d.ellipse([6, 6, 26, 26], outline=GOLD, width=2)
            d.ellipse([10, 10, 18, 18], fill=shade(TEAL, 0.4))
        elif kind == 'scroll':
            d.rectangle([8, 6, 24, 26], fill=(230, 220, 190), outline=(170, 150, 110))
            d.line([11, 11, 21, 11], fill=TEAL_D); d.line([11, 15, 21, 15], fill=TEAL_D); d.line([11, 19, 21, 19], fill=TEAL_D)
    return fn
simple_icon(tool_icon('file')).save(f'{TEX}/item/repair_file.png')
simple_icon(tool_icon('glue')).save(f'{TEX}/item/fate_glue.png')
simple_icon(tool_icon('oil')).save(f'{TEX}/item/perfect_oil.png')
simple_icon(tool_icon('feed')).save(f'{TEX}/item/spirit_feed.png')
simple_icon(tool_icon('ink')).save(f'{TEX}/item/rune_ink.png')
simple_icon(tool_icon('lens')).save(f'{TEX}/item/appraisal_lens.png')
simple_icon(tool_icon('scroll')).save(f'{TEX}/item/appraisal_scroll.png')

def scrolllike(letter, c):
    def fn(img, d):
        d.rectangle([7, 5, 25, 27], fill=shade(c, 0.3), outline=GOLD)
        digit(d, 14, 13, letter if isinstance(letter, int) else 0, WHITE)
        if not isinstance(letter, int):
            d.text((12, 12), letter, fill=WHITE)
    return fn
for i in range(1, 6):
    def talisman(img, d, i=i):
        d.polygon([(16, 3), (25, 8), (25, 24), (16, 29), (7, 24), (7, 8)], fill=shade(GOLD_D, 0.6), outline=GOLD)
        digit(d, 13, 13, i, WHITE)
    simple_icon(talisman).save(f'{TEX}/item/slot_expand_{i}.png')

def sigil_icon(c, mark):
    def fn(img, d):
        d.polygon([(16, 3), (27, 10), (23, 26), (9, 26), (5, 10)], fill=shade(c, 0.4), outline=GOLD)
        d.text((11, 13), mark, fill=WHITE)
    return fn
simple_icon(sigil_icon(TEAL_D, "+")).save(f'{TEX}/item/sigil_addition.png')
simple_icon(sigil_icon(GOLD_D, "×")).save(f'{TEX}/item/sigil_multiplication.png')
simple_icon(sigil_icon((120, 130, 160), "E")).save(f'{TEX}/item/sigil_echo.png')
simple_icon(sigil_icon((150, 120, 180), "E2")).save(f'{TEX}/item/sigil_echo_2.png')
simple_icon(sigil_icon((150, 100, 110), "R")).save(f'{TEX}/item/respec_stone.png')
simple_icon(sigil_icon((180, 150, 200), "L")).save(f'{TEX}/item/fate_lock_talisman.png')
simple_icon(sigil_icon((120, 110, 90), "$")).save(f'{TEX}/item/merchant_token.png')

def offering_icon(c):
    def fn(img, d):
        d.polygon([(16, 4), (26, 12), (22, 26), (10, 26), (6, 12)], fill=shade(c, 0.5), outline=GOLD)
        d.ellipse([13, 14, 19, 20], fill=WHITE)
    return fn
simple_icon(offering_icon((210, 200, 180))).save(f'{TEX}/item/offering_bone.png')
simple_icon(offering_icon((230, 235, 250))).save(f'{TEX}/item/offering_star.png')
simple_icon(offering_icon(TEAL)).save(f'{TEX}/item/offering_core.png')

def incense_icon(img, d):
    d.line([16, 28, 16, 12], fill=(160, 120, 70))
    d.line([16, 12, 16, 6], fill=(220, 120, 80))
    d.ellipse([14, 3, 18, 7], outline=(200, 200, 200))
simple_icon(incense_icon).save(f'{TEX}/item/incense.png')

def seal_icon(img, d):
    d.polygon([(16, 2), (29, 16), (16, 30), (3, 16)], fill=shade(GOLD_D, 0.8), outline=GOLD)
    d.polygon([(16, 8), (23, 16), (16, 24), (9, 16)], fill=INK_L, outline=GOLD_L)
simple_icon(seal_icon).save(f'{TEX}/item/sovereign_seal.png')

def uid_icon(img, d):
    d.polygon([(8, 10), (16, 5), (24, 10), (24, 21), (16, 27), (8, 21)], fill=shade(TEAL_D, 0.6), outline=GOLD)
    d.text((12, 13), "?", fill=WHITE)
simple_icon(uid_icon).save(f'{TEX}/item/unidentified_die.png')

# ================= 武器/盾/护甲/饰品 =================
def blade_icon(c):
    def fn(img, d):
        d.line([16, 3, 16, 20], fill=shade(c, 1.1), width=3)
        d.line([15, 3, 15, 20], fill=shade(c, 0.8), width=1)
        d.line([16, 3, 20, 8], fill=shade(c, 1.0), width=2)
        d.rectangle([10, 20, 22, 22], fill=GOLD_D)
        d.rectangle([14, 22, 18, 28], fill=(120, 90, 50))
        d.ellipse([14, 27, 18, 29], fill=GOLD)
    return fn
simple_icon(blade_icon((200, 195, 180))).save(f'{TEX}/item/dice_blade_bone.png')
simple_icon(blade_icon((190, 195, 205))).save(f'{TEX}/item/dice_blade_iron.png')
simple_icon(blade_icon((150, 200, 210))).save(f'{TEX}/item/dice_blade_mithril.png')
simple_icon(blade_icon(GOLD)).save(f'{TEX}/item/dice_blade_fategold.png')

def sling_icon(c):
    def fn(img, d):
        d.polygon([(6, 6), (12, 8), (8, 14)], fill=(140, 100, 60))
        d.arc([8, 10, 26, 28], 200, 340, fill=shade(c, 0.9), width=2)
        d.ellipse([20, 6, 26, 12], fill=GOLD, outline=GOLD_D)
    return fn
simple_icon(sling_icon((180, 170, 150))).save(f'{TEX}/item/dice_sling_bone.png')
simple_icon(sling_icon((150, 200, 210))).save(f'{TEX}/item/dice_sling_mithril.png')

def arrow_icon(img, d):
    d.line([16, 6, 16, 26], fill=(150, 120, 80), width=2)
    d.polygon([(16, 2), (12, 9), (20, 9)], fill=GOLD)
    d.polygon([(16, 26), (11, 29), (16, 22)], fill=TEAL)
    d.polygon([(16, 26), (21, 29), (16, 22)], fill=TEAL)
simple_icon(arrow_icon).save(f'{TEX}/item/fate_arrow.png')

def shield_icon(c):
    def fn(img, d):
        d.polygon([(16, 3), (27, 8), (27, 18), (16, 29), (5, 18), (5, 8)], fill=shade(c, 0.6), outline=GOLD)
        d.polygon([(16, 7), (23, 10), (23, 17), (16, 24), (9, 17), (9, 10)], fill=shade(c, 0.85))
        digit(d, 14, 13, 2, WHITE)
    return fn
simple_icon(shield_icon((210, 200, 180))).save(f'{TEX}/item/fate_shield_bone.png')
simple_icon(shield_icon((150, 200, 210))).save(f'{TEX}/item/fate_shield_mithril.png')
simple_icon(shield_icon(GOLD)).save(f'{TEX}/item/fate_shield_fategold.png')

def armor_icon(kind, c):
    def fn(img, d):
        if kind == 'hood':
            d.polygon([(8, 26), (8, 14), (16, 5), (24, 14), (24, 26)], fill=shade(c, 0.6), outline=GOLD_D)
            d.ellipse([11, 13, 21, 21], fill=INK)
        elif kind == 'robe':
            d.polygon([(10, 5), (22, 5), (26, 27), (6, 27)], fill=shade(c, 0.6), outline=GOLD_D)
            d.line([16, 5, 16, 27], fill=shade(GOLD_D, 0.9))
        elif kind == 'leggings':
            d.rectangle([10, 5, 22, 12], fill=shade(c, 0.6), outline=GOLD_D)
            d.rectangle([10, 12, 15, 27], fill=shade(c, 0.5), outline=GOLD_D)
            d.rectangle([17, 12, 22, 27], fill=shade(c, 0.5), outline=GOLD_D)
        elif kind == 'boots':
            d.rectangle([8, 12, 20, 20], fill=shade(c, 0.6), outline=GOLD_D)
            d.rectangle([8, 20, 24, 26], fill=shade(c, 0.5), outline=GOLD_D)
    return fn
dc = (90, 110, 130); gb = (130, 100, 70)
simple_icon(armor_icon('hood', dc)).save(f'{TEX}/item/dicecaster_hood.png')
simple_icon(armor_icon('robe', dc)).save(f'{TEX}/item/dicecaster_robe.png')
simple_icon(armor_icon('leggings', dc)).save(f'{TEX}/item/dicecaster_leggings.png')
simple_icon(armor_icon('boots', dc)).save(f'{TEX}/item/dicecaster_boots.png')
simple_icon(armor_icon('hood', gb)).save(f'{TEX}/item/gambler_hood.png')
simple_icon(armor_icon('robe', gb)).save(f'{TEX}/item/gambler_jacket.png')
simple_icon(armor_icon('leggings', gb)).save(f'{TEX}/item/gambler_pants.png')
simple_icon(armor_icon('boots', gb)).save(f'{TEX}/item/gambler_boots.png')

def curio_icon(c, mark):
    def fn(img, d):
        d.ellipse([6, 8, 26, 28], fill=shade(c, 0.5), outline=GOLD)
        d.ellipse([12, 14, 20, 22], fill=shade(c, 0.9), outline=WHITE)
        d.text((13, 16), mark, fill=INK)
    return fn
simple_icon(curio_icon((140, 120, 90), "P")).save(f'{TEX}/item/dice_pouch.png')
simple_icon(curio_icon(GOLD, "L")).save(f'{TEX}/item/lucky_pendant.png')
simple_icon(curio_icon(TEAL_D, "M")).save(f'{TEX}/item/fate_mirror.png')
simple_icon(curio_icon((150, 130, 190), "S")).save(f'{TEX}/item/spirit_bell.png')
simple_icon(curio_icon((190, 90, 80), "G")).save(f'{TEX}/item/gambler_charm.png')
simple_icon(curio_icon((150, 170, 200), "F")).save(f'{TEX}/item/fate_lock_ring.png')
simple_icon(curio_icon(TEAL, "R")).save(f'{TEX}/item/element_resonator.png')

# smoke test
def smoke_icon(img, d):
    d.rectangle([4, 4, 28, 28], fill=(80, 200, 120), outline=INK)
simple_icon(smoke_icon).save(f'{TEX}/item/smoke_test.png')

print('tools/gear icons done')

# ================= 方块贴图（32×32） =================
def block_tex(base_c, pattern='panel', accent=GOLD):
    img = Image.new('RGBA', (S, S), shade(base_c, 0.55))
    d = ImageDraw.Draw(img)
    # 边框
    d.rectangle([0, 0, 31, 31], outline=shade(base_c, 0.3))
    d.rectangle([1, 1, 30, 30], outline=accent)
    rng = random.Random(42)
    for i in range(40):
        x, y = rng.randint(2, 29), rng.randint(2, 29)
        d.point((x, y), fill=shade(base_c, 0.4 + (i % 4) * 0.12))
    if pattern == 'panel':
        d.polygon([(10, 8), (22, 8), (26, 16), (22, 24), (10, 24), (6, 16)], outline=accent)
        d.ellipse([13, 13, 19, 19], fill=shade(accent, 0.8))
    elif pattern == 'dust':
        for i in range(12):
            x = rng.randint(6, 26); y = rng.randint(6, 26)
            d.point((x, y), fill=GOLD_L)
    elif pattern == 'arch':
        d.arc([8, 12, 24, 30], 180, 360, fill=accent, width=2)
    return img

block_colors = {
    'fate_altar': ((70, 60, 90), 'panel'), 'die_engraver': ((80, 75, 85), 'panel'),
    'appraisal_table': ((60, 80, 90), 'panel'), 'repair_bench': ((85, 70, 60), 'dust'),
    'fate_loom': ((75, 85, 70), 'arch'), 'offering_shrine': ((90, 85, 80), 'panel'),
    'merchant_stall': ((110, 85, 55), 'arch'), 'null_portal': ((30, 25, 45), 'arch')}
for bid, (c, pat) in block_colors.items():
    block_tex(c, pat).save(f'{TEX}/block/{bid}.png')

# 进度背景（暗色纹理 64×64）
bg = Image.new('RGBA', (64, 64), (20, 17, 26, 255))
d = ImageDraw.Draw(bg)
rng = random.Random(7)
for i in range(60):
    x, y = rng.randint(0, 63), rng.randint(0, 63)
    d.point((x, y), fill=(40, 34, 52))
for i in range(12):
    x, y = rng.randint(4, 58), rng.randint(4, 58)
    r = rng.randint(2, 4)
    d.ellipse([x, y, x + r, y + r], outline=(60, 50, 70))
bg.save(f'{TEX}/gui/advancement_background.png')

# Boss 占位贴图 64×64（人形皮肤布局：后续 Blockbench 精修）
for boss, c in [('two_faced_gambler', (120, 90, 150)), ('molten_die_king', (180, 80, 40)),
                ('frost_silence_queen', (120, 170, 210)), ('nullface', (60, 55, 75))]:
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 64, 64], fill=shade(c, 0.5))
    d.rectangle([20, 20, 44, 32], fill=INK)      # 眼区
    d.rectangle([24, 24, 28, 28], fill=GOLD_L)
    d.rectangle([36, 24, 40, 28], fill=GOLD_L)
    d.rectangle([0, 32, 64, 64], fill=shade(c, 0.35))
    img.save(f'{TEX}/entity/{boss}.png')

# 护甲装备贴图（64×32 humanoid layer0 布局）
for asset, c in [('dicecaster', (70, 90, 115)), ('gambler', (120, 95, 65))]:
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([40, 0, 44, 8], fill=shade(c, 0.6))     # 头
    d.rectangle([16, 20, 40, 32], fill=shade(c, 0.5))   # 身
    d.rectangle([40, 20, 56, 30], fill=shade(c, 0.45))  # 臂
    d.rectangle([0, 20, 16, 32], fill=shade(c, 0.4))    # 腿
    d.rectangle([0, 20, 56, 21], fill=GOLD_D)
    img.save(f'{TEX}/equipment/humanoid/{asset}.png')

print('all textures done')
