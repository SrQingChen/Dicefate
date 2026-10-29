# -*- coding: utf-8 -*-
"""骰子物品模型切换为 3D 网格 + 生成材质化编号面纹理。"""
import json, os
from PIL import Image, ImageDraw

RES = 'src/main/resources/assets/dicefate'

def wjson(path, obj):
    full = os.path.join(RES, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)

# ============ 材质调色板（面块底色/边框/字色） ============
PALETTES = {
    "bone": ((222, 214, 196), (170, 150, 110), (40, 34, 26)),
    "iron": ((200, 204, 212), (120, 124, 132), (30, 30, 34)),
    "mithril": ((168, 208, 218), (95, 140, 155), (25, 40, 45)),
    "fategold": ((238, 200, 110), (170, 120, 40), (45, 32, 12)),
    "starlight": ((240, 240, 250), (150, 155, 185), (35, 35, 60)),
    "ember": ((228, 140, 80), (150, 70, 30), (40, 18, 8)),
    "frost": ((160, 205, 235), (85, 130, 175), (20, 40, 60)),
    "storm": ((228, 215, 110), (150, 135, 45), (40, 36, 10)),
    "venom": ((160, 200, 100), (90, 130, 50), (25, 40, 12)),
    "radiant": ((245, 230, 150), (180, 160, 70), (50, 42, 12)),
    "umbral": ((160, 125, 195), (95, 65, 130), (30, 18, 45)),
    "skill": ((185, 205, 200), (95, 130, 125), (25, 38, 36)),
    "add": ((185, 215, 205), (85, 145, 130), (20, 45, 40)),
    "mul": ((235, 205, 130), (160, 120, 45), (45, 32, 12)),
    "unique": ((230, 215, 245), (160, 120, 200), (45, 28, 60)),
    "cursed": ((170, 120, 180), (100, 55, 110), (35, 15, 40)),
}

FONT = {
 '0':["111","101","101","101","111"],'1':["010","110","010","010","111"],
 '2':["111","001","111","100","111"],'3':["111","001","111","001","111"],
 '4':["101","101","111","001","001"],'5':["111","100","111","001","111"],
 '6':["111","100","111","101","111"],'7':["111","001","010","010","010"],
 '8':["111","101","111","101","111"],'9':["111","101","111","001","111"]}

def draw_digit(d, x, y, num, c, s=1):
    for i, ch in enumerate(str(num)):
        g = FONT.get(ch, FONT['0'])
        for gy, row in enumerate(g):
            for gx, px in enumerate(row):
                if px == '1':
                    d.rectangle([x+i*4*s+gx*s, y+gy*s, x+i*4*s+gx*s+s-1, y+gy*s+s-1], fill=c)

GRIDS = {'d4':(2,2,4),'d6':(3,2,6),'d8':(3,3,8),'d10':(4,3,10),'d12':(4,3,12),'d20':(5,4,20),'d24':(5,5,24)}

def shape_texture(shape, palette):
    cols, rows, n = GRIDS[shape]
    base, border, ink = PALETTES[palette]
    img = Image.new('RGBA', (32, 32), (26, 22, 30, 255))
    d = ImageDraw.Draw(img)
    cw, ch = 32/cols, 32/rows
    for i in range(n):
        col, row = i % cols, i // cols
        x1, y1 = int(col*cw), int(row*ch)
        x2, y2 = int((col+1)*cw)-1, int((row+1)*ch)-1
        d.rectangle([x1, y1, x2, y2], fill=base, outline=border)
        # 高光角
        d.point((x1+1, y1+1), fill=(255, 255, 255, 160))
        num = i + 1
        wpx = 4 if num < 10 else 8
        s = 1
        draw_digit(d, x1 + (x2-x1-wpx)//2, y1 + (y2-y1-5)//2, num, ink, s)
    return img

def mesh_model(shape, palette):
    return {"loader": "dicefate:mesh",
            "mesh": f"dicefate:die_{shape}",
            "texture": f"dicefate:item/die_shape_{shape}_{palette}",
            "scale": 0.44}

# ============ 骰→形状/调色板映射 ============
mats = {"bone","iron","mithril","fategold","starlight"}
def shape_of(face):
    return f"d{face}"

def gen_die(id_path, face, palette):
    shape = shape_of(face)
    # 纹理（去重生成）
    tex = f'textures/item/die_shape_{shape}_{palette}.png'
    full = os.path.join(RES, tex)
    if not os.path.exists(full):
        shape_texture(shape, palette).save(full)
    # 模型
    wjson(f'models/item/{id_path}.json', mesh_model(shape, palette))

# 基础骰 15
combos = [("bone",4),("bone",6),("bone",8),("iron",6),("iron",8),("iron",10),
          ("mithril",8),("mithril",10),("mithril",12),("fategold",10),("fategold",12),("fategold",20),
          ("starlight",12),("starlight",20),("starlight",24)]
for m, f in combos:
    gen_die(f"die_{m}_d{f}", f, m)

# 元素骰 18
for e in ["ember","frost","storm","venom","radiant","umbral"]:
    for suf, face in [("spark",8),("resonant",12),("origin",20)]:
        gen_die(f"die_{e}_{suf}", face, e)

# 技能骰 24
for fam in ["echo","pursuit","ward","breaker","reverse","greed","lock","favor"]:
    for t, face in [(1,6),(2,12),(3,20)]:
        gen_die(f"skill_die_{fam}_{t}", face, "skill")

# 加算 6 / 乘算 6
for k, face in [("edge",6),("wall",6),("poise",8),("element",8),("combo",6),("mend",8)]:
    gen_die(f"add_die_{k}", face, "add")
for k, face in [("fierce",8),("bulwark",8),("pierce",8),("break",10),("resonance",10),("fate",12)]:
    gen_die(f"mul_die_{k}", face, "mul")

# 唯一 10
for u, face in [("cracked_face",6),("two_faced",8),("molten_core",12),("frost_silence",12),
                ("storm_verdict",12),("bone_eater",10),("radiant_oath",10),("umbral_veil",10),
                ("null_face",6),("fate_sovereign",20)]:
    gen_die(f"unique_die_{u}", face, "unique")

# 诅咒 4
for c, face in [("greed",12),("blood",10),("fracture",20),("misfortune",8)]:
    gen_die(f"cursed_die_{c}", face, "cursed")

print('mesh dice models:', 83)
