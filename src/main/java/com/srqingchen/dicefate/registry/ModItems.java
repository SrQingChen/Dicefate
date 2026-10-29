package com.srqingchen.dicefate.registry;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.item.MiscUseItem;
import com.srqingchen.dicefate.item.PackItem;
import com.srqingchen.dicefate.item.SlotExpandItem;
import com.srqingchen.dicefate.item.UnidentifiedDieItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 材料/工具/骰包/药剂/食物/盲盒（规划 5.2/5.3）。 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DiceFate.MODID);

    // ===== 骰胚 5 =====
    public static final DeferredItem<Item> BLANK_DIE_BONE = ITEMS.registerSimpleItem("blank_die_bone", p -> p.stacksTo(16).rarity(Rarity.COMMON));
    public static final DeferredItem<Item> BLANK_DIE_IRON = ITEMS.registerSimpleItem("blank_die_iron", p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> BLANK_DIE_MITHRIL = ITEMS.registerSimpleItem("blank_die_mithril", p -> p.stacksTo(16).rarity(Rarity.RARE));
    public static final DeferredItem<Item> BLANK_DIE_FATEGOLD = ITEMS.registerSimpleItem("blank_die_fategold", p -> p.stacksTo(16).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> BLANK_DIE_STARLIGHT = ITEMS.registerSimpleItem("blank_die_starlight", p -> p.stacksTo(16).rarity(Rarity.EPIC));

    // ===== 骰面刻片 7 =====
    public static final DeferredItem<Item> FACE_PLATE_D4 = ITEMS.registerSimpleItem("face_plate_d4", p -> p.stacksTo(16).rarity(Rarity.COMMON));
    public static final DeferredItem<Item> FACE_PLATE_D6 = ITEMS.registerSimpleItem("face_plate_d6", p -> p.stacksTo(16).rarity(Rarity.COMMON));
    public static final DeferredItem<Item> FACE_PLATE_D8 = ITEMS.registerSimpleItem("face_plate_d8", p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> FACE_PLATE_D10 = ITEMS.registerSimpleItem("face_plate_d10", p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> FACE_PLATE_D12 = ITEMS.registerSimpleItem("face_plate_d12", p -> p.stacksTo(16).rarity(Rarity.RARE));
    public static final DeferredItem<Item> FACE_PLATE_D20 = ITEMS.registerSimpleItem("face_plate_d20", p -> p.stacksTo(16).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> FACE_PLATE_D24 = ITEMS.registerSimpleItem("face_plate_d24", p -> p.stacksTo(16).rarity(Rarity.EPIC));

    // ===== 核心材料 7 =====
    public static final DeferredItem<Item> FATE_SAND = ITEMS.registerSimpleItem("fate_sand", p -> p.stacksTo(64).rarity(Rarity.COMMON));
    public static final DeferredItem<Item> FATE_DUST = ITEMS.registerSimpleItem("fate_dust", p -> p.stacksTo(64).rarity(Rarity.COMMON));
    public static final DeferredItem<Item> FATE_CORE = ITEMS.registerSimpleItem("fate_core", p -> p.stacksTo(64).rarity(Rarity.RARE));
    public static final DeferredItem<Item> STARSHARD = ITEMS.registerSimpleItem("starshard", p -> p.stacksTo(64).rarity(Rarity.RARE));
    public static final DeferredItem<Item> CRACKED_CORE = ITEMS.registerSimpleItem("cracked_core", p -> p.stacksTo(64).rarity(Rarity.RARE));
    public static final DeferredItem<Item> FATE_FRAGMENT = ITEMS.registerSimpleItem("fate_fragment", p -> p.stacksTo(64).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> SOVEREIGN_CORE = ITEMS.registerSimpleItem("sovereign_core", p -> p.stacksTo(64).rarity(Rarity.EPIC));

    // ===== 元素精华 6 =====
    public static final DeferredItem<Item> ELEMENT_ESSENCE_EMBER = ITEMS.registerSimpleItem("element_essence_ember", p -> p.stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> ELEMENT_ESSENCE_FROST = ITEMS.registerSimpleItem("element_essence_frost", p -> p.stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> ELEMENT_ESSENCE_STORM = ITEMS.registerSimpleItem("element_essence_storm", p -> p.stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> ELEMENT_ESSENCE_VENOM = ITEMS.registerSimpleItem("element_essence_venom", p -> p.stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> ELEMENT_ESSENCE_RADIANT = ITEMS.registerSimpleItem("element_essence_radiant", p -> p.stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> ELEMENT_ESSENCE_UMBRAL = ITEMS.registerSimpleItem("element_essence_umbral", p -> p.stacksTo(64).rarity(Rarity.UNCOMMON));

    // ===== 工具与消耗材料 =====
    public static final DeferredItem<MiscUseItem> RUNE_INK = ITEMS.register("rune_ink", id -> new MiscUseItem(IdProps.item(id).stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> REPAIR_FILE = ITEMS.registerSimpleItem("repair_file", p -> p.stacksTo(64).rarity(Rarity.COMMON));
    public static final DeferredItem<Item> FATE_GLUE = ITEMS.registerSimpleItem("fate_glue", p -> p.stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> PERFECT_OIL = ITEMS.registerSimpleItem("perfect_oil", p -> p.stacksTo(64).rarity(Rarity.RARE));
    public static final DeferredItem<Item> SPIRIT_FEED = ITEMS.registerSimpleItem("spirit_feed", p -> p.stacksTo(64).rarity(Rarity.COMMON));
    public static final DeferredItem<Item> APPRAISAL_LENS = ITEMS.registerSimpleItem("appraisal_lens", p -> p.stacksTo(1).durability(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> APPRAISAL_SCROLL = ITEMS.registerSimpleItem("appraisal_scroll", p -> p.stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> MIRROR_SHARD = ITEMS.registerSimpleItem("mirror_shard", p -> p.stacksTo(64).rarity(Rarity.RARE));
    public static final DeferredItem<Item> SLOT_SHARD = ITEMS.registerSimpleItem("slot_shard", p -> p.stacksTo(64).rarity(Rarity.UNCOMMON));

    // ===== 骰位扩容符 I–V =====
    public static final DeferredItem<SlotExpandItem> SLOT_EXPAND_1 = ITEMS.register("slot_expand_1", id -> new SlotExpandItem(IdProps.item(id).stacksTo(1).rarity(Rarity.UNCOMMON), 0));
    public static final DeferredItem<SlotExpandItem> SLOT_EXPAND_2 = ITEMS.register("slot_expand_2", id -> new SlotExpandItem(IdProps.item(id).stacksTo(1).rarity(Rarity.RARE), 1));
    public static final DeferredItem<SlotExpandItem> SLOT_EXPAND_3 = ITEMS.register("slot_expand_3", id -> new SlotExpandItem(IdProps.item(id).stacksTo(1).rarity(Rarity.RARE), 2));
    public static final DeferredItem<SlotExpandItem> SLOT_EXPAND_4 = ITEMS.register("slot_expand_4", id -> new SlotExpandItem(IdProps.item(id).stacksTo(1).rarity(Rarity.EPIC), 3));
    public static final DeferredItem<SlotExpandItem> SLOT_EXPAND_5 = ITEMS.register("slot_expand_5", id -> new SlotExpandItem(IdProps.item(id).stacksTo(1).rarity(Rarity.EPIC), 4));

    // ===== 专属槽/能力印 =====
    public static final DeferredItem<MiscUseItem> SIGIL_ADDITION = ITEMS.register("sigil_addition", id -> new MiscUseItem(IdProps.item(id).stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<MiscUseItem> SIGIL_MULTIPLICATION = ITEMS.register("sigil_multiplication", id -> new MiscUseItem(IdProps.item(id).stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<MiscUseItem> SIGIL_ECHO = ITEMS.register("sigil_echo", id -> new MiscUseItem(IdProps.item(id).stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<MiscUseItem> SIGIL_ECHO_2 = ITEMS.register("sigil_echo_2", id -> new MiscUseItem(IdProps.item(id).stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<MiscUseItem> RESPEC_STONE = ITEMS.register("respec_stone", id -> new MiscUseItem(IdProps.item(id).stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<MiscUseItem> FATE_LOCK_TALISMAN = ITEMS.register("fate_lock_talisman", id -> new MiscUseItem(IdProps.item(id).stacksTo(1).rarity(Rarity.RARE)));

    // ===== 骰运台祭品/工具 =====
    public static final DeferredItem<Item> INCENSE = ITEMS.registerSimpleItem("incense", p -> p.stacksTo(64).rarity(Rarity.COMMON));
    public static final DeferredItem<Item> OFFERING_BONE = ITEMS.registerSimpleItem("offering_bone", p -> p.stacksTo(64).rarity(Rarity.COMMON));
    public static final DeferredItem<Item> OFFERING_STAR = ITEMS.registerSimpleItem("offering_star", p -> p.stacksTo(64).rarity(Rarity.RARE));
    public static final DeferredItem<Item> OFFERING_CORE = ITEMS.registerSimpleItem("offering_core", p -> p.stacksTo(64).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> MERCHANT_TOKEN = ITEMS.registerSimpleItem("merchant_token", p -> p.stacksTo(64).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> SOVEREIGN_SEAL = ITEMS.registerSimpleItem("sovereign_seal", p -> p.stacksTo(4).rarity(Rarity.EPIC));

    // ===== 骰子包 16 =====
    public static final DeferredItem<PackItem> PACK_COMMON = ITEMS.register("pack_common", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.COMMON)));
    public static final DeferredItem<PackItem> PACK_FINE = ITEMS.register("pack_fine", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<PackItem> PACK_RARE = ITEMS.register("pack_rare", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.RARE)));
    public static final DeferredItem<PackItem> PACK_EPIC = ITEMS.register("pack_epic", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.EPIC)));
    public static final DeferredItem<PackItem> PACK_FATE_MINOR = ITEMS.register("pack_fate_minor", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.RARE)));
    public static final DeferredItem<PackItem> PACK_FATE_MAJOR = ITEMS.register("pack_fate_major", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.EPIC)));
    public static final DeferredItem<PackItem> PACK_EMBER = ITEMS.register("pack_ember", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.RARE)));
    public static final DeferredItem<PackItem> PACK_FROST = ITEMS.register("pack_frost", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.RARE)));
    public static final DeferredItem<PackItem> PACK_STORM = ITEMS.register("pack_storm", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.RARE)));
    public static final DeferredItem<PackItem> PACK_VENOM = ITEMS.register("pack_venom", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.RARE)));
    public static final DeferredItem<PackItem> PACK_RADIANT = ITEMS.register("pack_radiant", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.EPIC)));
    public static final DeferredItem<PackItem> PACK_UMBRAL = ITEMS.register("pack_umbral", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.EPIC)));
    public static final DeferredItem<PackItem> PACK_STRAIGHT = ITEMS.register("pack_straight", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.RARE)));
    public static final DeferredItem<PackItem> PACK_TRIPLE = ITEMS.register("pack_triple", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.RARE)));
    public static final DeferredItem<PackItem> PACK_TWO_FACED = ITEMS.register("pack_two_faced", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.EPIC)));
    public static final DeferredItem<PackItem> PACK_NULL = ITEMS.register("pack_null", id -> new PackItem(IdProps.item(id).stacksTo(4).rarity(Rarity.EPIC)));

    // ===== 药剂/食物/盲盒 =====
    public static final DeferredItem<MiscUseItem> POTION_FATE_AIM = ITEMS.register("potion_fate_aim", id -> new MiscUseItem(IdProps.item(id).stacksTo(4).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<MiscUseItem> POTION_FATE_GUARD = ITEMS.register("potion_fate_guard", id -> new MiscUseItem(IdProps.item(id).stacksTo(4).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<MiscUseItem> POTION_FATE_SIGHT = ITEMS.register("potion_fate_sight", id -> new MiscUseItem(IdProps.item(id).stacksTo(4).rarity(Rarity.RARE)));
    public static final DeferredItem<MiscUseItem> LUCKY_COOKIE = ITEMS.register("lucky_cookie", id -> new MiscUseItem(IdProps.item(id).stacksTo(16).rarity(Rarity.COMMON)));
    public static final DeferredItem<UnidentifiedDieItem> UNIDENTIFIED_DIE = ITEMS.register("unidentified_die", id -> new UnidentifiedDieItem(IdProps.item(id).stacksTo(16).rarity(Rarity.UNCOMMON)));

    private ModItems() {}
}
