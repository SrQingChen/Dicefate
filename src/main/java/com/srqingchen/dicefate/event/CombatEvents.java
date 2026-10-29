package com.srqingchen.dicefate.event;

import com.srqingchen.dicefate.DFConfig;
import com.srqingchen.dicefate.attachment.MobFateData;
import com.srqingchen.dicefate.attachment.PlayerFateData;
import com.srqingchen.dicefate.combat.CashOutService;
import com.srqingchen.dicefate.combat.ContestRules;
import com.srqingchen.dicefate.combat.ContestService;
import com.srqingchen.dicefate.combat.MomentumService;
import com.srqingchen.dicefate.combat.PoiseService;
import com.srqingchen.dicefate.combat.SnapshotFactory;
import com.srqingchen.dicefate.dice.DieState;
import com.srqingchen.dicefate.network.DFNetwork;
import com.srqingchen.dicefate.registry.ModAttachments;
import com.srqingchen.dicefate.registry.ModDataComponents;
import com.srqingchen.dicefate.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * 战斗事件接线（规划 8.6）：
 * 唯一伤害入口 LivingIncomingDamageEvent；Post 应用命势；Tick 做衰减与同步。
 * 0.2.0：玩家 vs 非 Mob LivingEntity 也完整拼点；召唤物归因到主人；
 * 玩家死亡重置骰势；登录即同步命盘；怪物 tick 按等级去重；时钟统一 gameTime；
 * 兑现致命判定计入吸收护盾。
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = com.srqingchen.dicefate.DiceFate.MODID)
public final class CombatEvents {

    private static long lastCreativeHintTick = Long.MIN_VALUE;

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        DamageSource src = event.getSource();
        LivingEntity target = event.getEntity();
        com.srqingchen.dicefate.util.DFDebug.always("伤害事件: target={} type={} amount={} direct={} owner={}",
            target.getType().toShortString(), src.typeHolder().getRegisteredName(),
            event.getAmount(),
            src.getDirectEntity() == null ? "null" : src.getDirectEntity().getType().toShortString(),
            src.getEntity() == null ? "null" : src.getEntity().getType().toShortString());

        // 兑现窗口内致命（计入吸收护盾）→ 图腾效果
        // 0.3.0 A8：移到 canContest 之前——环境伤害（摔落/岩浆/虚空等无主来源）同样触发保命
        if (target instanceof ServerPlayer sp) {
            PlayerFateData data = sp.getData(ModAttachments.PLAYER_FATE);
            float amount = event.getAmount();
            if (data.isCashoutArmed() && amount >= sp.getHealth() + sp.getAbsorptionAmount()) {
                if (CashOutService.onFatal(sp, data)) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        LivingEntity attacker = ContestRules.resolveAttacker(src);
        long tick = target.level().getGameTime();
        if (!ContestRules.canContest(attacker, target, src)) {
            String reason;
            if (com.srqingchen.dicefate.combat.RecursionGuard.isResolving()) reason = "递归保护(depth=" + com.srqingchen.dicefate.combat.RecursionGuard.depth() + ")";
            else if (src.is(com.srqingchen.dicefate.combat.ContestRules.BYPASS_CONTEST)) reason = "黑名单伤害";
            else if (attacker == null) reason = "无主来源(环境)";
            else if (attacker == target) reason = "自伤";
            else if (target.getType().builtInRegistryHolder().is(com.srqingchen.dicefate.combat.ContestRules.NO_CONTEST)) reason = "no_contest标签";
            else if (attacker instanceof Player ap && (ap.isCreative() || ap.isSpectator())) {
                reason = "创造/旁观";
                // 0.4.0 P3：创造模式攻击可拼点目标时给行动栏提示（10s 一次，防刷屏）
                if (attacker instanceof ServerPlayer sp2 && tick - lastCreativeHintTick > 200) {
                    lastCreativeHintTick = tick;
                    sp2.sendOverlayMessage(Component.translatable("message.dicefate.creative_no_contest"));
                }
            } else if (!attacker.isAlive() || !target.isAlive()) reason = "已死亡";
            else reason = "其他";
            com.srqingchen.dicefate.util.DFDebug.always("不拼点: attacker={} 原因={}",
                attacker == null ? "null" : attacker.getType().toShortString(), reason);
            return;
        }
        com.srqingchen.dicefate.util.DFDebug.always("进入拼点: attacker={} -> target={}",
            attacker.getType().toShortString(), target.getType().toShortString());

        ContestService.LivingIncomingEventLike view = new ContestService.LivingIncomingEventLike() {
            @Override public float getAmount() { return event.getAmount(); }
            @Override public void setAmount(float v) { event.setAmount(v); }
            @Override public void setCanceled(boolean v) { event.setCanceled(v); }
            @Override public boolean isCanceled() { return event.isCanceled(); }
        };

        if (attacker instanceof ServerPlayer player && target != player) {
            // 玩家有源伤害一律完整拼点（Mob 或其他 LivingEntity；PvP 默认关闭）
            if (target instanceof Player) return;
            ContestService.resolvePlayerAttack(view, player, target, src);
        } else if (attacker instanceof Mob mob && target instanceof ServerPlayer player) {
            ContestService.resolveMobAttackOnPlayer(view, mob, player, event.getAmount(), src);
        }
        // 怪 vs 怪：默认不拼点（config 可开但 0.2.0 无行为）
    }

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof Mob mob) {
            com.srqingchen.dicefate.util.DFDebug.log("伤害落地(Post): mob={} dealt={}",
                mob.getType().toShortString(), event.getInflictedDamage());
            ContestService.onDamagePost(mob, event.getInflictedDamage());
        }
        if (!event.getEntity().isAlive()) {
            DamageSource src = event.getSource();
            if (src.getEntity() instanceof ServerPlayer killer) {
                PlayerFateData data = killer.getData(ModAttachments.PLAYER_FATE);
                if (data.playerRune() != null && data.playerRune().getPath().equals("rune_hunt")) {
                    killer.heal(2.0F);
                }
                var type = net.minecraft.resources.Identifier.parse(
                    event.getEntity().getType().builtInRegistryHolder().getRegisteredName());
                data.addKill(type);
                // 0.3.0 F1：命损窗口击杀 / 零面者击杀
                if (event.getEntity() instanceof Mob deadMob) {
                    var mobData = deadMob.getData(ModAttachments.MOB_FATE);
                    if (mobData.woundTicks() > 0) {
                        com.srqingchen.dicefate.util.AdvancementGrant.grant(killer, "fatewound");
                    }
                }
                if (type.getPath().equals("nullface")) {
                    com.srqingchen.dicefate.util.AdvancementGrant.grant(killer, "nullface");
                }
            }
        }
    }

    /** 玩家死亡：骰势归零、兑现解除（规划 3.2.1）。 */
    @SubscribeEvent
    public static void onLivingDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerFateData data = sp.getData(ModAttachments.PLAYER_FATE);
            long tick = sp.level().getGameTime();
            data.setMomentum(0, tick);
            data.disarmCashout();
        }
        if (event.getEntity() instanceof Mob mob) {
            PoiseService.cleanup(mob);
        }
    }

    /** 登录：即时同步命盘与状态（消除最长 5s 空窗）。 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerFateData data = sp.getData(ModAttachments.PLAYER_FATE);
        long tick = sp.level().getGameTime();
        // 0.3.2 诊断：数据注册表自检（若 die/pool_profile 计数为 0，快照将为空 → 全部 Miss）
        try {
            var regs = sp.level().registryAccess();
            int dice = regs.lookupOrThrow(com.srqingchen.dicefate.data.DFRegistries.DIE).registryKeySet().size();
            int pools = regs.lookupOrThrow(com.srqingchen.dicefate.data.DFRegistries.POOL_PROFILE).registryKeySet().size();
            int packs = regs.lookupOrThrow(com.srqingchen.dicefate.data.DFRegistries.PACK).registryKeySet().size();
            int reacts = regs.lookupOrThrow(com.srqingchen.dicefate.data.DFRegistries.REACTION).registryKeySet().size();
            com.srqingchen.dicefate.util.DFDebug.always(
                "登录自检: die注册表={} pool_profile={} pack={} reaction={} 玩家[骰位={} 已装备骰={} 命势档案={} 骰势={}]",
                dice, pools, packs, reacts, data.getBaseSlots(), countEquipped(data), data.pool().baseAt(0).isEmpty() ? "空" : "有", data.getMomentum());
        } catch (Exception ex) {
            com.srqingchen.dicefate.util.DFDebug.err("登录自检失败: {}", ex.toString());
        }
        DFNetwork.sendPool(sp, data);
        DFNetwork.sendPouch(sp, data);
        DFNetwork.sendFateState(sp, data, tick);
    }

    private static int countEquipped(PlayerFateData data) {
        int n = 0;
        for (ItemStack s : data.pool().base()) if (!s.isEmpty()) n++;
        return n;
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        // Boss 首杀补发（0.3.0 F5 验收修复②：以档案 rank=BOSS 判定——实体注册名无 boss_ 前缀）
        var mobFate = mob.getData(ModAttachments.MOB_FATE);
        if (mobFate.profile() != null
            && mobFate.profile().rank() == com.srqingchen.dicefate.data.PoolProfile.Rank.BOSS) {
            var profileId = mobFate.profile().id();
            var firstKills = com.srqingchen.dicefate.data.FirstKillData.get(level);
            if (firstKills.markFirstKill(profileId)) {
                boolean anyUnique = false;
                for (var drop : bossFirstKillDrops(profileId.getPath())) {
                    var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(drop);
                    if (item.isPresent()) {
                        var itemVal = item.get().value();
                        event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                            level, mob.getX(), mob.getY() + 0.5, mob.getZ(), new ItemStack(itemVal)));
                        if (itemVal instanceof com.srqingchen.dicefate.item.DiceItem d
                            && d.spec.kind() == com.srqingchen.dicefate.dice.DieKind.UNIQUE) {
                            anyUnique = true;
                        }
                    }
                }
                // 0.3.0 验收风险⑪：首杀直给的唯一骰也触发进度
                if (anyUnique) com.srqingchen.dicefate.util.AdvancementGrant.grant(killer, "unique_die");
            }
        }
        var type = net.minecraft.resources.Identifier.parse(
            mob.getType().builtInRegistryHolder().getRegisteredName());
        // 档案驱动掉落（命砂/命尘/精华；Boss 档案 drops 已移除——统一走 loot_table）
        var profile = SnapshotFactory.profileFor(mob, level.registryAccess());
        if (profile == null) return;
        var rng = mob.getRandom();
        var itemReg = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        for (var drop : profile.drops()) {
            if (rng.nextDouble() < drop.chance()) {
                int count = drop.min() + rng.nextInt(Math.max(1, drop.max() - drop.min() + 1));
                if (count <= 0) continue;
                String raw = drop.item().contains(":") ? drop.item() : "dicefate:" + drop.item();
                var item = itemReg.get(net.minecraft.resources.Identifier.parse(raw));
                if (item.isPresent()) {
                    event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                        level, mob.getX(), mob.getY() + 0.5, mob.getZ(), new ItemStack(item.get(), count)));
                }
            }
        }
    }

    /** Boss 首杀专属掉落（策划 6.3；常规掉落在 loot_table）。 */
    private static java.util.List<net.minecraft.resources.Identifier> bossFirstKillDrops(String path) {
        return switch (path) {
            case "boss_two_faced_gambler" -> java.util.List.of(
                com.srqingchen.dicefate.DiceFate.id("sigil_addition"),
                com.srqingchen.dicefate.DiceFate.id("unique_die_two_faced"),
                com.srqingchen.dicefate.DiceFate.id("skill_die_reverse_2"));
            case "boss_molten_die_king" -> java.util.List.of(
                com.srqingchen.dicefate.DiceFate.id("sigil_multiplication"),
                com.srqingchen.dicefate.DiceFate.id("unique_die_molten_core"),
                com.srqingchen.dicefate.DiceFate.id("skill_die_breaker_3"));
            case "boss_frost_silence_queen" -> java.util.List.of(
                com.srqingchen.dicefate.DiceFate.id("unique_die_frost_silence"),
                com.srqingchen.dicefate.DiceFate.id("blank_die_starlight"),
                com.srqingchen.dicefate.DiceFate.id("skill_die_ward_3"));
            case "boss_nullface" -> java.util.List.of(
                com.srqingchen.dicefate.DiceFate.id("sovereign_core"),
                com.srqingchen.dicefate.DiceFate.id("unique_die_null_face"),
                com.srqingchen.dicefate.DiceFate.id("unique_die_fate_sovereign"),
                com.srqingchen.dicefate.DiceFate.id("blank_die_starlight"),
                com.srqingchen.dicefate.DiceFate.id("face_plate_d24"));
            default -> java.util.List.of();
        };
    }

    // ==================== Tick ====================

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        var server = event.getServer();
        long tick = server.overworld().getGameTime();
        if (tick % 10 != 0) return;
        ContestService.purgePending(tick);

        // 怪物 tick 按 tick 全局去重（0.2.0 验收修复项4：跨玩家不重复 tick 同一怪）
        Set<Mob> tickedMobs = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerFateData data = player.getData(ModAttachments.PLAYER_FATE);
            if (tick % 20 == 0) {
                MomentumService.decay(player, data, tick);
                if (data.isCashoutArmed() && tick > data.cashoutExpireTick()) {
                    CashOutService.onExpire(player, data);
                }
                // 离开战斗 30s：重置每场一次标记（幸运骰坠/命纹/定命次数/裂面禁用）
                if (data.lastContestTick() != Long.MIN_VALUE && tick - data.lastContestTick() > 600) {
                    data.resetBattleOnceFlags();
                }
                // 韧纹对账：装备期间护甲 +1（0.3.0）
                ContestService.reconcileTough(player, data);
            }
            DFNetwork.sendFateState(player, data, tick);
            if (data.sightTicks() > 0) {
                data.setSightTicks(Math.max(0, data.sightTicks() - 10));
                // 命盘镜：常驻视界
                if (com.srqingchen.dicefate.compat.CuriosCompat.hasFateMirror(player)) {
                    data.setSightTicks(40);
                }
            } else if (com.srqingchen.dicefate.compat.CuriosCompat.hasFateMirror(player)) {
                data.setSightTicks(40);
            }

            // 怪物 tick：按维度去重（0.2.0 A5：多玩家不再重复 tick 同一怪）
            if (player.level() instanceof ServerLevel level) {
                for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(40))) {
                    if (!tickedMobs.add(mob)) continue;
                    var mobData = mob.getData(ModAttachments.MOB_FATE);
                    if (!mobData.isInitialized()) {
                        var profile = SnapshotFactory.profileFor(mob, level.registryAccess());
                        if (profile != null) PoiseService.ensureInit(mob, profile);
                        continue;
                    }
                    // 0.3.0：带命损的怪也要推进 tick（否则离场后 +15% 易伤永不过期）
                    if (mobData.state() != MobFateData.State.NORMAL
                        || mobData.woundTicks() > 0
                        || tick - mobData.lastCombatTick() < 200) {
                        PoiseService.tick(mob, tick);
                    }
                }
            }
        }
    }

    /** 首次进入：初始 1 骰位 d6 + 8 命砂；周期性补发命盘同步。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerFateData data = sp.getData(ModAttachments.PLAYER_FATE);
        long tick = sp.level().getGameTime();
        if (!data.isKitGiven()) {
            data.markKitGiven();
            com.srqingchen.dicefate.util.DFDebug.log("初始礼包发放: 玩家={}", sp.getName().getString());
            if (data.pool().baseAt(0).isEmpty()) {
                ItemStack die = new ItemStack(com.srqingchen.dicefate.registry.DiceItems.DIE_BONE_D6.get());
                die.set(ModDataComponents.DIE_STATE.get(), DieState.fresh());
                data.pool().setBase(0, die);
            }
            sp.getInventory().add(new ItemStack(ModItems.FATE_SAND.get(), 8));
            DFNetwork.sendPool(sp, data);
            DFNetwork.sendFateState(sp, data, tick);
        }
        if (tick % 100 == 0) {
            DFNetwork.sendPool(sp, data);
            // 0.3.0 验收修复⑥：全图鉴进度（收集 83 种骰）
            if (data.codex().size() >= 83) {
                com.srqingchen.dicefate.util.AdvancementGrant.grant(sp, "full_codex");
            }
        }
    }

    private CombatEvents() {}
}
