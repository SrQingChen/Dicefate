package com.srqingchen.dicefate.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * 26.1 的 Item.Properties / BlockBehaviour.Properties 强制要求 id（构造时 Objects.requireNonNull）。
 * 凡走 DeferredRegister.register(Supplier) 路径的注册都必须显式注入 id —— 本工具统一构造。
 */
public final class IdProps {

    public static Item.Properties item(Identifier id) {
        Item.Properties props = new Item.Properties();
        props.setId(ResourceKey.create(Registries.ITEM, id));
        return props;
    }

    private IdProps() {}
}
