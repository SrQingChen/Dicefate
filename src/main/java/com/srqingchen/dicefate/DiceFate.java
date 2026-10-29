package com.srqingchen.dicefate;

import com.srqingchen.dicefate.data.DFRegistries;
import com.srqingchen.dicefate.network.DFNetwork;
import com.srqingchen.dicefate.registry.ModAttachments;
import com.srqingchen.dicefate.registry.ModDataComponents;
import com.srqingchen.dicefate.registry.DiceItems;
import com.srqingchen.dicefate.registry.GearItems;
import com.srqingchen.dicefate.registry.ModBlocks;
import com.srqingchen.dicefate.registry.ModCreativeTabs;
import com.srqingchen.dicefate.registry.ModEntities;
import com.srqingchen.dicefate.registry.ModItems;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 骰掷命术 Dicefate —— 世界级命数骰局体系。
 * 主类只负责装配（总线注册），不承载逻辑。
 */
@Mod(DiceFate.MODID)
public final class DiceFate {
    public static final String MODID = "dicefate";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DiceFate(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, DFConfig.SPEC);

        ModItems.ITEMS.register(modBus);
        DiceItems.ITEMS.register(modBus);
        GearItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        com.srqingchen.dicefate.block.FateBlocks.BLOCKS.register(modBus);
        ModBlocks.BLOCK_ITEMS.register(modBus);
        com.srqingchen.dicefate.village.DiceMerchant.POI_TYPES.register(modBus);
        com.srqingchen.dicefate.village.DiceMerchant.PROFESSIONS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        com.srqingchen.dicefate.registry.ModSounds.SOUNDS.register(modBus);
        DFRegistries.register(modBus);
        DFNetwork.register(modBus);
        // 0.2.0：Curios 可选集成（物品注册完成后初始化；在装注册行为，不在装降级背包生效）
        modBus.addListener(com.srqingchen.dicefate.compat.CuriosCompat::onCommonSetup);

        LOGGER.info("[Dicefate] 命数潮汐开始流动……");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
