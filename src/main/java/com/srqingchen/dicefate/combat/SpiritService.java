package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.DFConfig;
import com.srqingchen.dicefate.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * 骰灵（规划 3.7，自动修正模式）：
 * 开启后每次涉及玩家的拼点，自动消耗 1 灵粮，将自己点数最低的基础骰 +1
 * （诅咒面值骰修正方向仍为有利方向）。等级提高修正量概率。
 */
public final class SpiritService {
    private SpiritService() {}

    /** 尝试自动修正：成功返回修正值（1-3），失败返回 0。
     *  0.2.0：骰灵铃（spirit_bell）15% 概率本次修正不消耗灵粮。
     *  0.3.0：灵粮消耗量接 DFConfig.SPIRIT_FEED_PER_USE。 */
    public static int tryNudge(ServerPlayer player, int spiritLevel, RandomSource rng) {
        boolean bellFree = com.srqingchen.dicefate.compat.CuriosCompat.hasSpiritBell(player)
            && rng.nextDouble() < 0.15;
        if (!bellFree) {
            int need = DFConfig.SPIRIT_FEED_PER_USE.get();
            for (int i = 0; i < need; i++) {
                if (!consumeFeed(player)) return 0;
            }
        }
        return switch (spiritLevel) {
            case 1 -> 1;
            case 2 -> rng.nextDouble() < 0.30 ? 2 : 1;
            default -> {
                double d = rng.nextDouble();
                if (d < 0.10) yield 3;
                if (d < 0.50) yield 2;
                yield 1;
            }
        };
    }

    /** 消耗背包 1 灵粮。 */
    public static boolean consumeFeed(ServerPlayer player) {
        var feed = ModItems.SPIRIT_FEED;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(feed.get())) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** 背包灵粮计数（HUD 显示）。 */
    public static int countFeed(ServerPlayer player) {
        var feed = ModItems.SPIRIT_FEED;
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(feed.get())) n += s.getCount();
        }
        return n;
    }
}
