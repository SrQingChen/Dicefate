# -*- coding: utf-8 -*-
"""0.3.0 骰子资产统一生成器：精确数学网格 + 对齐网格贴图 + 物品模型。

背景（0.2.0 缺陷）：Blockbench 导出的 mesh JSON 顶点不共享/面不共面/绕序混乱
（d12 V=19≠20、d20 E=43≠30、d10 面翘曲 0.445），32×32 贴图上 5×5 网格数字溢出。
本脚本用精确坐标重建 7 种正多面体/标准骰形体，网格与贴图由同一布局定义生成，
保证 uv 矩形与数字单元格严格对齐；顶点坐标以 6 位小数字符串化，共享顶点跨面完全一致。

用法：
  python gen_dice_assets_030.py           # 写入 resources
  python gen_dice_assets_030.py --dry-run # 只做几何自检并打印报告
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw
from scipy.spatial import ConvexHull

RES = os.path.join('src', 'main', 'resources', 'assets', 'dicefate')
DRY = '--dry-run' in sys.argv

# ============ 贴图网格布局（形状 → (列, 行, 纹理尺寸)） ============
# 单元格按 number-1 行优先排列；≥10 面用 64×64 保证两位数不溢出。
LAYOUTS = {
    'd4':  (2, 2, 32),
    'd6':  (3, 2, 32),
    'd8':  (4, 2, 64),
    'd10': (5, 2, 64),
    'd12': (4, 3, 64),
    'd20': (5, 4, 64),
    'd24': (6, 4, 64),
}

# 材质调色板（沿用 0.2.0 视觉基调）
PALETTES = {
    "bone":      ((222, 214, 196), (170, 150, 110), (40, 34, 26)),
    "iron":      ((200, 204, 212), (120, 124, 132), (30, 30, 34)),
    "mithril":   ((168, 208, 218), (95, 140, 155), (25, 40, 45)),
    "fategold":  ((238, 200, 110), (170, 120, 40), (45, 32, 12)),
    "starlight": ((240, 240, 250), (150, 155, 185), (35, 35, 60)),
    "ember":     ((228, 140, 80), (150, 70, 30), (40, 18, 8)),
    "frost":     ((160, 205, 235), (85, 130, 175), (20, 40, 60)),
    "storm":     ((228, 215, 110), (150, 135, 45), (40, 36, 10)),
    "venom":     ((160, 200, 100), (90, 130, 50), (25, 40, 12)),
    "radiant":   ((245, 230, 150), (180, 160, 70), (50, 42, 12)),
    "umbral":    ((160, 125, 195), (95, 65, 130), (30, 18, 45)),
    "skill":     ((185, 205, 200), (95, 130, 125), (25, 38, 36)),
    "add":       ((185, 215, 205), (85, 145, 130), (20, 45, 40)),
    "mul":       ((235, 205, 130), (160, 120, 45), (45, 32, 12)),
    "unique":    ((230, 215, 245), (160, 120, 200), (45, 28, 60)),
    "cursed":    ((170, 120, 180), (100, 55, 110), (35, 15, 40)),
}

FONT = {
    '0': ["111", "101", "101", "101", "111"], '1': ["010", "110", "010", "010", "111"],
    '2': ["111", "001", "111", "100", "111"], '3': ["111", "001", "111", "001", "111"],
    '4': ["101", "101", "111", "001", "001"], '5': ["111", "100", "111", "001", "111"],
    '6': ["111", "100", "111", "101", "111"], '7': ["111", "001", "010", "010", "010"],
    '8': ["111", "101", "111", "101", "111"], '9': ["111", "101", "111", "001", "111"],
}

# 生成哪些 贴图后缀（与既有 57 张文件名一一对应）
SHAPE_PALETTE_SUFFIXES = {
    'd4':  ['', '_bone'],
    'd6':  ['', '_bone', '_iron', '_skill', '_unique', '_add'],
    'd8':  ['', '_bone', '_iron', '_mithril', '_ember', '_frost', '_storm', '_venom',
            '_radiant', '_umbral', '_add', '_mul', '_unique', '_cursed'],
    'd10': ['', '_iron', '_mithril', '_fategold', '_unique', '_mul', '_cursed'],
    'd12': ['', '_mithril', '_fategold', '_starlight', '_ember', '_frost', '_storm',
            '_venom', '_radiant', '_umbral', '_skill', '_mul', '_unique', '_cursed'],
    'd20': ['', '_fategold', '_starlight', '_ember', '_frost', '_storm', '_venom',
            '_radiant', '_umbral', '_skill', '_unique', '_cursed'],
    'd24': ['', '_starlight'],
}

# ============ 几何构造 ============

def _fmt(v):
    """6 位小数、去多余尾零；保证共享顶点字符串完全一致。"""
    s = f"{v:.6f}".rstrip('0').rstrip('.')
    return '0' if s in ('-0', '') else s


def _canon(verts):
    return [tuple(float(_fmt(c)) for c in v) for v in verts]


def _poly_from_hull(points):
    """凸包 → 外向 CCW 面。points: np.array(N,3)。"""
    hull = ConvexHull(points)
    faces = []
    for simplex in hull.simplices:
        a, b, c = (points[simplex[i]] for i in range(3))
        center = points[simplex].mean(axis=0)
        n = np.cross(b - a, c - a)
        if np.dot(n, center) < 0:          # 法线朝内 → 反转绕序
            simplex = [simplex[0], simplex[2], simplex[1]]
        faces.append([points[i] for i in simplex])
    return faces


def _dedupe_faces(faces):
    """把 hull 的三角面合并回共面多边形（d12 五边形）。"""
    merged = []
    used = [False] * len(faces)
    for i, f in enumerate(faces):
        if used[i]:
            continue
        group = [f]
        used[i] = True
        changed = True
        while changed:
            changed = False
            for j, g in enumerate(faces):
                if used[j]:
                    continue
                # 共面且共享至少 2 个顶点 → 同一多边形面
                for other in group:
                    shared = {tuple(_canon([v])[0]) for v in g} & {tuple(_canon([v])[0]) for v in other}
                    if len(shared) >= 2 and _coplanar(group[0], g):
                        group.append(g)
                        used[j] = True
                        changed = True
                        break
        merged.append(_merge_group(group))
    return merged


def _coplanar(f, g, tol=1e-6):
    a, b, c = np.array(f[0]), np.array(f[1]), np.array(f[2])
    n = np.cross(b - a, c - a)
    n = n / np.linalg.norm(n)
    for v in g:
        if abs(np.dot(n, np.array(v) - a)) > tol:
            return False
    return True


def _merge_group(group):
    """把一组共享边的三角形合并为一个有序多边形（边端点度为 1 的顺序环）。"""
    edges = {}
    for tri in group:
        ks = [tuple(_canon([v])[0]) for v in tri]
        for i in range(3):
            e = tuple(sorted((ks[i], ks[(i + 1) % 3])))
            edges[e] = edges.get(e, 0) + 1
    boundary = [e for e, c in edges.items() if c == 1]
    adj = {}
    for a, b in boundary:
        adj.setdefault(a, []).append(b)
        adj.setdefault(b, []).append(a)
    start = boundary[0][0]
    ring = [start]
    prev = None
    cur = start
    while True:
        nxt = [n for n in adj[cur] if n != prev][0]
        ring.append(nxt)
        prev, cur = cur, nxt
        if cur == start:
            break
    ring = ring[:-1]
    if len(ring) != len(boundary):
        raise ValueError('ring merge failed')
    return ring


def shape_d4():
    verts = [(1, 1, 1), (1, -1, -1), (-1, 1, -1), (-1, -1, 1)]
    faces_idx = [(0, 1, 3), (0, 2, 1), (0, 3, 2), (1, 2, 3)]  # 外向 CCW
    return [tuple(verts[i] for i in f) for f in faces_idx]


def shape_d6():
    v = [(x, y, z) for x in (-1, 1) for y in (-1, 1) for z in (-1, 1)]
    # 6 个外向 CCW 四边形
    quads = [
        (0, 1, 3, 2),  # z=-1 面（v[0]=(-1,-1,-1)…）
        (4, 6, 7, 5),  # z=+1
        (0, 4, 5, 1),  # x=-1
        (2, 3, 7, 6),  # x=+1
        (0, 2, 6, 4),  # y=-1
        (1, 5, 7, 3),  # y=+1
    ]
    return [tuple(v[i] for i in q) for q in quads]


def shape_d8():
    ax = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]
    idx = [(4, 0, 2), (4, 2, 1), (4, 1, 3), (4, 3, 0),
           (5, 2, 0), (5, 1, 2), (5, 3, 1), (5, 0, 3)]
    return [tuple(ax[i] for i in f) for f in idx]


def shape_d10():
    """五角偏方面体（标准 d10）：共面条件 b/a = tan²(θ/2)，θ=36°。"""
    a = 0.95
    b = a * math.tan(math.radians(18.0)) ** 2
    r = 1.0
    up = [(r * math.cos(math.radians(72 * k)), r * math.sin(math.radians(72 * k)), b) for k in range(5)]
    lo = [(r * math.cos(math.radians(72 * k + 36)), r * math.sin(math.radians(72 * k + 36)), -b) for k in range(5)]
    apex_t = (0, 0, a)
    apex_b = (0, 0, -a)
    faces = []
    for k in range(5):
        # 上半风筝：apex_t, U[k], L[k], U[k+1] —— 给出外向 CCW
        f = [apex_t, up[k], lo[k], up[(k + 1) % 5]]
        faces.append(_fix_winding(f))
        # 下半风筝：apex_b, L[k], U[k+1], L[k+1]
        g = [apex_b, lo[k], up[(k + 1) % 5], lo[(k + 1) % 5]]
        faces.append(_fix_winding(g))
    return faces


def _fix_winding(face):
    pts = [np.array(p) for p in face]
    n = np.cross(pts[1] - pts[0], pts[2] - pts[0])
    c = np.mean(pts, axis=0)
    if np.dot(n, c) < 0:
        face = list(reversed(face))
    return tuple(face)


def shape_d12():
    phi = (1 + 1.618033988749895) / 2 if False else (1 + math.sqrt(5)) / 2
    pts = []
    for x in (-1, 1):
        for y in (-1, 1):
            for z in (-1, 1):
                pts.append((x, y, z))
    inv = 1 / phi
    for s1 in (-1, 1):
        for s2 in (-1, 1):
            pts.append((0, s1 * inv, s2 * phi))
            pts.append((s1 * inv, s2 * phi, 0))
            pts.append((s1 * phi, 0, s2 * inv))
    arr = np.array(pts, dtype=float)
    return _dedupe_faces(_poly_from_hull(arr))


def shape_d20():
    phi = (1 + math.sqrt(5)) / 2
    pts = []
    for s1 in (-1, 1):
        for s2 in (-1, 1):
            pts.append((0, s1, s2 * phi))
            pts.append((s1, s2 * phi, 0))
            pts.append((s2 * phi, 0, s1))
    return _poly_from_hull(np.array(pts, dtype=float))


def shape_d24():
    """ deltoidal icositetrahedron = 小斜方截半立方体（均匀）的面心对偶。"""
    t = 1 + math.sqrt(2)
    pts = []
    for a in (-1, 1):
        for b in (-1, 1):
            pts.append((a, b, t)); pts.append((a, b, -t))
            pts.append((a, t, b)); pts.append((a, -t, b))
            pts.append((t, a, b)); pts.append((-t, a, b))
    arr = np.array(pts, dtype=float)
    hull = ConvexHull(arr)
    centroids = []
    for eq, simplex in zip(hull.equations, hull.simplices):
        n = eq[:3]
        d = -eq[3]                      # 平面 n·x = d（n 已单位化）
        centroids.append(n / d)         # 极对偶顶点 → Catalan 严格对偶（面必共面）
    centroids = np.array(centroids)
    return _dedupe_faces(_poly_from_hull(centroids))


SHAPES = {
    'd4': shape_d4, 'd6': shape_d6, 'd8': shape_d8, 'd10': shape_d10,
    'd12': shape_d12, 'd20': shape_d20, 'd24': shape_d24,
}


def assign_numbers(faces):
    """对面配对：number 之和 = N+1（偶数骰的通用惯例）。"""
    n = len(faces)
    centers = [np.mean(np.array(f), axis=0) for f in faces]
    numbers = [0] * n
    nxt = 1
    for i in range(n):
        if numbers[i]:
            continue
        if nxt > n // 2:
            numbers[i] = nxt
            continue
        # 找对面
        best, bd = -1, 1e9
        for j in range(n):
            if j == i or numbers[j]:
                continue
            d = np.linalg.norm(centers[i] + centers[j])
            if d < bd:
                bd, best = d, j
        numbers[i] = nxt
        numbers[best] = n + 1 - nxt
        nxt += 1
    return numbers


def normalize_scale(faces):
    """scale = 0.46 / max|coord|，保证 scale 后几何完全位于 0.04..0.96。"""
    m = max(abs(c) for f in faces for v in f for c in v)
    return round(0.46 / m, 4)


def uv_rect(shape, number):
    cols, rows, _size = LAYOUTS[shape]
    idx = number - 1
    col, row = idx % cols, idx // cols
    inset = 1.0 / LAYOUTS[shape][2]  # 1px
    u1 = col / cols + inset
    u2 = (col + 1) / cols - inset
    v1 = row / rows + inset
    v2 = (row + 1) / rows - inset
    return [round(u1, 5), round(v1, 5), round(u2, 5), round(v2, 5)]


def build_mesh(shape):
    faces = SHAPES[shape]()
    faces = [[[float(c) for c in v] for v in f] for f in faces]   # 统一 float
    faces = [_fix_winding(f) for f in faces]
    numbers = assign_numbers(faces)
    scale = normalize_scale(faces)
    face_objs = []
    for f, num in zip(faces, numbers):
        verts = [[float(_fmt(c)) for c in v] for v in f]
        face_objs.append({
            'verts': verts,
            'uv': uv_rect(shape, num),
            'number': num,
        })
    return {
        'format': 'dicefate:mesh/1',
        'texture': f'dicefate:item/die_shape_{shape}',
        'scale': scale,
        'faces': face_objs,
    }, faces, numbers


# ============ 贴图 ============

def draw_digits(d, x, y, num, color, s):
    chars = str(num)
    total_w = len(chars) * 4 * s - s
    cx = x + (4 * s - 1 - total_w) // 2 + (s - 1 if s > 1 else 0)
    for i, ch in enumerate(chars):
        g = FONT.get(ch, FONT['0'])
        ox = x + i * 4 * s
        for gy, rowline in enumerate(g):
            for gx, px in enumerate(rowline):
                if px == '1':
                    d.rectangle([ox + gx * s, y + gy * s, ox + gx * s + s - 1, y + gy * s + s - 1], fill=color)


def build_texture(shape, palette_name):
    cols, rows, size = LAYOUTS[shape]
    base, border, ink = PALETTES[palette_name if palette_name else 'bone']
    img = Image.new('RGBA', (size, size), (26, 22, 30, 255))
    d = ImageDraw.Draw(img)
    cw, ch = size / cols, size / rows
    n = cols * rows
    for i in range(LAYOUT_COUNT[shape]):
        col, row = i % cols, i // cols
        x1, y1 = int(round(col * cw)), int(round(row * ch))
        x2, y2 = int(round((col + 1) * cw)) - 1, int(round((row + 1) * ch)) - 1
        d.rectangle([x1, y1, x2, y2], fill=base, outline=border)
        d.point((x1 + 1, y1 + 1), fill=(255, 255, 255, 150))
        num = i + 1
        s = 2 if size == 64 else 1
        if size == 32 and num >= 10:
            s = 1
        wpx = (len(str(num)) * 4 - 1) * s
        hpx = 5 * s
        draw_digits(d, x1 + max(0, (x2 - x1 + 1 - wpx) // 2), y1 + max(0, (y2 - y1 + 1 - hpx) // 2), num, ink, s)
    return img


LAYOUT_COUNT = {k: int(k[1:]) for k in LAYOUTS}


def item_model(shape, palette):
    """0.3.0：不再硬编码 scale（由 mesh JSON 的 scale 提供）。"""
    return {"loader": "dicefate:mesh",
            "mesh": f"dicefate:die_{shape}",
            "texture": f"dicefate:item/die_shape_{shape}_{palette}"}


def collect_dice_items():
    out = []
    for m, f in [("bone", 4), ("bone", 6), ("bone", 8), ("iron", 6), ("iron", 8), ("iron", 10),
                 ("mithril", 8), ("mithril", 10), ("mithril", 12), ("fategold", 10), ("fategold", 12),
                 ("fategold", 20), ("starlight", 12), ("starlight", 20), ("starlight", 24)]:
        out.append((f"die_{m}_d{f}", f, m))
    for e in ["ember", "frost", "storm", "venom", "radiant", "umbral"]:
        for suf, face in [("spark", 8), ("resonant", 12), ("origin", 20)]:
            out.append((f"die_{e}_{suf}", face, e))
    for fam in ["echo", "pursuit", "ward", "breaker", "reverse", "greed", "lock", "favor"]:
        for t, face in [(1, 6), (2, 12), (3, 20)]:
            out.append((f"skill_die_{fam}_{t}", face, "skill"))
    for k, face in [("edge", 6), ("wall", 6), ("poise", 8), ("element", 8), ("combo", 6), ("mend", 8)]:
        out.append((f"add_die_{k}", face, "add"))
    for k, face in [("fierce", 8), ("bulwark", 8), ("pierce", 8), ("break", 10), ("resonance", 10), ("fate", 12)]:
        out.append((f"mul_die_{k}", face, "mul"))
    for u, face in [("cracked_face", 6), ("two_faced", 8), ("molten_core", 12), ("frost_silence", 12),
                    ("storm_verdict", 12), ("bone_eater", 10), ("radiant_oath", 10), ("umbral_veil", 10),
                    ("null_face", 6), ("fate_sovereign", 20)]:
        out.append((f"unique_die_{u}", face, "unique"))
    for c, face in [("greed", 12), ("blood", 10), ("fracture", 20), ("misfortune", 8)]:
        out.append((f"cursed_die_{c}", face, "cursed"))
    return out


def main():
    report = {}
    for shape in ('d4', 'd6', 'd8', 'd10', 'd12', 'd20', 'd24'):
        mesh, faces, numbers = build_mesh(shape)
        # —— 自检 ——
        vset = {tuple(round(c, 6) for c in v) for f in faces for v in f}
        eset = set()
        for f in faces:
            for i in range(len(f)):
                a = tuple(round(c, 6) for c in f[i])
                b = tuple(round(c, 6) for c in f[(i + 1) % len(f)])
                eset.add((min(a, b), max(a, b)))
        euler = len(vset) - len(eset) + len(faces)
        # 平面度
        worst = 0.0
        for f in faces:
            a, b, c = (np.array(f[i]) for i in range(3))
            n = np.cross(b - a, c - a)
            n /= np.linalg.norm(n)
            for p in f[3:]:
                worst = max(worst, abs(np.dot(n, np.array(p) - a)))
        # 绕序
        inward = 0
        for f in faces:
            a, b, c = (np.array(f[i]) for i in range(3))
            n = np.cross(b - a, c - a)
            ctr = np.mean([np.array(p) for p in f], axis=0)
            if np.dot(n, ctr) < 0:
                inward += 1
        # 对面和：每面取其最近对偶面（argmin |ci+cj|），配对需互斥且和为 N+1
        # （d4 四面体无对面，跳过配对校验）
        n_faces = len(faces)
        centers = [np.mean(np.array(f), axis=0) for f in faces]
        pair_ok = True
        if shape == 'd4':
            pair_ok = None  # 不适用
        else:
            for i in range(n_faces):
                j = min((j for j in range(n_faces) if j != i),
                        key=lambda j: np.linalg.norm(centers[i] + centers[j]))
                if np.linalg.norm(centers[i] + centers[j]) > 0.6 * np.linalg.norm(centers[i]):
                    pair_ok = False  # 找不到对面（不该发生）
                    continue
                if numbers[i] + numbers[j] != n_faces + 1:
                    pair_ok = False
        counts = sorted(numbers) == list(range(1, n_faces + 1))
        report[shape] = dict(V=len(vset), E=len(eset), F=n_faces, euler=euler,
                             planarity=f'{worst:.2e}', inward=inward,
                             pairs_ok=pair_ok, numbers_ok=counts,
                             scale=mesh['scale'])
        if not DRY:
            mdir = os.path.join(RES, 'models', 'mesh')
            os.makedirs(mdir, exist_ok=True)
            with open(os.path.join(mdir, f'die_{shape}.json'), 'w', encoding='utf-8', newline='\n') as fh:
                json.dump(mesh, fh, ensure_ascii=False, indent=1)
            # 贴图
            tdir = os.path.join(RES, 'textures', 'item')
            os.makedirs(tdir, exist_ok=True)
            for suf in SHAPE_PALETTE_SUFFIXES[shape]:
                pal = suf[1:] if suf else ''
                name = f'die_shape_{shape}{suf}.png'
                build_texture(shape, pal).save(os.path.join(tdir, name))

    print(json.dumps(report, indent=1))
    if DRY:
        print('[dry-run] 未写入任何文件')
        return

    # —— 83 个物品模型（无 scale，由 mesh JSON 提供） ——
    items = collect_dice_items()
    assert len(items) == 83, f'物品数 {len(items)} != 83'
    imdir = os.path.join(RES, 'models', 'item')
    os.makedirs(imdir, exist_ok=True)
    for id_path, face, palette in items:
        with open(os.path.join(imdir, f'{id_path}.json'), 'w', encoding='utf-8', newline='\n') as fh:
            json.dump(item_model(f'd{face}', palette), fh, ensure_ascii=False, indent=1)
    print(f'item models: {len(items)}')


if __name__ == '__main__':
    main()
