package com.srqingchen.dicefate.registry;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.block.FateBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 创造标签：骰子 / 材料 / 装备与方块 三页。 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DiceFate.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DICE_TAB =
        TABS.register("dice", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.dicefate.dice"))
            .icon(() -> new ItemStack(DiceItems.DIE_BONE_D6.get()))
            .displayItems((params, output) -> {
                DiceItems.allDice().forEach(d -> output.accept(DiceItemWithState.of(d)));
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MATERIAL_TAB =
        TABS.register("materials", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
            .title(Component.translatable("itemGroup.dicefate.materials"))
            .icon(() -> new ItemStack(ModItems.FATE_SAND.get()))
            .displayItems((params, output) -> {
                ModItems.ITEMS.getEntries().forEach(h -> output.accept(h.get()));
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GEAR_TAB =
        TABS.register("gear", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 2)
            .title(Component.translatable("itemGroup.dicefate.gear"))
            .icon(() -> new ItemStack(GearItems.DICE_BLADE_FATEGOLD.get()))
            .displayItems((params, output) -> {
                GearItems.ITEMS.getEntries().forEach(h -> output.accept(h.get()));
                FateBlocks.BLOCKS.getEntries().forEach(b -> output.accept(b.get()));
            })
            .build());

    /** 带初始 die_state 的骰子创造栈。 */
    private static final class DiceItemWithState {
        static ItemStack of(com.srqingchen.dicefate.item.DiceItem d) {
            return com.srqingchen.dicefate.item.DiceItem.withFreshState(d);
        }
    }

    private ModCreativeTabs() {}
}
