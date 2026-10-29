package com.srqingchen.dicefate.client.hud;

import com.srqingchen.dicefate.DFConfig;
import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.client.ClientFateData;
import com.srqingchen.dicefate.dice.DieElement;
import com.srqingchen.dicefate.network.ContestPayload;
import com.srqingchen.dicefate.registry.ModSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 攻击轨/受击轨 HUD（策划 1.7）：命中瞬间数值已定；最新一次直接覆盖，不排队、不自动消失。
 *
 * 0.3.0 动效重做（"动效没有"主诉根因：0.2.0 参数低于感知阈值——2tick/5px/闪白<0.05s）：
 * - 掷骰期 6 tick（0.3s）：骰面数字快速轮换（确定性伪随机）+ 高频抖动 + dice_roll 音效；
 * - 落定期 2 tick（0.1s）：数字切真值、骰子从 -14px 砸落定格、白闪 alpha 0.35 全程、
 *   震屏 ±3px 全程衰减；落定瞬间（since 跨越掷骰期边界）播 settle_impact 音效；
 * - 大胜/天命：金色描边脉冲 5 tick；Miss：灰色大字 2× + 碎裂骰面。
 * 计时基准 gameTime+partial（与 ClientFateData.onContest 记录的 gameTime 一致）。
 */
public final class ContestTrackHud {

    private static final float ROLL_TICKS = 6.0F;     // 掷骰期 0.3s
    private static final float SETTLE_TICKS = 2.0F;   // 落定期 0.1s
    private static final float TOTAL_TICKS = ROLL_TICKS + SETTLE_TICKS;

    /** 落定音效一次性标记（每轨）：attackSoundDone/defenseSoundDone。 */
    private static boolean attackSettleSoundDone = true;
    private static boolean defenseSettleSoundDone = true;

    /** 收包时调用（ClientFateData.onContest）：掷骰期开始音效。 */
    public static void onContestReceived(boolean attackTrack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        com.srqingchen.dicefate.util.DFDebug.log("HUD 轨道更新: track={} 播掷骰音效", attackTrack ? "攻击" : "受击");
        if (attackTrack) attackSettleSoundDone = false;
        else defenseSettleSoundDone = false;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.DICE_ROLL, 0.9F + mc.player.getRandom().nextFloat() * 0.2F));
    }

    private static void playSettle(ContestPayload p) {
        Minecraft mc = Minecraft.getInstance();
        boolean great = p.outcome().ordinal() >= 5;   // GREAT_WIN/DESTINY
        mc.getSoundManager().play(SimpleSoundInstance.forUI(
            great ? ModSounds.SETTLE_IMPACT_GREAT : ModSounds.SETTLE_IMPACT, 1.0F));
        if (p.outcome() == com.srqingchen.dicefate.combat.Outcome.CRASH) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.CONTEST_MISS, 0.9F));
        }
        for (byte t : p.triggers()) {
            if (t >= 1 && t <= 4) {   // PATTERN_STRAIGHT..PATTERN_DTRIPLE
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PATTERN, 1.2F));
                break;
            }
        }
    }

    private static boolean renderLogged = false;
    private static boolean trackDataLogged = false;

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!renderLogged) {
            renderLogged = true;
            com.srqingchen.dicefate.util.DFDebug.always("HUD 渲染器首次运行（图层已注册且生效）");
        }
        if (!trackDataLogged && (ClientFateData.attackTrack != null || ClientFateData.defenseTrack != null)) {
            trackDataLogged = true;
            com.srqingchen.dicefate.util.DFDebug.always("HUD 轨道数据就绪（拼点包已驱动渲染）");
        }
        Font font = mc.font;
        float now = renderTime(mc, delta);
        renderTrack(g, font, mc, ClientFateData.attackTrack, ClientFateData.attackTrackTime, now, true);
        renderTrack(g, font, mc, ClientFateData.defenseTrack, ClientFateData.defenseTrackTime, now, false);
    }

    private static float renderTime(Minecraft mc, DeltaTracker delta) {
        long game = mc.level == null ? 0 : mc.level.getGameTime();
        return game + delta.getGameTimeDeltaPartialTick(false);
    }

    private static void renderTrack(GuiGraphicsExtractor g, Font font, Minecraft mc,
                                    ContestPayload p, long time, float now, boolean attack) {
        if (p == null || time == Long.MIN_VALUE) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int w = 128, h = 46;
        // 攻击轨右缘贴热键栏左缘外 4px；受击轨左缘贴热键栏右缘外 4px（策划 1.7.2 中下两侧）
        int x = attack ? screenW / 2 - 95 - w : screenW / 2 + 95;
        int y = screenH - 88;

        float since = Math.max(0, now - time);
        boolean rolling = since < ROLL_TICKS;
        boolean settling = !rolling && since < TOTAL_TICKS;

        // 落定音效：跨越掷骰期边界的那一帧触发一次
        boolean done = attack ? attackSettleSoundDone : defenseSettleSoundDone;
        if (!rolling && !done) {
            com.srqingchen.dicefate.util.DFDebug.log("HUD 落定帧: track={} 战果={}", attack ? "攻击" : "受击", p.outcome());
            playSettle(p);
            if (attack) attackSettleSoundDone = true;
            else defenseSettleSoundDone = true;
        }

        // 掷骰期整体小抖动；落定期震屏 ±3px 全程衰减
        int shakeX = 0, shakeY = 0;
        if (rolling && DFConfig.HUD_SHAKE.get()) {
            shakeX = (int) Math.round(Math.sin(since * 43.0F) * 1.0F);
            shakeY = (int) Math.round(Math.cos(since * 37.0F) * 1.0F);
        } else if (settling && DFConfig.HUD_SHAKE.get()) {
            float decay = 1.0F - (since - ROLL_TICKS) / SETTLE_TICKS;
            shakeX = (int) Math.round(Math.sin(since * 27.0F) * 3 * decay);
            shakeY = (int) Math.round(Math.cos(since * 21.0F) * 2 * decay);
        }
        int bx = x + shakeX, by = y + shakeY;

        // 背景板
        g.fill(bx - 3, by - 3, bx + w + 3, by + h + 3, 0x90101018);
        g.fill(bx - 2, by - 2, bx + w + 2, by + h + 2, 0xB0202030);

        // 大胜/天命：金色描边脉冲（落定后 5 tick）
        float pulse = 0;
        if (p.outcome().ordinal() >= 5 && since >= ROLL_TICKS && since < ROLL_TICKS + 5) {
            pulse = 0.5F + 0.5F * (float) Math.sin((since - ROLL_TICKS) * 1.6F);
            int pc = (int) (0xB0 * pulse) + 0x40;
            g.fill(bx - 3, by - 3, bx + w + 3, by - 2, (pc << 24) | 0xFFD966);
            g.fill(bx - 3, by + h + 2, bx + w + 3, by + h + 3, (pc << 24) | 0xFFD966);
            g.fill(bx - 3, by - 2, bx - 2, by + h + 2, (pc << 24) | 0xFFD966);
            g.fill(bx + w + 2, by - 2, bx + w + 3, by + h + 2, (pc << 24) | 0xFFD966);
        }

        // 落定冲击闪白（alpha 0.35，落定期全程）
        if (settling) {
            int a = (int) (0.35F * 255);
            g.fill(bx - 2, by - 2, bx + w + 2, by + h + 2, (a << 24) | 0xFFFFFF);
        }

        // 标题
        Component title = attack
            ? Component.translatable("hud.dicefate.attack")
            : Component.translatable("hud.dicefate.defense");
        g.text(font, title, bx + 2, by + 2, 0xFFD9A441, false);

        // 双方骰面（掷骰期轮换 → 落定砸落 → 定格）
        var atk = p.attackerRolls();
        var def = p.defenderRolls();
        int diceY = by + 12;
        renderDiceRow(g, mc, atk, bx, diceY, since, rolling, settling);
        renderDiceRow(g, mc, def, bx, diceY + 12, since, rolling, settling);

        // 总点数 + 幸运角标（掷骰期隐藏总点，落定后揭示）
        String atkTotal = rolling ? "?" : String.valueOf(p.attackerTotal());
        String defTotal = rolling ? "?" : String.valueOf(p.defenderTotal());
        g.text(font, atkTotal, bx + 2, by + h - 10, 0xFFFFE9B0, true);
        int dx = bx + 2 + font.width(atkTotal) + 2;
        if (!rolling && p.attackerLuck() != 0) {
            g.text(font, (p.attackerLuck() > 0 ? "+" : "") + p.attackerLuck(), dx, by + h - 10, 0xFF7FE3B2, false);
        }
        g.text(font, defTotal, bx + 48, by + h - 10, 0xFFB8C7D9, true);
        int dx2 = bx + 48 + font.width(defTotal) + 2;
        if (!rolling && p.defenderLuck() != 0) {
            g.text(font, (p.defenderLuck() > 0 ? "+" : "") + p.defenderLuck(), dx2, by + h - 10, 0xFF7FE3B2, false);
        }

        // 战果印章 + 伤害 + 命势/命定（掷骰期不显示）
        if (rolling) {
            g.text(font, "···", bx + 78, by + h - 10, 0xFF8892A0, false);
            return;
        }
        OutcomeInfo info = outcomeInfo(p.outcome());
        g.text(font, info.text(), bx + 78, by + h - 10, info.color(), false);
        boolean hasPoiseLine = false;
        if (p.outcome() == com.srqingchen.dicefate.combat.Outcome.CRASH) {
            // Miss 灰色大字（2 倍缩放）
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(bx + 82, by + h - 32);
            pose.scale(2.0F, 2.0F);
            g.text(font, "Miss!", 0, 0, 0xFFB8B8B8, true);
            pose.popMatrix();
        } else {
            if (p.finalDamage() > 0) {
                g.text(font, String.valueOf((int) Math.ceil(p.finalDamage())), bx + 78, by + h - 20, 0xFFFFD966, false);
            }
            if (p.poiseDamage() > 0) {
                // 0.3.0：底衬防糊边
                g.fill(bx + 1, by + h + 3, bx + 40, by + h + 12, 0x80101018);
                g.text(font, "命势-" + p.poiseDamage(), bx + 2, by + h + 4, 0xFFC77AD4, false);
                hasPoiseLine = true;
            }
            if (p.decreeGain() > 0) {
                g.fill(bx + 41, by + h + 3, bx + 80, by + h + 12, 0x80101018);
                g.text(font, "命定+" + p.decreeGain(), bx + 42, by + h + 4, 0xFF8E4E9E, false);
                hasPoiseLine = true;
            }
        }

        // 触发图标（短词）
        int tx = bx + 2;
        int ty = hasPoiseLine ? by + h + 14 : by + h + 4;
        for (byte t : p.triggers()) {
            ContestPayload.Trigger trig;
            try {
                trig = ContestPayload.Trigger.values()[t];
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
            String word = triggerWord(trig);
            if (word == null) continue;
            g.text(font, word, tx, ty, 0xFF7FE3B2, false);
            tx += font.width(word) + 3;
            if (tx > bx + w - 20) break;
        }
        // 多目标
        if (p.targetCount() > 1) {
            g.text(font, "×" + p.targetCount(), bx + w - 16, by + 2, 0xFFFFE9B0, false);
        }
    }

    private static void renderDiceRow(GuiGraphicsExtractor g, Minecraft mc,
                                      java.util.List<ContestPayload.DieEntry> rolls,
                                      int x, int y, float since, boolean rolling, boolean settling) {
        int dx = x + 2;
        int idx = 0;
        for (ContestPayload.DieEntry e : rolls) {
            int value;
            int jitterY = 0, jitterX = 0;
            if (rolling) {
                // 掷骰期：数字确定性轮换 + 高频抖动
                value = spinValue(since, idx, e.face());
                jitterX = (int) Math.round(Math.sin(since * 47.0F + idx * 2.1F) * 1.5F);
                jitterY = (int) Math.round(Math.cos(since * 41.0F + idx * 1.7F) * 1.5F);
            } else {
                value = e.value();
                if (settling) {
                    // 落定期：从 -14px 砸落（ease-in）+ 收敛抖动
                    float t = (since - ROLL_TICKS) / SETTLE_TICKS;   // 0→1
                    float drop = (1 - t * t) * -14.0F;
                    jitterY = (int) drop;
                    jitterX = (int) Math.round(Math.sin(since * 31.0F + e.value()) * 1.5F * (1 - t));
                }
            }
            Identifier tex = dieFaceTexture(e, value);
            int drawV = Math.max(1, Math.min(24, value));
            g.blit(RenderPipelines.GUI_TEXTURED, tex, dx + jitterX, y + jitterY, 0, 0, 10, 10, 16, 16,
                e.cracked() ? 0x80FFFFFF : -1);
            // 数字
            if (drawV > 0) {
                g.text(mc.font, String.valueOf(drawV), dx + jitterX + 6, y + jitterY + 6, 0xFF202028, false);
            }
            dx += 11;
            idx++;
        }
    }

    /** 掷骰期轮换值（确定性：同帧同槽同值，帧间快速变化）。 */
    private static int spinValue(float since, int idx, int face) {
        int h = (int) (since * 16.0F) * 31 + idx * 1013904223;
        h ^= h >>> 13;
        h *= 0x5BD1E995;
        h ^= h >>> 15;
        return 1 + Math.floorMod(h, Math.max(1, face));
    }

    private static Identifier dieFaceTexture(ContestPayload.DieEntry e, int value) {
        int v = Math.max(1, Math.min(24, value));
        String elem = elementSuffix(e.elementOrdinal());
        return DiceFate.id("textures/gui/dice/face_" + v + (elem.isEmpty() ? "" : "_" + elem) + ".png");
    }

    private static String elementSuffix(byte elementOrdinal) {
        DieElement[] values = DieElement.values();
        if (elementOrdinal < 0 || elementOrdinal >= values.length) return "";
        return switch (values[elementOrdinal]) {
            case EMBER -> "ember";
            case FROST -> "frost";
            case STORM -> "storm";
            case VENOM -> "venom";
            case RADIANT -> "radiant";
            case UMBRAL -> "umbral";
            default -> "";
        };
    }

    private record OutcomeInfo(Component text, int color) {}

    private static OutcomeInfo outcomeInfo(com.srqingchen.dicefate.combat.Outcome o) {
        return switch (o) {
            case CRASH -> new OutcomeInfo(Component.translatable("hud.dicefate.outcome.crash"), 0xFFB8B8B8);
            case LOSE -> new OutcomeInfo(Component.translatable("hud.dicefate.outcome.lose"), 0xFFFF7A7A);
            case TIE -> new OutcomeInfo(Component.translatable("hud.dicefate.outcome.tie"), 0xFFE8E8E8);
            case NARROW_WIN -> new OutcomeInfo(Component.translatable("hud.dicefate.outcome.narrow"), 0xFFFFE9B0);
            case WIN -> new OutcomeInfo(Component.translatable("hud.dicefate.outcome.win"), 0xFF7FE3B2);
            case GREAT_WIN -> new OutcomeInfo(Component.translatable("hud.dicefate.outcome.great"), 0xFFFFD966);
            case DESTINY -> new OutcomeInfo(Component.translatable("hud.dicefate.outcome.destiny"), 0xFFFF9A3D);
        };
    }

    static int elementColor(DieElement e) {
        return switch (e) {
            case EMBER -> 0xFFB04A2A;
            case FROST -> 0xFF5A9BD4;
            case STORM -> 0xFFB8A83D;
            case VENOM -> 0xFF6FA83D;
            case RADIANT -> 0xFFE8D44D;
            case UMBRAL -> 0xFF6B4E8E;
            default -> 0xFF8A7A5C;
        };
    }

    private static String triggerWord(ContestPayload.Trigger t) {
        return switch (t) {
            case PATTERN_STRAIGHT -> "顺";
            case PATTERN_TRIPLE -> "豹";
            case PATTERN_DSTRAIGHT -> "天顺";
            case PATTERN_DTRIPLE -> "天豹";
            case REACTION -> "反应";
            case SYNERGY -> "协同";
            case SKILL_ECHO -> "回响";
            case SKILL_PURSUIT -> "追击";
            case SKILL_WARD -> "护命";
            case SKILL_BREAKER -> "破势";
            case SKILL_REVERSE -> "逆命";
            case SKILL_GREED -> "贪命";
            case SKILL_LOCK -> "定命";
            case SKILL_FAVOR -> "命佑";
            case PITY -> "命佑";
            case SPIRIT -> "骰灵";
            case CASHOUT -> "兑现";
            case LOCK -> "锁定";
            case EXHAUSTED -> "散竭";
            case FATEBREAK -> "命破";
            case WOUND -> "命损";
            case POISE_BREAK -> "破势";
            case PERFECT_BLOCK -> "完美格挡";
            case SHIELD_BREAK -> "破防";
            case MOMENTUM_LOSS -> "骰势-";
            case MOMENTUM_NEGATE -> "免扣";
            case LUCKY -> "硬币";
            default -> null;
        };
    }

    private ContestTrackHud() {}
}
