package com.srqingchen.dicefate.item;

import com.srqingchen.dicefate.dice.DFRarity;
import com.srqingchen.dicefate.dice.DieDefinition;
import com.srqingchen.dicefate.dice.DieRune;
import com.srqingchen.dicefate.dice.DieState;
import com.srqingchen.dicefate.dice.DiceLookup;
import com.srqingchen.dicefate.registry.DiceItems;
import com.srqingchen.dicefate.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * 骰子物品：物品本体为 3D 网格模型，tooltip 呈现面数/元素/骰纹/磨损。
 * 动态状态存 die_state 组件；静态定义查数据包（静态 Spec 兜底）。
 */
public class DiceItem extends Item {
    public final DiceItems.Spec spec;
    public final Identifier dieId;

    public DiceItem(Identifier dieId, Properties props, DiceItems.Spec spec) {
        super(props);
        this.dieId = dieId;
        this.spec = spec;
    }

    public static ItemStack withFreshState(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.DIE_STATE.get(), DieState.fresh());
        return stack;
    }

    @Nullable
    public DieDefinition definition(Level level) {
        return DiceLookup.definition(level.registryAccess(), dieId);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        DieState state = stack.get(ModDataComponents.DIE_STATE.get());
        DieDefinition def = ctx instanceof Level level
            ? definition(level)
            : DiceLookup.fromStatic(dieId);
        if (def == null) return;

        builder.accept(Component.translatable("tooltip.dicefate.die.face", def.face())
            .withStyle(ChatFormatting.GRAY));
        if (def.element().isReal()) {
            builder.accept(Component.translatable("tooltip.dicefate.die.element." + def.element().getSerializedName())
                .withStyle(ChatFormatting.BLUE));
        }
        switch (def.kind()) {
            case ADD -> builder.accept(Component.translatable("tooltip.dicefate.die.add").withStyle(ChatFormatting.AQUA));
            case MUL -> builder.accept(Component.translatable("tooltip.dicefate.die.mul").withStyle(ChatFormatting.AQUA));
            case SKILL -> builder.accept(Component.translatable("tooltip.dicefate.die.skill").withStyle(ChatFormatting.LIGHT_PURPLE));
            case UNIQUE -> builder.accept(Component.translatable("tooltip.dicefate.die.unique").withStyle(ChatFormatting.GOLD));
            case CURSED -> builder.accept(Component.translatable("tooltip.dicefate.die.cursed").withStyle(ChatFormatting.DARK_PURPLE));
            default -> {}
        }
        if (state != null) {
            if (!state.rune().isEmpty() && state.runeId() != DieRune.NONE) {
                builder.accept(Component.translatable("tooltip.dicefate.rune." + state.runeId().getSerializedName())
                    .withStyle(ChatFormatting.DARK_AQUA));
            }
            if (def.material().baseWear() > 0) {
                int wear = state.wear();
                int max = def.material().baseWear();
                if (wear > 0) {
                    ChatFormatting c = wear >= max ? ChatFormatting.RED : wear >= max / 2 ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
                    builder.accept(Component.translatable("tooltip.dicefate.die.wear", wear, max).withStyle(c));
                }
            }
        }
        builder.accept(Component.translatable("tooltip.dicefate.die." + def.rarity().getSerializedName())
            .withStyle(def.rarity().color()));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return spec.rarity() == DFRarity.FATED || spec.rarity() == DFRarity.LEGENDARY;
    }
}
