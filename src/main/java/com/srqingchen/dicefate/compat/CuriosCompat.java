package com.srqingchen.dicefate.compat;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.registry.GearItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Curios 兼容层（规划 8.10.1）。0.2.0 落地：
 * - 槽位分配走数据标签（data/curios/tags/item/{charm,ring,belt}.json，validator=curios:tag）；
 * - 行为注册走 CuriosApi.registerCurio（Curios 在装时）；
 * - Curios 不在装时效果降级为背包扫描（README 承诺）。
 * CuriosHooks 内部类只有 Curios 在装时才会被加载，保证 optional 依赖安全。
 */
public final class CuriosCompat {
    private static boolean loaded;

    private CuriosCompat() {}

    /** FMLCommonSetupEvent 调用（items 已注册）。 */
    public static void onCommonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        init();
    }

    static void init() {
        loaded = ModList.get().isLoaded("curios");
        if (loaded) {
            CuriosHooks.registerAll();
            DiceFate.LOGGER.info("[Dicefate] Curios 已连接：7 件饰品经 charm/ring/belt 槽位生效");
        } else {
            DiceFate.LOGGER.info("[Dicefate] Curios 未在装：饰品效果降级为背包携带生效");
        }
    }

    /** 玩家是否携带（装备/背包）指定饰品。
     *  0.3.0：Curios 在装时**严格**只认装备槽位（原背包兜底在装时仍生效，
     *  使"命具"专属槽位形同虚设——用户"槽位未注册"观感根因之一）；
     *  未安装 Curios 时才降级为背包携带生效。 */
    public static boolean hasCurio(Player player, Item item) {
        if (player == null || item == null) return false;
        if (loaded) return CuriosHooks.isEquipped(player, item);
        return inventoryHas(player, item);
    }

    public static boolean hasGamblerCharm(Player player) {
        return hasCurio(player, GearItems.GAMBLER_CHARM.get());
    }

    public static boolean hasLuckyPendant(Player player) {
        return hasCurio(player, GearItems.LUCKY_PENDANT.get());
    }

    public static boolean hasFateLockRing(ServerPlayer player) {
        return hasCurio(player, GearItems.FATE_LOCK_RING.get());
    }

    public static boolean hasElementResonator(Player player) {
        return hasCurio(player, GearItems.ELEMENT_RESONATOR.get());
    }

    public static boolean hasFateMirror(Player player) {
        return hasCurio(player, GearItems.FATE_MIRROR.get());
    }

    public static boolean hasSpiritBell(Player player) {
        return hasCurio(player, GearItems.SPIRIT_BELL.get());
    }

    private static boolean inventoryHas(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) return true;
        }
        return false;
    }

    /** 仅 Curios 在装时加载的钩子（隔离 API 类引用）。 */
    private static final class CuriosHooks {
        static void registerAll() {
            top.theillusivec4.curios.api.CuriosApi.registerCurio(
                GearItems.DICE_POUCH.get(), new DiceCurio());
            top.theillusivec4.curios.api.CuriosApi.registerCurio(
                GearItems.LUCKY_PENDANT.get(), new DiceCurio());
            top.theillusivec4.curios.api.CuriosApi.registerCurio(
                GearItems.FATE_MIRROR.get(), new DiceCurio());
            top.theillusivec4.curios.api.CuriosApi.registerCurio(
                GearItems.SPIRIT_BELL.get(), new DiceCurio());
            top.theillusivec4.curios.api.CuriosApi.registerCurio(
                GearItems.GAMBLER_CHARM.get(), new DiceCurio());
            top.theillusivec4.curios.api.CuriosApi.registerCurio(
                GearItems.FATE_LOCK_RING.get(), new DiceCurio());
            top.theillusivec4.curios.api.CuriosApi.registerCurio(
                GearItems.ELEMENT_RESONATOR.get(), new DiceCurio());
        }

        static boolean isEquipped(Player player, Item item) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.isEquipped(item))
                .orElse(false);
        }
    }

    /** 默认行为 + 槽位提示的 ICurioItem 实现（槽位合法性由数据标签决定）。 */
    private static final class DiceCurio implements top.theillusivec4.curios.api.type.capability.ICurioItem {
        /** 0.3.1 修复：传入列表可能是不可变的（List.of/NBT 视图）——重建可变列表再添加，
         *  原直接 add 抛 UnsupportedOperationException 崩掉整个 tooltip 事件。 */
        @Override
        public java.util.List<net.minecraft.network.chat.Component> getSlotsTooltip(
            java.util.List<net.minecraft.network.chat.Component> tooltips,
            net.minecraft.world.item.Item.TooltipContext ctx, ItemStack stack) {
            java.util.List<net.minecraft.network.chat.Component> out = new java.util.ArrayList<>(tooltips);
            out.add(net.minecraft.network.chat.Component.translatable("tooltip.dicefate.curios_slot"));
            return out;
        }
    }
}
