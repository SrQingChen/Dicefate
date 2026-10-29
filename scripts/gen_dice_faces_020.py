# -*- coding: utf-8 -*-
"""0.2.0 HUD 骰面贴图：face_<n>.png 与 face_<n>_<element>.png（16×16）。
3×5 像素数字字体，元素色底 + 奶白数字 + 描边。"""
from PIL import Image, ImageDraw
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "dicefate", "textures", "gui", "dice")
os.makedirs(OUT, exist_ok=True)

# 3x5 数字字体（每行3位二进制）
FONT = {
    '0': ["111", "101", "101", "101", "111"],
    '1': ["010", "110", "010", "010", "111"],
    '2': ["111", "001", "111", "100", "111"],
    '3': ["111", "001", "111", "001", "111"],
    '4': ["101", "101", "111", "001", "001"],
    '5': ["111", "100", "111", "001", "111"],
    '6': ["111", "100", "111", "101", "111"],
    '7': ["111", "001", "010", "010", "010"],
    '8': ["111", "101", "111", "101", "111"],
    '9': ["111", "101", "111", "001", "111"],
}

ELEMENTS = {
    "":       ("#5a5142", "#7a7160"),   # 中性（NONE）
    "ember":  ("#8a3a20", "#b04a2a"),
    "frost":  ("#3a6a9a", "#5a9bd4"),
    "storm":  ("#888026", "#b8a83d"),
    "venom":  ("#4f7a2a", "#6fa83d"),
    "radiant":("#a89036", "#e8d44d"),
    "umbral": ("#4c3868", "#6b4e8e"),
    "cursed": ("#4a2030", "#6a3048"),
}

CREAM = (255, 246, 214, 255)
DARK = (18, 18, 24, 255)

def darken(hexcolor, f=0.55):
    h = hexcolor.lstrip('#')
    r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
    return (int(r * f), int(g * f), int(b * f))

def draw_number(draw, num, cx, cy, fill, outline):
    s = str(num)
    w = len(s) * 4 - 1
    x0, y0 = cx - w // 2, cy - 2
    # 描边层
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            if dx or dy:
                paint_num(draw, s, x0 + dx, y0 + dy, outline)
    paint_num(draw, s, x0, y0, fill)

def paint_num(draw, s, x0, y0, color):
    for i, ch in enumerate(s):
        rows = FONT[ch]
        for ry, row in enumerate(rows):
            for rx, c in enumerate(row):
                if c == '1':
                    draw.rectangle([x0 + i * 4 + rx, y0 + ry, x0 + i * 4 + rx, y0 + ry], fill=color)

count = 0
for elem, (base, edge) in ELEMENTS.items():
    for n in range(1, 25):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        b_rgb = tuple(int(base.lstrip('#')[i:i+2], 16) for i in (0, 2, 4)) + (255,)
        e_rgb = tuple(int(edge.lstrip('#')[i:i+2], 16) for i in (0, 2, 4)) + (255,)
        d.rectangle([0, 0, 15, 15], fill=b_rgb, outline=e_rgb, width=1)
        d.rectangle([1, 1, 14, 14], outline=darken(base, 0.5) + (255,))
        # 角点高光
        d.point((2, 2), fill=(255, 255, 255, 70))
        draw_number(d, n, 8, 8, CREAM, DARK)
        name = f"face_{n}" + (f"_{elem}" if elem else "") + ".png"
        img.save(os.path.join(OUT, name))
        count += 1
print("generated", count, "->", OUT)
