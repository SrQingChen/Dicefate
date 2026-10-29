<div align="center">

<img src="docs/assets/banner.png" width="100%" alt="骰掷命术 Dicefate — 命数潮汐之下，众生以骰局决胜负"/>

# 骰掷命术 · DICEFATE

**万物皆骰，命数自搏。**

*Every blow lands as a dice contest. Read the board, build your pool, break poise — and out-roll fate itself.*

<a href="https://github.com/SrQingChen/Dicefate/releases">
  <img alt="version" src="https://img.shields.io/badge/version-0.3.2%20beta-C9A24D?style=flat-square"/>
</a>
<img alt="minecraft" src="https://img.shields.io/badge/Minecraft-26.1.2-2EA88F?style=flat-square"/>
<img alt="neoforge" src="https://img.shields.io/badge/NeoForge-26.1.2%2B-1F1F1F?style=flat-square"/>
<img alt="java" src="https://img.shields.io/badge/Java-25-7A5A22?style=flat-square"/>
<a href="LICENSE">
  <img alt="license" src="https://img.shields.io/badge/license-GPL--3.0-blue?style=flat-square"/>
</a>
<a href="https://github.com/SrQingChen/Dicefate">
  <img alt="github" src="https://img.shields.io/badge/GitHub-SrQingChen%2FDicefate-181717?style=flat-square&logo=github"/>
</a>

**中文** · [English](#-english-overview)

</div>

---

## 🎲 这是什么

一套挂在 Minecraft 整个战斗系统上的**命数骰局体系**：从你挥出的第一刀，到僵尸扑来的爪击，再到 Boss 的熔铸轰击——**每一次命中都是一场双方同 tick 完成的骰局对抗**。掷骰、拼点、破势、兑现，0.1 秒落定，命运在 HUD 上翻牌揭晓。

你不再是"点击就赢"的战士，而是一个读局、押注、构筑骰池的**赌命者**。

> A world-grade fate-dice system wired into Minecraft's combat: every hit becomes a same-tick dice contest between attacker and defender. Build your dice pool, chain poise breaks, cash in momentum — and out-roll fate itself. Full English overview [below](#-english-overview).

## ✨ 核心机制

| 系统 | 一句话 |
|---|---|
| **🎲 拼点 Dice Contest** | 所有玩家有源伤害（近战/横扫/箭/投索/爆炸/召唤物）与防御方完整拼点，同 tick 掷骰结算，怪物远程轻拼，同 tick 多目标共享一次攻击掷骰 |
| **⚔️ 命势 Poise** | 破势 → 命气散竭（全骰碎裂掷 0）→ 命定 → 命破真伤 → 命损易伤，一条环环相扣的压制链，附恢复/防循环封顶 |
| **📈 骰势 Momentum** | 连击积累 0–100，四层被动加持，`V` 键一瞬兑现（max vs 0、6s 窗口、保命兜底） |
| **🧘 姿态 Stances** | 均衡 / 背水 / 守势 / 诡道四条曲线 + 精通，定义你的赌风 |
| **🀄 技能骰 Skill Dice** | 8 族 × 3 阶（回响/追击/护命/破势/逆命/贪命/定命/命佑），CD 随掷出的点数浮动 |
| **🧩 骰池构筑 Deckbuilding** | 初始 1 骰位 d6，扩容符 I–V 乱序解锁至 6 位；专属加算/乘算/技能槽；顺子/豹子/天命升格牌型；4 元素反应；10 种协同 |
| **🎲 修正系统** | 保底、定命、骰灵（`G` 键开关，自动 ±1）、16 种骰包随时换运 |
| **💰 命数经济** | 命砂/命尘/命核/星屑/命运碎片全链材料，骰运台 12 条命纹，鉴定/重鉴/磨损/修复，骰商村民 + 盲盒 |

## 📦 内容量（1.0 全量已实现）

| 类别 | 数量 |
|---|---:|
| 骰子（基础 15 · 元素 18 · 技能 24 · 加算 6 · 乘算 6 · 唯一 10 · 诅咒 4） | **83** |
| 材料 / 工具 / 消耗品 | 51 |
| 骰包 / 药剂 / 食物 / 盲盒 | 21 |
| 功能方块（刻骰台 / 骰运台 / 修复台 / 命线织机 / 供奉龛…） | 8 |
| 装备（骰刃 / 投索 / 盾 / 护甲 2 套 / Curios 饰品 7） | 25 |
| 原版生物命盘档案 | 18 |
| 自定义 Boss（双面赌徒 / 熔面骰王 / 霜寂女王 / 零面者） | 4 |
| 世界结构（命盘祭坛 / 倾颓赌坊 / 双面金库 / 熔面赌窟 / 冰封赌宫 / 无面赌场） | 6 |
| Advancement 进度（6 分支） | 33 |

## 🎮 上手 30 秒

| 按键 | 功能 |
|---|---|
| **B** | 打开命盘 —— 把背包里的骰装进骰位，切换姿态，保存 3 套预设 |
| **V** | 兑现 —— 骰势将满 / 已满时，把连击一次性兑换成爆发 |
| **G** | 骰灵开关 —— 自动修正最低骰 +1，消耗灵粮 |

**开局循环**：造一把骰 → `B` 装上 → 打怪拼点攒骰势 → `V` 兑现爆发 → 用命砂在骰运台洗命纹、扩骰位、换更强的骰。手持骰胚右键**刻骰台**造骰，手持骰右键**骰运台**洗纹，**命线织机**存取预设，**未鉴定骰**右键开盒赌命。

## 🧊 真·多面体骰子

所有 83 种骰子都是**精确多面体网格**（d4 / d6 / d8 / d10 / d12 / d20 / d24），按数学坐标生成——d10 五角偏方面体、d24 小斜方截半立方体极对偶——32×32 编号面纹理按材质调色（骨 / 铁 / 秘银 / 命金 / 星辉 / 六元素），HUD 物品栏中渲染为真实 3D 骰子。

## 🔗 依赖

- **必需**：NeoForge 26.1.2+
- **可选**：[Curios](https://curseforge.com/minecraft/mc-mods/curios)（专属「命具」槽位 + ring/belt）、[Jade](https://curseforge.com/minecraft/mc-mods/jade)（命势面板，0.4 接入中）、[JECharacters](https://curseforge.com/minecraft/mc-mods/jecharacters)（拼音搜索）
- 未安装 Curios 时，饰品效果自动降级为背包携带生效，功能不缺失。

## 🗺 路线图

- ✅ **0.3.2**（当前）—— 核心拼点管线实机验证通过；83 骰 / 4 Boss / 6 结构 / 33 进度全量落地
- 🔧 **0.4.0**（开发中）—— 世界飘字渲染修复、Jade 命势面板联动、创造模式拼点提示、诊断日志默认关闭
- 🎯 **1.0** —— 全屏工作站 GUI 精修、骰面美术终稿、平衡性整轮调参

> ⚠️ 创造/旁观模式下不参与拼点，属设计行为（策划 2.1.3）——想体验完整机制请用生存模式。

## 🛠 从源码构建

```bash
# 可选联动编译：将 Curios / Jade / JECharacters 的 jar 放入 libs/（不加也能构建）
gradlew.bat build        # 产物 build/libs/dicefate-<version>.jar
gradlew.bat runClient    # 开发客户端
```

开发事实清单见 [`docs/API_FACTS.md`](docs/API_FACTS.md)（26.1.2 反编译核实的 API 笔记）。

## 📄 许可

[GPL-3.0](LICENSE) © SrQingChen

---

## 🇬🇧 English Overview

**Dicefate** is a world-grade fate-dice combat system for Minecraft **26.1.2 (NeoForge)**. Every attack — yours or the mob's — resolves as a same-tick dice contest: both sides roll their pools, totals are compared, and the outcome reshapes the fight in a 0.1-second reveal.

- **Dice Contests** — all player-sourced damage (melee, sweeps, arrows, throws, explosions, minions) runs the full contest pipeline; mob ranged attacks get light contests; multi-hit sweeps share one attack roll.
- **Poise & Fatebreak** — break an opponent's poise and they enter *Fate Exhaustion*: every die shatters and rolls 0. Chain into guaranteed fate strikes and vulnerability windows.
- **Momentum** — build 0–100 momentum through combat, then press `V` to cash it in: maxed dice vs. zeroes, a 6-second power window, with a life-saving floor.
- **Deckbuilding** — grow from one d6 slot to six; dedicated additive / multiplier / skill slots; poker-hand bonuses (straights, triples, destiny upgrades); 4 elemental reactions; 10 synergies.
- **Skill Dice** — 8 families × 3 tiers, with cooldowns that scale off the number you rolled.
- **Real Polyhedral Dice** — all 83 dice are exact 3D meshes (d4–d24) with numbered faces, rendered in your hotbar.
- **Content** — 83 dice, 51 items, 8 workstations, 25 equipment pieces, 18 vanilla mob fate-profiles, 4 bosses, 6 structures, 33 advancements, a dice-merchant economy with mystery boxes.
- **Optional integrations** — Curios (dedicated charm slots), Jade (0.4), JECharacters; degrades gracefully when absent.

**Keys**: `B` fate board · `V` cash-in · `G` dice-spirit toggle. Note: creative/spectator mode skips contests by design — test in survival.

<div align="center">

**⭐ If the dice favor you, star the repo ⭐**

<img src="docs/assets/logo.png" width="128" alt="Dicefate logo"/>

</div>
