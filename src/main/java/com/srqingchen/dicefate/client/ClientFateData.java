package com.srqingchen.dicefate.client;

import com.srqingchen.dicefate.network.CombatTextPayload;
import com.srqingchen.dicefate.network.ContestPayload;
import com.srqingchen.dicefate.network.FateStatePayload;
import com.srqingchen.dicefate.network.PoiseSyncPayload;
import com.srqingchen.dicefate.network.PoolSyncPayload;
import com.srqingchen.dicefate.network.PouchSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户侧命数数据缓存。所有数据来自服务端包；客户端只渲染。
 * 0.2.0：全部计时改为世界 gameTime（修复服务器 tickCount 与 gameTime 零点错位
 * 导致的命势条/CD 永不显示）；轨道按接收者视角判定（PvP/旁观者不再错轨）；
 * 退世界清空缓存；飘字按实体 ID 精确挂靠。
 */
public final class ClientFateData {
    /** 攻击轨/受击轨最新拼点（gameTime 记录，落定动画从此起算）。 */
    public static ContestPayload attackTrack;
    public static long attackTrackTime = Long.MIN_VALUE;
    public static ContestPayload defenseTrack;
    public static long defenseTrackTime = Long.MIN_VALUE;

    public static FateStatePayload fateState;
    public static PoolSyncPayload pool;
    public static List<ItemStack> pouch = List.of();

    /** 附近实体命势（entityId → entry，过期剔除）。 */
    public static final Map<Integer, PoiseSyncPayload.Entry> poiseEntries = new HashMap<>();

    /** 飘字队列（每实体最多 12 条，带出生 gameTime）。 */
    public static final List<TimedText> combatTexts = new ArrayList<>();

    public record TimedText(CombatTextPayload payload, long bornAt,
                            double x, double y, double z,
                            net.minecraft.world.entity.EntityType<?> type) {}

    /** 按实体 ID 取飘字（已知 ID 时）。 */
    public static List<TimedText> textsFor(int entityId, long nowGameTime) {
        List<TimedText> out = new ArrayList<>();
        for (TimedText t : combatTexts) {
            if (t.payload().entityId() == entityId && nowGameTime - t.bornAt() < 24) out.add(t);
        }
        return out;
    }

    /** 按渲染状态近似匹配飘字（类型一致 + 位置 0.5 格内；26.1 渲染状态无实体 ID）。 */
    public static List<TimedText> textsNear(net.minecraft.world.entity.EntityType<?> type,
                                            double x, double y, double z, long nowGameTime) {
        List<TimedText> out = new ArrayList<>();
        for (TimedText t : combatTexts) {
            if (t.type() != type) continue;
            if (nowGameTime - t.bornAt() >= 24) continue;
            double dx = t.x() - x, dy = t.y() - y, dz = t.z() - z;
            if (dx * dx + dy * dy + dz * dz < 0.25) out.add(t);
        }
        return out;
    }

    private ClientFateData() {}

    public static void onContest(ContestPayload payload) {
        com.srqingchen.dicefate.util.DFDebug.always("客户端收到拼点包: track={} outcome={}",
            payload.track(), payload.outcome());
        Minecraft mc = Minecraft.getInstance();
        // 0.2.0：轨道按接收者视角（我打人→攻击轨；我被打→受击轨；旁观者按 payload 轨）
        boolean iAmAttacker = mc.player != null && payload.attackerId() == mc.player.getId();
        boolean iAmDefender = mc.player != null && payload.defenderId() == mc.player.getId();
        int track;
        if (iAmAttacker && !iAmDefender) track = 0;
        else if (iAmDefender && !iAmAttacker) track = 1;
        else track = payload.track();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        if (track == 0) {
            attackTrack = payload;
            attackTrackTime = now;
        } else {
            defenseTrack = payload;
            defenseTrackTime = now;
        }
        com.srqingchen.dicefate.client.hud.ContestTrackHud.onContestReceived(track == 0);
    }

    public static void onFateState(FateStatePayload payload) {
        fateState = payload;
    }

    public static void onPoiseSync(PoiseSyncPayload payload) {
        // 低频：仅首个条目变化时记录（诊断期保留，确认头顶条通路）
        com.srqingchen.dicefate.util.DFDebug.log("客户端收到命势同步: 条目数={} 首条实体={}",
            payload.entries().size(),
            payload.entries().isEmpty() ? "-" : payload.entries().get(0).entityId());
        for (PoiseSyncPayload.Entry e : payload.entries()) {
            poiseEntries.put(e.entityId(), e);
        }
    }

    public static void onCombatText(CombatTextPayload payload) {
        com.srqingchen.dicefate.util.DFDebug.log("客户端收到飘字包: 实体={} 类型={} 值={}",
            payload.entityId(), payload.kind(), payload.value());
        long now = Minecraft.getInstance().level == null ? 0
            : Minecraft.getInstance().level.getGameTime();
        Minecraft mc = Minecraft.getInstance();
        double ex = 0, ey = 0, ez = 0;
        net.minecraft.world.entity.EntityType<?> type = null;
        if (mc.level != null) {
            var entity = mc.level.getEntity(payload.entityId());
            if (entity != null) {
                ex = entity.getX(); ey = entity.getY(); ez = entity.getZ();
                type = entity.getType();
            }
        }
        if (type == null && !combatTexts.isEmpty()) {
            // 实体已卸载：沿用同 ID 上一条的位置
            for (int i = combatTexts.size() - 1; i >= 0; i--) {
                if (combatTexts.get(i).payload().entityId() == payload.entityId()) {
                    TimedText prev = combatTexts.get(i);
                    ex = prev.x(); ey = prev.y(); ez = prev.z(); type = prev.type();
                    break;
                }
            }
        }
        combatTexts.add(new TimedText(payload, now, ex, ey, ez, type));
        // 每实体最多 12 条
        Map<Integer, Integer> counts = new HashMap<>();
        for (TimedText t : combatTexts) counts.merge(t.payload().entityId(), 1, Integer::sum);
        for (Map.Entry<Integer, Integer> en : counts.entrySet()) {
            int over = en.getValue() - 12;
            if (over > 0) {
                var it = combatTexts.iterator();
                while (it.hasNext() && over > 0) {
                    if (it.next().payload().entityId() == en.getKey()) {
                        it.remove();
                        over--;
                    }
                }
            }
        }
        if (combatTexts.size() > 96) {
            combatTexts.subList(0, combatTexts.size() - 96).clear();
        }
        // 过期清理
        combatTexts.removeIf(t -> now - t.bornAt() > 24);
    }

    public static void onPoolSync(PoolSyncPayload payload) {
        pool = payload;
    }

    public static void onPouchSync(PouchSyncPayload payload) {
        pouch = payload.slots();
    }

    public record PoolView(java.util.List<ItemStack> base,
                           ItemStack add,
                           ItemStack mul,
                           ItemStack skill0,
                           ItemStack skill1) {}

    /** HUD 用：当前装备的基础骰数量。 */
    public static int equippedBaseCount() {
        if (pool == null) return 0;
        int n = 0;
        for (ItemStack s : pool.base()) if (!s.isEmpty()) n++;
        return n;
    }

    /** 0.2.0：退世界/换服清空（实体 ID 跨世界复用会错挂）。 */
    public static void clearAll() {
        attackTrack = null;
        attackTrackTime = Long.MIN_VALUE;
        defenseTrack = null;
        defenseTrackTime = Long.MIN_VALUE;
        fateState = null;
        pool = null;
        pouch = List.of();
        poiseEntries.clear();
        combatTexts.clear();
    }
}
