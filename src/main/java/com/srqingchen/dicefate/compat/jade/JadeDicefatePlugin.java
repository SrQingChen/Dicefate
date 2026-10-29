package com.srqingchen.dicefate.compat.jade;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.client.ClientFateData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.ITooltip;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 兼容层（策划 3.1.8 / 8.10.2，0.4.0 落地）：
 * 准星对准生物时在 Jade 面板显示命势/命定/命气散竭/命损。
 * 数据源复用 ClientFateData.poiseEntries（PoiseSyncPayload 已同步，服务端权威）。
 * 类位于 compat.jade 包且仅在 Jade 在装时被 @WailaPlugin 注解扫描实例化，
 * 类体只引用 Jade API 与客户端公共类，无 Jade 时不会被加载（延迟到 Jade 扫描）。
 */
@WailaPlugin
public final class JadeDicefatePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(new FateProvider(), LivingEntity.class);
    }

    public static final class FateProvider implements IComponentProvider<EntityAccessor> {
        @Override
        public net.minecraft.resources.Identifier getUid() {
            return DiceFate.id("fate");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            Entity entity = accessor.getEntity();
            var entry = ClientFateData.poiseEntries.get(entity.getId());
            if (entry == null || entry.maxPoise() <= 0) return;

            // 命势条（颜色随状态：散竭紫 / 恢复蓝 / 常态金）
            ChatFormatting color = switch (entry.state()) {
                case 1 -> ChatFormatting.LIGHT_PURPLE;   // EXHAUSTED
                case 2 -> ChatFormatting.AQUA;           // RECOVERING
                default -> ChatFormatting.GOLD;
            };
            tooltip.add(Component.translatable("tooltip.dicefate.jade.poise",
                bar(entry.poise() / (float) entry.maxPoise(), 10),
                entry.poise(), entry.maxPoise()).withStyle(color));

            // 命定细条（仅散竭时）
            if (entry.state() == 1 && entry.decreeThreshold() > 0) {
                tooltip.add(Component.translatable("tooltip.dicefate.jade.decree",
                    bar(entry.decree() / (float) entry.decreeThreshold(), 8),
                    entry.decree(), entry.decreeThreshold()).withStyle(ChatFormatting.DARK_PURPLE));
            }
            // 命气散竭倒计时
            if (entry.state() == 1) {
                tooltip.add(Component.translatable("tooltip.dicefate.jade.exhausted",
                    String.format("%.1f", entry.woundTicks() / 20.0F)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            // 命损（受伤 +15%）
            if (entry.woundTicks() > 0 && entry.state() != 1) {
                tooltip.add(Component.translatable("tooltip.dicefate.jade.wound",
                    String.format("%.1f", entry.woundTicks() / 20.0F)).withStyle(ChatFormatting.DARK_RED));
            }
        }

        private static String bar(float frac, int cells) {
            int filled = Math.round(Math.max(0, Math.min(1, frac)) * cells);
            return "█".repeat(filled) + "░".repeat(cells - filled);
        }
    }

    private JadeDicefatePlugin() {}
}
