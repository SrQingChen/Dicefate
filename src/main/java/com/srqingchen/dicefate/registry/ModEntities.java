package com.srqingchen.dicefate.registry;

import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.entity.boss.FrostSilenceQueen;
import com.srqingchen.dicefate.entity.boss.MoltenDieKing;
import com.srqingchen.dicefate.entity.boss.Nullface;
import com.srqingchen.dicefate.entity.boss.TwoFacedGambler;
import com.srqingchen.dicefate.entity.projectile.DieProjectile;
import com.srqingchen.dicefate.entity.projectile.FateArrow;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 实体注册：仅 4 Boss + 2 投射物。 */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.createEntities(DiceFate.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<TwoFacedGambler>> TWO_FACED_GAMBLER =
        ENTITIES.register("two_faced_gambler", () -> EntityType.Builder
            .<TwoFacedGambler>of(TwoFacedGambler::new, MobCategory.MONSTER)
            .sized(0.7F, 2.1F).eyeHeight(1.9F).clientTrackingRange(10)
            .build(key("two_faced_gambler")));

    public static final DeferredHolder<EntityType<?>, EntityType<MoltenDieKing>> MOLTEN_DIE_KING =
        ENTITIES.register("molten_die_king", () -> EntityType.Builder
            .<MoltenDieKing>of(MoltenDieKing::new, MobCategory.MONSTER)
            .sized(1.1F, 2.4F).eyeHeight(2.0F).clientTrackingRange(10)
            .build(key("molten_die_king")));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostSilenceQueen>> FROST_SILENCE_QUEEN =
        ENTITIES.register("frost_silence_queen", () -> EntityType.Builder
            .<FrostSilenceQueen>of(FrostSilenceQueen::new, MobCategory.MONSTER)
            .sized(0.7F, 2.3F).eyeHeight(2.0F).clientTrackingRange(10)
            .build(key("frost_silence_queen")));

    public static final DeferredHolder<EntityType<?>, EntityType<Nullface>> NULLFACE =
        ENTITIES.register("nullface", () -> EntityType.Builder
            .<Nullface>of(Nullface::new, MobCategory.MONSTER)
            .sized(0.8F, 2.6F).eyeHeight(2.2F).clientTrackingRange(12)
            .build(key("nullface")));

    public static final DeferredHolder<EntityType<?>, EntityType<DieProjectile>> DIE_PROJECTILE =
        ENTITIES.register("die_projectile", () -> EntityType.Builder
            .<DieProjectile>of(DieProjectile::new, MobCategory.MISC)
            .sized(0.4F, 0.4F).eyeHeight(0.1F).clientTrackingRange(6).updateInterval(20)
            .build(key("die_projectile")));

    public static final DeferredHolder<EntityType<?>, EntityType<FateArrow>> FATE_ARROW =
        ENTITIES.register("fate_arrow", () -> EntityType.Builder
            .<FateArrow>of(FateArrow::new, MobCategory.MISC)
            .sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20)
            .build(key("fate_arrow")));

    private static ResourceKey<EntityType<?>> key(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, DiceFate.id(path));
    }

    private ModEntities() {}
}
