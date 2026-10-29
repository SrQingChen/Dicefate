package com.srqingchen.dicefate.data;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.dice.DieDefinition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

/**
 * 数据包注册表：骰子定义 / 生物命盘 / 骰包 / 元素反应 / 协同 / 命纹。
 * 全部同步到客户端（tooltip/HUD 需要 DieDefinition）。
 */
public final class DFRegistries {
    public static final ResourceKey<Registry<DieDefinition>> DIE =
        key("die");
    public static final ResourceKey<Registry<PoolProfile>> POOL_PROFILE =
        key("pool_profile");
    public static final ResourceKey<Registry<PackDefinition>> PACK =
        key("pack");
    public static final ResourceKey<Registry<ReactionDefinition>> REACTION =
        key("reaction");
    public static final ResourceKey<Registry<SynergyDefinition>> SYNERGY =
        key("synergy");
    public static final ResourceKey<Registry<FateRuneDefinition>> FATE_RUNE =
        key("fate_rune");

    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(DiceFate.id(name));
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DFRegistries::onNewRegistry);
    }

    private static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(DIE, DieDefinition.CODEC, DieDefinition.CODEC);
        event.dataPackRegistry(POOL_PROFILE, PoolProfile.CODEC, PoolProfile.CODEC);
        event.dataPackRegistry(PACK, PackDefinition.CODEC, PackDefinition.CODEC);
        event.dataPackRegistry(REACTION, ReactionDefinition.CODEC, ReactionDefinition.CODEC);
        event.dataPackRegistry(SYNERGY, SynergyDefinition.CODEC, SynergyDefinition.CODEC);
        event.dataPackRegistry(FATE_RUNE, FateRuneDefinition.CODEC, FateRuneDefinition.CODEC);
    }

    private DFRegistries() {}
}
