package com.srqingchen.dicefate.util;

import com.srqingchen.dicefate.DiceFate;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 进度直授服务（0.3.0 F1）：
 * 23 个进度 JSON 的触发器改为 minecraft:impossible（原 minecraft:tick 进世界即全部达成），
 * 由服务端在各结算点直接 award 全部 criteria。
 */
public final class AdvancementGrant {

    public static void grant(ServerPlayer player, String path) {
        if (player == null) return;
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if (level.getServer() == null) return;
        var holder = level.getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(MODID, path));
        if (holder == null) return;
        var adv = player.getAdvancements();
        for (String criterion : holder.value().criteria().keySet()) {
            adv.award(holder, criterion);
        }
    }

    private static final String MODID = DiceFate.MODID;

    private AdvancementGrant() {}
}
