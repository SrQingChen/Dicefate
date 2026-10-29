package com.srqingchen.dicefate.registry;

import com.srqingchen.dicefate.DiceFate;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 方块物品注册表（由 FateBlocks 联动填充）。 */
public final class ModBlocks {
    public static final DeferredRegister<Item> BLOCK_ITEMS =
        DeferredRegister.createItems(DiceFate.MODID);

    private ModBlocks() {}
}
