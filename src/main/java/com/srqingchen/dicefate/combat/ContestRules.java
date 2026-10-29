package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.DFConfig;
import com.srqingchen.dicefate.DiceFate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 拼点触发规则（规划 2.1）。
 * 0.2.0：no_contest 实体黑名单；召唤物（OwnableEntity）向上归因到主人；
 * 轻拼接入 config（怪物 projectile/爆炸类）。
 */
public final class ContestRules {
    /** 不拼点的伤害类型（自定义命破等）。 */
    public static final TagKey<DamageType> BYPASS_CONTEST = TagKey.create(Registries.DAMAGE_TYPE, DiceFate.id("bypass_contest"));
    /** 不拼点的实体类型（规划 2.1.1 条件 7）。 */
    public static final TagKey<net.minecraft.world.entity.EntityType<?>> NO_CONTEST =
        TagKey.create(Registries.ENTITY_TYPE, DiceFate.id("no_contest"));

    private ContestRules() {}

    /** 是否可拼点。 */
    public static boolean canContest(LivingEntity attacker, LivingEntity defender, DamageSource src) {
        if (RecursionGuard.isResolving()) return false;
        if (src.is(BYPASS_CONTEST)) return false;
        if (attacker == null || defender == null) return false;
        if (attacker.level().isClientSide()) return false;
        if (attacker == defender) return false;
        if (defender.getType().builtInRegistryHolder().is(NO_CONTEST)) return false;
        if (attacker instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (defender instanceof Player pd && pd.isSpectator()) return false;
        return attacker.isAlive() && defender.isAlive();
    }

    /** 归因：找到"拥有者"（玩家/生物）；召唤物（狼/傀儡等 OwnableEntity）向上追溯到主人（规划 2.1.1 条件 3）。 */
    public static LivingEntity resolveAttacker(DamageSource src) {
        LivingEntity direct = resolveOwnerChain(src.getEntity());
        if (direct != null) return direct;
        return resolveOwnerChain(src.getDirectEntity());
    }

    private static LivingEntity resolveOwnerChain(net.minecraft.world.entity.Entity entity) {
        int depth = 0;
        while (entity instanceof LivingEntity le && depth++ < 4) {
            if (le.isAlive()) {
                if (le instanceof OwnableEntity own && own.getOwner() instanceof LivingEntity owner) {
                    // 召唤物 → 主人（若主人死亡/离线则退回实体本身）
                    if (owner.isAlive()) {
                        entity = owner;
                        continue;
                    }
                }
                return le;
            }
            return null;
        }
        return null;
    }

    /** 玩家有源伤害判定（近战/弹射/爆炸 owner/召唤物）。 */
    public static boolean isPlayerSourced(DamageSource src) {
        return resolveAttacker(src) instanceof Player;
    }

    /** 怪物远程/小技能 → 轻拼（仅用第 1 槽）。0.2.0：接入 config。 */
    public static boolean isLightContest(DamageSource src) {
        if (!DFConfig.LIGHT_CONTEST_FOR_MOB_RANGED.get()) return false;
        return src.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
            || src.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
    }

    public static boolean isBossOrElite(Mob mob) {
        var data = mob.getData(com.srqingchen.dicefate.registry.ModAttachments.MOB_FATE);
        if (data.isInitialized() && data.profile() != null) {
            return data.profile().rank() != com.srqingchen.dicefate.data.PoolProfile.Rank.NORMAL;
        }
        return false;
    }
}
