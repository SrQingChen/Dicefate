package com.srqingchen.dicefate.client.hud;

import com.srqingchen.dicefate.client.ClientFateData;
import com.srqingchen.dicefate.network.FateStatePayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * 骰池状态 HUD（策划 8.7.2 首选位：屏幕左上角）。
 * 0.3.0：从"物品栏左侧"移至左上——原位置与攻击轨面板 [W/2-223,W/2-95] 完全重叠，
 * 拼点时骰池列被轨道背景盖住（HUD 错位主诉根因）；左上与攻击/受击两轨零冲突。
 * 纵列骰位（基础+专属）、骰势条+层级刻度+姿态、兑现提示、技能 CD、骰包。
 */
public final class FateStatusHud {

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        ClientFateData.PoolView pool = poolView();
        FateStatePayload st = ClientFateData.fateState;
        Font font = mc.font;

        // 左上角锚点（避开原版左上效果图标行：下移 2 行留白）
        int x = 4;
        int y = 4;

        // 标题行：姿态 + 骰势数字
        if (st != null) {
            int momentum = st.bars().momentum();
            int tierColor = tierColor(momentum);
            String stance = stanceName(st.meta().stanceOrdinal());
            g.text(font, stance + " " + momentum, x, y, tierColor, false);
            y += 10;

            // 骰势条 + 层级刻度
            int barW = 60;
            g.fill(x, y, x + barW, y + 5, 0xFF15151E);
            int filled = (int) (barW * Math.min(1.0F, momentum / 100.0F));
            g.fill(x + 1, y + 1, x + 1 + Math.max(0, Math.min(barW - 2, filled - 2)), y + 4, tierColor);
            for (int t : new int[]{25, 50, 75, 100}) {
                int tx = x + (int) (barW * t / 100.0);
                g.fill(tx, y, tx + 1, y + 5, 0x80000000);
            }
            y += 8;

            // 兑现提示
            if (st.bars().cashoutArmed()) {
                g.text(font, Component.translatable("hud.dicefate.cashout_ready"),
                    x, y, 0xFFFFD966, true);
                y += 10;
            }

            // 骰包剩余
            if (st.extras() != null && !st.extras().pack().isEmpty()) {
                g.text(font, "骰包×" + st.extras().packRemaining(), x, y, 0xFF7FE3B2, false);
                y += 10;
            }
            y += 2;
        }

        // 骰位纵列（基础 + 专属）
        if (pool != null) {
            int baseCount = Math.min(pool.base().size(), st == null ? 6 : st.meta().baseSlots());
            int dy = y;
            for (int i = 0; i < pool.base().size() && i < baseCount; i++) {
                var stack = pool.base().get(i);
                g.fill(x - 1, dy - 1, x + 17, dy + 17, 0x80202030);
                if (!stack.isEmpty()) {
                    g.item(stack, x, dy);
                }
                dy += 18;
            }
            if (!pool.add().isEmpty()) { slotBg(g, x, dy); g.item(pool.add(), x, dy); dy += 18; }
            if (!pool.mul().isEmpty()) { slotBg(g, x, dy); g.item(pool.mul(), x, dy); dy += 18; }
            if (!pool.skill0().isEmpty()) {
                slotBg(g, x, dy);
                g.item(pool.skill0(), x, dy);
                drawCooldown(g, font, st == null ? 0 : st.meta().skillCd0(), x, dy);
                dy += 18;
            }
            if (!pool.skill1().isEmpty()) {
                slotBg(g, x, dy);
                g.item(pool.skill1(), x, dy);
                drawCooldown(g, font, st == null ? 0 : st.meta().skillCd1(), x, dy);
            }
        }
    }

    private static void slotBg(net.minecraft.client.gui.GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0x80202030);
    }

    private static int tierColor(int momentum) {
        if (momentum >= 100) return 0xFFFFD966;
        if (momentum >= 75) return 0xFFE89A3D;
        if (momentum >= 50) return 0xFF7FE3B2;
        if (momentum >= 25) return 0xFF6FB3D4;
        return 0xFF505868;
    }

    private static void drawCooldown(GuiGraphicsExtractor g, Font font, long cdUntil, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        if (cdUntil <= now) return;
        int secs = (int) Math.ceil((cdUntil - now) / 20.0);
        String s = secs + "s";
        g.fill(x, y + 9, x + 16, y + 16, 0x90000000);
        g.text(font, s, x + 1, y + 9, 0xFFE8E8E8, false);
    }

    private static String stanceName(int ordinal) {
        return switch (ordinal) {
            case 1 -> "背水";
            case 2 -> "守势";
            case 3 -> "诡道";
            default -> "均衡";
        };
    }

    private static ClientFateData.PoolView poolView() {
        var sync = ClientFateData.pool;
        if (sync == null) return null;
        return new ClientFateData.PoolView(sync.base(), sync.add(), sync.mul(), sync.skill0(), sync.skill1());
    }

    private FateStatusHud() {}
}
