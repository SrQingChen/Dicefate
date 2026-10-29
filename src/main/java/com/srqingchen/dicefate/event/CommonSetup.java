package com.srqingchen.dicefate.event;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.registry.ModBlocks;
import com.srqingchen.dicefate.registry.ModEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/** 服务端属性注册（Boss 属性）。 */
@EventBusSubscriber(modid = DiceFate.MODID)
public final class CommonSetup {

    /** 0.4.0：服务器启动自检（数据注册表计数，异常即数据包布局问题）。 */
    @SubscribeEvent
    public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        try {
            var regs = event.getServer().registryAccess();
            com.srqingchen.dicefate.util.DFDebug.always("服务器启动自检: die={} pool_profile={} pack={} reaction={} synergy={} fate_rune={}",
                regs.lookupOrThrow(com.srqingchen.dicefate.data.DFRegistries.DIE).registryKeySet().size(),
                regs.lookupOrThrow(com.srqingchen.dicefate.data.DFRegistries.POOL_PROFILE).registryKeySet().size(),
                regs.lookupOrThrow(com.srqingchen.dicefate.data.DFRegistries.PACK).registryKeySet().size(),
                regs.lookupOrThrow(com.srqingchen.dicefate.data.DFRegistries.REACTION).registryKeySet().size(),
                regs.lookupOrThrow(com.srqingchen.dicefate.data.DFRegistries.SYNERGY).registryKeySet().size(),
                regs.lookupOrThrow(com.srqingchen.dicefate.data.DFRegistries.FATE_RUNE).registryKeySet().size());
        } catch (Exception ex) {
            com.srqingchen.dicefate.util.DFDebug.err("服务器启动自检失败: {}", ex.toString());
        }
    }

    @SubscribeEvent
    public static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.TWO_FACED_GAMBLER.get(),
            com.srqingchen.dicefate.entity.boss.TwoFacedGambler.createAttributes().build());
        event.put(ModEntities.MOLTEN_DIE_KING.get(),
            com.srqingchen.dicefate.entity.boss.MoltenDieKing.createAttributes().build());
        event.put(ModEntities.FROST_SILENCE_QUEEN.get(),
            com.srqingchen.dicefate.entity.boss.FrostSilenceQueen.createAttributes().build());
        event.put(ModEntities.NULLFACE.get(),
            com.srqingchen.dicefate.entity.boss.Nullface.createAttributes().build());
    }

    private CommonSetup() {}
}
