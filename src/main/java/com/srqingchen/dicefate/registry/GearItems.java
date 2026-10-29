package com.srqingchen.dicefate.registry;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.item.DiceSlingItem;
import com.srqingchen.dicefate.item.FateArrowItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.Map;

/** 武器/远程/盾/护甲/Curios（规划 5.5）。 */
public final class GearItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DiceFate.MODID);

    // ===== 武器材质 =====
    public static final ToolMaterial BONE_BLADE = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 180, 2.0F, 0.0F, 15, ItemTags.WOODEN_TOOL_MATERIALS);
    public static final ToolMaterial IRON_BLADE = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 420, 2.0F, 0.0F, 14, ItemTags.IRON_TOOL_MATERIALS);
    public static final ToolMaterial MITHRIL_BLADE = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 900, 2.5F, 0.0F, 18, ItemTags.IRON_TOOL_MATERIALS);
    public static final ToolMaterial FATEGOLD_BLADE = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1600, 3.0F, 0.0F, 22, ItemTags.DIAMOND_TOOL_MATERIALS);

    // ===== 护甲材质 =====
    public static final ResourceKey<EquipmentAsset> DICECASTER_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, DiceFate.id("dicecaster"));
    public static final ResourceKey<EquipmentAsset> GAMBLER_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, DiceFate.id("gambler"));
    public static final ArmorMaterial DICECASTER = new ArmorMaterial(15, Map.of(
            ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 4, ArmorType.CHESTPLATE, 5, ArmorType.HELMET, 2, ArmorType.BODY, 4),
            20, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, ItemTags.REPAIRS_LEATHER_ARMOR, DICECASTER_ASSET);
    public static final ArmorMaterial GAMBLER = new ArmorMaterial(20, Map.of(
            ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 5, ArmorType.CHESTPLATE, 6, ArmorType.HELMET, 2, ArmorType.BODY, 5),
            18, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, ItemTags.REPAIRS_LEATHER_ARMOR, GAMBLER_ASSET);

    // ===== 骰刃 4（sword 工厂 + 总点数加成由拼点服务读取物品身份） =====
    public static final DeferredItem<Item> DICE_BLADE_BONE = ITEMS.registerSimpleItem("dice_blade_bone", p -> p.sword(BONE_BLADE, 4.0F, -2.4F).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> DICE_BLADE_IRON = ITEMS.registerSimpleItem("dice_blade_iron", p -> p.sword(IRON_BLADE, 6.0F, -2.4F).rarity(Rarity.RARE));
    public static final DeferredItem<Item> DICE_BLADE_MITHRIL = ITEMS.registerSimpleItem("dice_blade_mithril", p -> p.sword(MITHRIL_BLADE, 8.0F, -2.4F).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> DICE_BLADE_FATEGOLD = ITEMS.registerSimpleItem("dice_blade_fategold", p -> p.sword(FATEGOLD_BLADE, 10.0F, -2.4F).rarity(Rarity.EPIC));

    // ===== 远程 3 =====
    public static final DeferredItem<DiceSlingItem> DICE_SLING_BONE = ITEMS.register("dice_sling_bone", id -> new DiceSlingItem(IdProps.item(id).durability(180).rarity(Rarity.UNCOMMON), 3.0F));
    public static final DeferredItem<DiceSlingItem> DICE_SLING_MITHRIL = ITEMS.register("dice_sling_mithril", id -> new DiceSlingItem(IdProps.item(id).durability(600).rarity(Rarity.RARE), 5.0F));
    public static final DeferredItem<FateArrowItem> FATE_ARROW = ITEMS.register("fate_arrow", id -> new FateArrowItem(IdProps.item(id).stacksTo(64).rarity(Rarity.UNCOMMON)));

    // ===== 盾 3 =====
    public static final DeferredItem<ShieldItem> FATE_SHIELD_BONE = ITEMS.register("fate_shield_bone", id -> new ShieldItem(IdProps.item(id).durability(120).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ShieldItem> FATE_SHIELD_MITHRIL = ITEMS.register("fate_shield_mithril", id -> new ShieldItem(IdProps.item(id).durability(240).rarity(Rarity.RARE)));
    public static final DeferredItem<ShieldItem> FATE_SHIELD_FATEGOLD = ITEMS.register("fate_shield_fategold", id -> new ShieldItem(IdProps.item(id).durability(400).rarity(Rarity.EPIC)));

    // ===== 护甲 2 套 × 4 =====
    public static final DeferredItem<Item> DICECASTER_HOOD = ITEMS.registerSimpleItem("dicecaster_hood", p -> p.humanoidArmor(DICECASTER, ArmorType.HELMET).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> DICECASTER_ROBE = ITEMS.registerSimpleItem("dicecaster_robe", p -> p.humanoidArmor(DICECASTER, ArmorType.CHESTPLATE).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> DICECASTER_LEGGINGS = ITEMS.registerSimpleItem("dicecaster_leggings", p -> p.humanoidArmor(DICECASTER, ArmorType.LEGGINGS).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> DICECASTER_BOOTS = ITEMS.registerSimpleItem("dicecaster_boots", p -> p.humanoidArmor(DICECASTER, ArmorType.BOOTS).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> GAMBLER_HOOD = ITEMS.registerSimpleItem("gambler_hood", p -> p.humanoidArmor(GAMBLER, ArmorType.HELMET).rarity(Rarity.RARE));
    public static final DeferredItem<Item> GAMBLER_JACKET = ITEMS.registerSimpleItem("gambler_jacket", p -> p.humanoidArmor(GAMBLER, ArmorType.CHESTPLATE).rarity(Rarity.RARE));
    public static final DeferredItem<Item> GAMBLER_PANTS = ITEMS.registerSimpleItem("gambler_pants", p -> p.humanoidArmor(GAMBLER, ArmorType.LEGGINGS).rarity(Rarity.RARE));
    public static final DeferredItem<Item> GAMBLER_BOOTS = ITEMS.registerSimpleItem("gambler_boots", p -> p.humanoidArmor(GAMBLER, ArmorType.BOOTS).rarity(Rarity.RARE));

    // ===== Curios 7（Curios 存在时经 CuriosCompat 注册；否则背包内生效） =====
    public static final DeferredItem<com.srqingchen.dicefate.item.PouchItem> DICE_POUCH = ITEMS.register("dice_pouch",
        id -> new com.srqingchen.dicefate.item.PouchItem(IdProps.item(id).stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> LUCKY_PENDANT = ITEMS.registerSimpleItem("lucky_pendant", p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> FATE_MIRROR = ITEMS.registerSimpleItem("fate_mirror", p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> SPIRIT_BELL = ITEMS.registerSimpleItem("spirit_bell", p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> GAMBLER_CHARM = ITEMS.registerSimpleItem("gambler_charm", p -> p.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> FATE_LOCK_RING = ITEMS.registerSimpleItem("fate_lock_ring", p -> p.stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> ELEMENT_RESONATOR = ITEMS.registerSimpleItem("element_resonator", p -> p.stacksTo(1).rarity(Rarity.EPIC));

    private GearItems() {}
}
