package com.srqingchen.dicefate.village;

import com.google.common.collect.ImmutableSet;
import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.block.FateBlocks;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 骰商：原版村民职业 + 摊位工作站 POI（规划 6.5.1）。 */
public final class DiceMerchant {
    public static final DeferredRegister<PoiType> POI_TYPES =
        DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, DiceFate.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
        DeferredRegister.create(Registries.VILLAGER_PROFESSION, DiceFate.MODID);

    public static final ResourceKey<PoiType> MERCHANT_STALL_POI_KEY =
        ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, DiceFate.id("merchant_stall"));

    public static final DeferredHolder<PoiType, PoiType> MERCHANT_STALL_POI = POI_TYPES.register(
        "merchant_stall", () -> new PoiType(
            ImmutableSet.copyOf(FateBlocks.MERCHANT_STALL.get().getStateDefinition().getPossibleStates()),
            1, 1));

    /** 0.2.0：交易集（trade_set/merchant_basic.json：6 格，命砂计价，规划 7.3.1）。 */
    public static final ResourceKey<net.minecraft.world.item.trading.TradeSet> BASIC_TRADES =
        ResourceKey.create(Registries.TRADE_SET, DiceFate.id("merchant_basic"));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> DICE_MERCHANT =
        PROFESSIONS.register("dice_merchant", () -> new VillagerProfession(
            Component.translatable("entity.villager.dicefate.dice_merchant"),
            holder -> holder.is(MERCHANT_STALL_POI_KEY),
            holder -> holder.is(MERCHANT_STALL_POI_KEY),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_CARTOGRAPHER,
            (Int2ObjectMap<ResourceKey<net.minecraft.world.item.trading.TradeSet>>)
                Int2ObjectMaps.singleton(1, BASIC_TRADES)));

    private DiceMerchant() {}
}
