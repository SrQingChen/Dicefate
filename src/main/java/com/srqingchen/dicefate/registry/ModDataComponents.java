package com.srqingchen.dicefate.registry;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.dice.DieState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 数据组件注册。骰子的动态状态走 die_state 组件。
 */
public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, DiceFate.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DieState>> DIE_STATE =
        COMPONENTS.register("die_state", () -> DataComponentType.<DieState>builder()
            .persistent(DieState.CODEC)
            .networkSynchronized(DieState.STREAM_CODEC)
            .build());

    private ModDataComponents() {}
}
