package com.srqingchen.dicefate.registry;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.dice.DieElement;
import com.srqingchen.dicefate.dice.DieKind;
import com.srqingchen.dicefate.dice.DieMaterial;
import com.srqingchen.dicefate.dice.DFRarity;
import com.srqingchen.dicefate.item.DiceItem;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 83 种骰子物品（规划 5.1 全表）。
 * 静态 Spec 为注册期兜底；运行时以数据包 dicefate/die 注册表为准。
 */
public final class DiceItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DiceFate.MODID);

    /** 骰子静态规格（面数/类别/材质/稀有度/元素/技能族/阶）。 */
    public record Spec(int face, DieKind kind, DieMaterial material, DFRarity rarity, DieElement element,
                       String skillFamily, int skillTier) implements StringRepresentable {
        @Override public String getSerializedName() { return "spec"; }
    }

    // ===== 基础骰 15 =====
    public static final DeferredItem<DiceItem> DIE_BONE_D4 = register("die_bone_d4", new Spec(4, DieKind.BASIC, DieMaterial.BONE, DFRarity.COMMON, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_BONE_D6 = register("die_bone_d6", new Spec(6, DieKind.BASIC, DieMaterial.BONE, DFRarity.COMMON, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_BONE_D8 = register("die_bone_d8", new Spec(8, DieKind.BASIC, DieMaterial.BONE, DFRarity.UNCOMMON, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_IRON_D6 = register("die_iron_d6", new Spec(6, DieKind.BASIC, DieMaterial.IRON, DFRarity.UNCOMMON, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_IRON_D8 = register("die_iron_d8", new Spec(8, DieKind.BASIC, DieMaterial.IRON, DFRarity.UNCOMMON, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_IRON_D10 = register("die_iron_d10", new Spec(10, DieKind.BASIC, DieMaterial.IRON, DFRarity.RARE, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_MITHRIL_D8 = register("die_mithril_d8", new Spec(8, DieKind.BASIC, DieMaterial.MITHRIL, DFRarity.RARE, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_MITHRIL_D10 = register("die_mithril_d10", new Spec(10, DieKind.BASIC, DieMaterial.MITHRIL, DFRarity.RARE, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_MITHRIL_D12 = register("die_mithril_d12", new Spec(12, DieKind.BASIC, DieMaterial.MITHRIL, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_FATEGOLD_D10 = register("die_fategold_d10", new Spec(10, DieKind.BASIC, DieMaterial.FATEGOLD, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_FATEGOLD_D12 = register("die_fategold_d12", new Spec(12, DieKind.BASIC, DieMaterial.FATEGOLD, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_FATEGOLD_D20 = register("die_fategold_d20", new Spec(20, DieKind.BASIC, DieMaterial.FATEGOLD, DFRarity.LEGENDARY, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_STARLIGHT_D12 = register("die_starlight_d12", new Spec(12, DieKind.BASIC, DieMaterial.STARLIGHT, DFRarity.LEGENDARY, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_STARLIGHT_D20 = register("die_starlight_d20", new Spec(20, DieKind.BASIC, DieMaterial.STARLIGHT, DFRarity.LEGENDARY, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> DIE_STARLIGHT_D24 = register("die_starlight_d24", new Spec(24, DieKind.BASIC, DieMaterial.STARLIGHT, DFRarity.FATED, DieElement.NONE, null, 0));

    // ===== 元素骰 18 =====
    public static final DeferredItem<DiceItem> DIE_EMBER_SPARK = register("die_ember_spark", new Spec(8, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.UNCOMMON, DieElement.EMBER, null, 0));
    public static final DeferredItem<DiceItem> DIE_EMBER_RESONANT = register("die_ember_resonant", new Spec(12, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.EMBER, null, 0));
    public static final DeferredItem<DiceItem> DIE_EMBER_ORIGIN = register("die_ember_origin", new Spec(20, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.EMBER, null, 0));
    public static final DeferredItem<DiceItem> DIE_FROST_SPARK = register("die_frost_spark", new Spec(8, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.UNCOMMON, DieElement.FROST, null, 0));
    public static final DeferredItem<DiceItem> DIE_FROST_RESONANT = register("die_frost_resonant", new Spec(12, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.FROST, null, 0));
    public static final DeferredItem<DiceItem> DIE_FROST_ORIGIN = register("die_frost_origin", new Spec(20, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.FROST, null, 0));
    public static final DeferredItem<DiceItem> DIE_STORM_SPARK = register("die_storm_spark", new Spec(8, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.UNCOMMON, DieElement.STORM, null, 0));
    public static final DeferredItem<DiceItem> DIE_STORM_RESONANT = register("die_storm_resonant", new Spec(12, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.STORM, null, 0));
    public static final DeferredItem<DiceItem> DIE_STORM_ORIGIN = register("die_storm_origin", new Spec(20, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.STORM, null, 0));
    public static final DeferredItem<DiceItem> DIE_VENOM_SPARK = register("die_venom_spark", new Spec(8, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.UNCOMMON, DieElement.VENOM, null, 0));
    public static final DeferredItem<DiceItem> DIE_VENOM_RESONANT = register("die_venom_resonant", new Spec(12, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.VENOM, null, 0));
    public static final DeferredItem<DiceItem> DIE_VENOM_ORIGIN = register("die_venom_origin", new Spec(20, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.VENOM, null, 0));
    public static final DeferredItem<DiceItem> DIE_RADIANT_SPARK = register("die_radiant_spark", new Spec(8, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.UNCOMMON, DieElement.RADIANT, null, 0));
    public static final DeferredItem<DiceItem> DIE_RADIANT_RESONANT = register("die_radiant_resonant", new Spec(12, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.RADIANT, null, 0));
    public static final DeferredItem<DiceItem> DIE_RADIANT_ORIGIN = register("die_radiant_origin", new Spec(20, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.RADIANT, null, 0));
    public static final DeferredItem<DiceItem> DIE_UMBRAL_SPARK = register("die_umbral_spark", new Spec(8, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.UNCOMMON, DieElement.UMBRAL, null, 0));
    public static final DeferredItem<DiceItem> DIE_UMBRAL_RESONANT = register("die_umbral_resonant", new Spec(12, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.UMBRAL, null, 0));
    public static final DeferredItem<DiceItem> DIE_UMBRAL_ORIGIN = register("die_umbral_origin", new Spec(20, DieKind.ELEMENT, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.UMBRAL, null, 0));

    // ===== 技能骰 24（8 族 × 3 阶） =====
    public static final DeferredItem<DiceItem> SKILL_DIE_ECHO_1 = register("skill_die_echo_1", new Spec(6, DieKind.SKILL, DieMaterial.SKILL, DFRarity.UNCOMMON, DieElement.NONE, "echo", 0));
    public static final DeferredItem<DiceItem> SKILL_DIE_ECHO_2 = register("skill_die_echo_2", new Spec(12, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "echo", 1));
    public static final DeferredItem<DiceItem> SKILL_DIE_ECHO_3 = register("skill_die_echo_3", new Spec(20, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "echo", 2));
    public static final DeferredItem<DiceItem> SKILL_DIE_PURSUIT_1 = register("skill_die_pursuit_1", new Spec(6, DieKind.SKILL, DieMaterial.SKILL, DFRarity.UNCOMMON, DieElement.NONE, "pursuit", 0));
    public static final DeferredItem<DiceItem> SKILL_DIE_PURSUIT_2 = register("skill_die_pursuit_2", new Spec(12, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "pursuit", 1));
    public static final DeferredItem<DiceItem> SKILL_DIE_PURSUIT_3 = register("skill_die_pursuit_3", new Spec(20, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "pursuit", 2));
    public static final DeferredItem<DiceItem> SKILL_DIE_WARD_1 = register("skill_die_ward_1", new Spec(6, DieKind.SKILL, DieMaterial.SKILL, DFRarity.UNCOMMON, DieElement.NONE, "ward", 0));
    public static final DeferredItem<DiceItem> SKILL_DIE_WARD_2 = register("skill_die_ward_2", new Spec(12, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "ward", 1));
    public static final DeferredItem<DiceItem> SKILL_DIE_WARD_3 = register("skill_die_ward_3", new Spec(20, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "ward", 2));
    public static final DeferredItem<DiceItem> SKILL_DIE_BREAKER_1 = register("skill_die_breaker_1", new Spec(6, DieKind.SKILL, DieMaterial.SKILL, DFRarity.UNCOMMON, DieElement.NONE, "breaker", 0));
    public static final DeferredItem<DiceItem> SKILL_DIE_BREAKER_2 = register("skill_die_breaker_2", new Spec(12, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "breaker", 1));
    public static final DeferredItem<DiceItem> SKILL_DIE_BREAKER_3 = register("skill_die_breaker_3", new Spec(20, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "breaker", 2));
    public static final DeferredItem<DiceItem> SKILL_DIE_REVERSE_1 = register("skill_die_reverse_1", new Spec(6, DieKind.SKILL, DieMaterial.SKILL, DFRarity.UNCOMMON, DieElement.NONE, "reverse", 0));
    public static final DeferredItem<DiceItem> SKILL_DIE_REVERSE_2 = register("skill_die_reverse_2", new Spec(12, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "reverse", 1));
    public static final DeferredItem<DiceItem> SKILL_DIE_REVERSE_3 = register("skill_die_reverse_3", new Spec(20, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "reverse", 2));
    public static final DeferredItem<DiceItem> SKILL_DIE_GREED_1 = register("skill_die_greed_1", new Spec(6, DieKind.SKILL, DieMaterial.SKILL, DFRarity.UNCOMMON, DieElement.NONE, "greed", 0));
    public static final DeferredItem<DiceItem> SKILL_DIE_GREED_2 = register("skill_die_greed_2", new Spec(12, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "greed", 1));
    public static final DeferredItem<DiceItem> SKILL_DIE_GREED_3 = register("skill_die_greed_3", new Spec(20, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "greed", 2));
    public static final DeferredItem<DiceItem> SKILL_DIE_LOCK_1 = register("skill_die_lock_1", new Spec(6, DieKind.SKILL, DieMaterial.SKILL, DFRarity.UNCOMMON, DieElement.NONE, "lock", 0));
    public static final DeferredItem<DiceItem> SKILL_DIE_LOCK_2 = register("skill_die_lock_2", new Spec(12, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "lock", 1));
    public static final DeferredItem<DiceItem> SKILL_DIE_LOCK_3 = register("skill_die_lock_3", new Spec(20, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "lock", 2));
    public static final DeferredItem<DiceItem> SKILL_DIE_FAVOR_1 = register("skill_die_favor_1", new Spec(6, DieKind.SKILL, DieMaterial.SKILL, DFRarity.UNCOMMON, DieElement.NONE, "favor", 0));
    public static final DeferredItem<DiceItem> SKILL_DIE_FAVOR_2 = register("skill_die_favor_2", new Spec(12, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "favor", 1));
    public static final DeferredItem<DiceItem> SKILL_DIE_FAVOR_3 = register("skill_die_favor_3", new Spec(20, DieKind.SKILL, DieMaterial.SKILL, DFRarity.EPIC, DieElement.NONE, "favor", 2));

    // ===== 加算骰 6 =====
    public static final DeferredItem<DiceItem> ADD_DIE_EDGE = register("add_die_edge", new Spec(6, DieKind.ADD, DieMaterial.ELEMENTAL, DFRarity.RARE, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> ADD_DIE_WALL = register("add_die_wall", new Spec(6, DieKind.ADD, DieMaterial.ELEMENTAL, DFRarity.RARE, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> ADD_DIE_POISE = register("add_die_poise", new Spec(8, DieKind.ADD, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> ADD_DIE_ELEMENT = register("add_die_element", new Spec(8, DieKind.ADD, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> ADD_DIE_COMBO = register("add_die_combo", new Spec(6, DieKind.ADD, DieMaterial.ELEMENTAL, DFRarity.RARE, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> ADD_DIE_MEND = register("add_die_mend", new Spec(8, DieKind.ADD, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.NONE, null, 0));

    // ===== 乘算骰 6 =====
    public static final DeferredItem<DiceItem> MUL_DIE_FIERCE = register("mul_die_fierce", new Spec(8, DieKind.MUL, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> MUL_DIE_BULWARK = register("mul_die_bulwark", new Spec(8, DieKind.MUL, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> MUL_DIE_PIERCE = register("mul_die_pierce", new Spec(8, DieKind.MUL, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> MUL_DIE_BREAK = register("mul_die_break", new Spec(10, DieKind.MUL, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> MUL_DIE_RESONANCE = register("mul_die_resonance", new Spec(10, DieKind.MUL, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> MUL_DIE_FATE = register("mul_die_fate", new Spec(12, DieKind.MUL, DieMaterial.ELEMENTAL, DFRarity.EPIC, DieElement.NONE, null, 0));

    // ===== 唯一骰 10 =====
    public static final DeferredItem<DiceItem> UNIQUE_DIE_CRACKED_FACE = register("unique_die_cracked_face", new Spec(6, DieKind.UNIQUE, DieMaterial.UNIQUE, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> UNIQUE_DIE_TWO_FACED = register("unique_die_two_faced", new Spec(8, DieKind.UNIQUE, DieMaterial.UNIQUE, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> UNIQUE_DIE_MOLTEN_CORE = register("unique_die_molten_core", new Spec(12, DieKind.UNIQUE, DieMaterial.UNIQUE, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> UNIQUE_DIE_FROST_SILENCE = register("unique_die_frost_silence", new Spec(12, DieKind.UNIQUE, DieMaterial.UNIQUE, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> UNIQUE_DIE_STORM_VERDICT = register("unique_die_storm_verdict", new Spec(12, DieKind.UNIQUE, DieMaterial.UNIQUE, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> UNIQUE_DIE_BONE_EATER = register("unique_die_bone_eater", new Spec(10, DieKind.UNIQUE, DieMaterial.UNIQUE, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> UNIQUE_DIE_RADIANT_OATH = register("unique_die_radiant_oath", new Spec(10, DieKind.UNIQUE, DieMaterial.UNIQUE, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> UNIQUE_DIE_UMBRAL_VEIL = register("unique_die_umbral_veil", new Spec(10, DieKind.UNIQUE, DieMaterial.UNIQUE, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> UNIQUE_DIE_NULL_FACE = register("unique_die_null_face", new Spec(6, DieKind.UNIQUE, DieMaterial.UNIQUE, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> UNIQUE_DIE_FATE_SOVEREIGN = register("unique_die_fate_sovereign", new Spec(20, DieKind.UNIQUE, DieMaterial.UNIQUE, DFRarity.FATED, DieElement.NONE, null, 0));

    // ===== 诅咒骰 4 =====
    public static final DeferredItem<DiceItem> CURSED_DIE_GREED = register("cursed_die_greed", new Spec(12, DieKind.CURSED, DieMaterial.CURSED, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> CURSED_DIE_BLOOD = register("cursed_die_blood", new Spec(10, DieKind.CURSED, DieMaterial.CURSED, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> CURSED_DIE_FRACTURE = register("cursed_die_fracture", new Spec(20, DieKind.CURSED, DieMaterial.CURSED, DFRarity.EPIC, DieElement.NONE, null, 0));
    public static final DeferredItem<DiceItem> CURSED_DIE_MISFORTUNE = register("cursed_die_misfortune", new Spec(8, DieKind.CURSED, DieMaterial.CURSED, DFRarity.EPIC, DieElement.NONE, null, 0));

    private static DeferredItem<DiceItem> register(String path, Spec spec) {
        // 0.3.0：稀有度落到物品属性（名字着色）；命定档额外斜体在 DiceItem.getName/tooltip 处理
        return ITEMS.register(path, id -> new DiceItem(id,
            IdProps.item(id).stacksTo(1).rarity(spec.rarity().vanilla()), spec));
    }

    /** 按 path 查静态 Spec（兜底用）。 */
    public static Spec spec(String path) {
        var entry = ITEMS.getEntries().stream()
            .filter(e -> e.getId().getPath().equals(path))
            .findFirst();
        return entry.map(h -> ((DiceItem) h.value()).spec).orElse(null);
    }

    /** 全部骰物品（创造标签/图鉴用）。 */
    public static java.util.stream.Stream<DiceItem> allDice() {
        return ITEMS.getEntries().stream().map(h -> (DiceItem) h.value());
    }

    private DiceItems() {}
}
