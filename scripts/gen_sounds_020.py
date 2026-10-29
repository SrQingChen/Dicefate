# -*- coding: utf-8 -*-
"""0.2.0 音效合成：numpy 生成 WAV → ffmpeg 转 OGG。
策划 8.9：落定统一金属冲击音、Miss、破势、命破、兑现、牌型、掷骰。"""
import numpy as np, wave, os, subprocess, sys

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "dicefate", "sounds")
TMP = os.path.join(os.path.dirname(__file__), "wav_tmp")
os.makedirs(OUT, exist_ok=True); os.makedirs(TMP, exist_ok=True)
rng = np.random.default_rng(20261023)

def env(n, a=0.002, decay=None):
    t = np.linspace(0, 1, n)
    d = decay if decay else 6.0
    e = np.minimum(t / max(a * n / n, 1e-9) if False else t / a if a > 0 else np.ones_like(t), 1.0)
    e = np.where(t < a, t / a, np.exp(-d * (t - a)))
    return e

def metal_hit(dur=0.28, base=520.0, bright=2.6, noise=0.35):
    n = int(SR * dur); t = np.arange(n) / SR
    sig = np.zeros(n)
    for h, amp in [(1, 1.0), (2.01, 0.55), (2.74, 0.42), (4.05, 0.3), (5.42, 0.2)]:
        f = base * h
        if f < SR / 2:
            sig += amp * np.sin(2 * np.pi * f * t) * np.exp(-6.5 * h ** 0.5 * t)
    sig += noise * rng.standard_normal(n) * np.exp(-24 * t)          # 攻击瞬态
    sig *= env(n, a=0.001, decay=7)
    return sig

def thud(dur=0.35, base=110.0):
    n = int(SR * dur); t = np.arange(n) / SR
    sig = np.sin(2 * np.pi * base * t * (1 - 0.35 * t)) * np.exp(-9 * t)
    sig += 0.25 * rng.standard_normal(n) * np.exp(-30 * t)
    sig *= env(n, a=0.002, decay=9)
    return sig

def crack(dur=0.4):
    n = int(SR * dur); t = np.arange(n) / SR
    bursts = []
    for off, amp, dec in [(0.0, 1.0, 34), (0.045, 0.7, 30), (0.11, 0.5, 26), (0.19, 0.35, 22)]:
        m = np.zeros(n)
        s = int(off * SR)
        m[s:] = rng.standard_normal(n - s) * np.exp(-dec * (t[:n - s])) * amp
        bursts.append(m)
    sig = np.sum(bursts, axis=0)
    return sig / np.max(np.abs(sig) + 1e-9)

def shimmer(dur=0.5, base=880.0, harmonics=(1, 1.335, 1.78, 2.0, 2.67)):
    n = int(SR * dur); t = np.arange(n) / SR
    sig = np.zeros(n)
    for i, h in enumerate(harmonics):
        f = base * h
        if f < SR / 2:
            sig += (1 / (i + 1)) * np.sin(2 * np.pi * f * t + rng.uniform(0, 6.28)) * np.exp(-4.2 * t)
    return sig * env(n, a=0.004, decay=4)

def roll_rattle(dur=0.5, n_hits=7):
    n = int(SR * dur)
    sig = np.zeros(n)
    for i in range(n_hits):
        off = int((i + rng.uniform(0.2, 0.8)) * (n - SR * 0.05) / n_hits)
        d = int(SR * 0.05)
        seg = rng.standard_normal(d) * np.exp(-60 * np.arange(d) / SR)
        f = 380 + rng.uniform(-90, 160)
        tt = np.arange(d) / SR
        seg += 0.5 * np.sin(2 * np.pi * f * tt) * np.exp(-45 * tt)
        end = min(n, off + d)
        sig[off:end] += seg[:end - off] * (0.55 + 0.45 * i / n_hits)
    return sig

def power_chord(dur=0.45, base=196.0):
    n = int(SR * dur); t = np.arange(n) / SR
    sig = np.zeros(n)
    for mult, amp in [(1, 1.0), (1.5, 0.75), (2.0, 0.6), (3.0, 0.35)]:
        sig += amp * np.sin(2 * np.pi * base * mult * t) * np.exp(-5 * t)
    sig += 0.18 * rng.standard_normal(n) * np.exp(-26 * t)
    return sig * env(n, a=0.003, decay=5)

def bell(dur=0.6, base=660.0):
    n = int(SR * dur); t = np.arange(n) / SR
    sig = np.zeros(n)
    for h, amp in [(1, 1.0), (2.4, 0.5), (3.9, 0.28)]:
        f = base * h
        if f < SR / 2:
            sig += amp * np.sin(2 * np.pi * f * t) * np.exp(-3.5 * h ** 0.4 * t)
    return sig

SOUNDS = {
    "settle_impact": metal_hit(),          # 落定统一金属冲击
    "settle_impact_great": metal_hit(base=660, bright=3.0),  # 大胜落定
    "contest_miss": thud(base=95),          # Miss 闷响
    "poise_break": crack(),                 # 破势碎裂
    "fatebreak": power_chord(base=147),     # 命破
    "cashout": shimmer(base=784),           # 兑现
    "pattern": bell(base=880),              # 牌型
    "dice_roll": roll_rattle(),             # 掷骰
}

for name, sig in SOUNDS.items():
    sig = sig / (np.max(np.abs(sig)) + 1e-9) * 0.85
    pcm = (sig * 32767).astype(np.int16)
    wav_path = os.path.join(TMP, name + ".wav")
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    ogg = os.path.join(OUT, name + ".ogg")
    r = subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path,
                        "-c:a", "libvorbis", "-q:a", "4", ogg], capture_output=True, text=True)
    if r.returncode != 0:
        print("FFMPEG FAIL", name, r.stderr); sys.exit(1)
    print("ok", name, os.path.getsize(ogg), "bytes")
print("ALL DONE ->", OUT)
