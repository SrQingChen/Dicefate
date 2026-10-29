package com.srqingchen.dicefate.dice;

import com.srqingchen.dicefate.data.DFRegistries;
import com.srqingchen.dicefate.item.DiceItem;
import com.srqingchen.dicefate.registry.DiceItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

/** 骰子定义查询：数据包优先，静态 Spec 兜底。 */
public final class DiceLookup {
    private DiceLookup() {}

    /** 取骰子定义（数据包覆盖静态）。 */
    @Nullable
    public static DieDefinition definition(HolderLookup.Provider provider, Identifier itemId) {
        ResourceKey<DieDefinition> key = ResourceKey.create(DFRegistries.DIE, itemId);
        Optional<? extends Holder.Reference<DieDefinition>> held =
            provider.lookup(DFRegistries.DIE).flatMap(reg -> reg.get(key));
        if (held.isPresent()) return held.get().value();
        return fromStatic(itemId);
    }

    @Nullable
    public static DieDefinition definitionFor(HolderLookup.Provider provider, ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (!(stack.getItem() instanceof DiceItem diceItem)) return null;
        return definition(provider, diceItem.dieId);
    }

    /** 由静态 Spec 构建兜底定义。 */
    @Nullable
    public static DieDefinition fromStatic(Identifier itemId) {
        DiceItems.Spec spec = DiceItems.spec(itemId.getPath());
        if (spec == null) return null;
        return new DieDefinition(
            Identifier.fromNamespaceAndPath("dicefate", itemId.getPath()),
            spec.face(), spec.kind(), spec.material(), spec.rarity(), spec.element(),
            itemId.getPath().endsWith("null_face") ? java.util.List.of(0, 0, 0, 6, 6, 6) : java.util.List.of(),
            spec.skillFamily() == null ? Optional.empty()
                : Optional.of(Identifier.fromNamespaceAndPath("dicefate", "skill/" + spec.skillFamily())),
            spec.skillTier()
        );
    }
}
