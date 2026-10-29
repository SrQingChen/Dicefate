package com.srqingchen.dicefate.network;

import com.srqingchen.dicefate.DFConfig;
import com.srqingchen.dicefate.DiceFate;
import com.srqingchen.dicefate.attachment.MobFateData;
import com.srqingchen.dicefate.attachment.PlayerFateData;
import com.srqingchen.dicefate.data.PoolProfile;
import com.srqingchen.dicefate.registry.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/** 网络装配与发送门面。 */
public final class DFNetwork {

    public static void register(IEventBus modBus) {
        modBus.addListener(DFNetwork::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1");
        reg.playToClient(ContestPayload.TYPE, ContestPayload.STREAM_CODEC, (payload, ctx) ->
            ctx.enqueueWork(() -> com.srqingchen.dicefate.client.ClientFateData.onContest(payload)));
        reg.playToClient(FateStatePayload.TYPE, FateStatePayload.STREAM_CODEC, (payload, ctx) ->
            ctx.enqueueWork(() -> com.srqingchen.dicefate.client.ClientFateData.onFateState(payload)));
        reg.playToClient(PoiseSyncPayload.TYPE, PoiseSyncPayload.STREAM_CODEC, (payload, ctx) ->
            ctx.enqueueWork(() -> com.srqingchen.dicefate.client.ClientFateData.onPoiseSync(payload)));
        reg.playToClient(CombatTextPayload.TYPE, CombatTextPayload.STREAM_CODEC, (payload, ctx) ->
            ctx.enqueueWork(() -> com.srqingchen.dicefate.client.ClientFateData.onCombatText(payload)));
        reg.playToClient(PoolSyncPayload.TYPE, PoolSyncPayload.STREAM_CODEC, (payload, ctx) ->
            ctx.enqueueWork(() -> com.srqingchen.dicefate.client.ClientFateData.onPoolSync(payload)));
        reg.playToClient(PouchSyncPayload.TYPE, PouchSyncPayload.STREAM_CODEC, (payload, ctx) ->
            ctx.enqueueWork(() -> com.srqingchen.dicefate.client.ClientFateData.onPouchSync(payload)));
        reg.playToServer(ClientActionPayload.TYPE, ClientActionPayload.STREAM_CODEC, (payload, ctx) ->
            ctx.enqueueWork(() -> ServerActions.handle(ctx.player() instanceof ServerPlayer sp ? sp : null, payload)));
    }

    // ==================== 发送门面 ====================

    /** 拼点结果：参与者 + 观战者。 */
    public static void sendContest(LivingEntity a, LivingEntity b, ContestPayload payload) {
        int range = DFConfig.CONTEST_HUD_RANGE.get();
        if (a.level() instanceof ServerLevel level) {
            List<ServerPlayer> targets = new ArrayList<>();
            for (Player p : level.players()) {
                if (p instanceof ServerPlayer sp
                    && (sp == a || sp == b
                        || sp.distanceToSqr(a) < (double) range * range)) {
                    targets.add(sp);
                }
            }
            for (ServerPlayer sp : targets) {
                com.srqingchen.dicefate.util.DFDebug.always("发送拼点包 -> {} (track={} 战果={})",
                    sp.getName().getString(), payload.track(), payload.outcome());
                PacketDistributor.sendToPlayer(sp, payload);
            }
        }
    }

    public static void sendCombatText(ServerLevel level, LivingEntity target, CombatTextPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntity(target, payload);
        // 命中者自身若不在 tracking 范围也给一份（防御拼点时玩家是目标，会被 tracking 覆盖）
    }

    public static void sendFateState(ServerPlayer player, PlayerFateData d, long gameTick) {
        PoolProfile.Stance st = d.stance();
        boolean inCombat = d.lastContestTick() != Long.MIN_VALUE && gameTick - d.lastContestTick() < 160;
        PacketDistributor.sendToPlayer(player, new FateStatePayload(
            new FateStatePayload.Bars(d.getMomentum(), d.getMomentumOverflow(),
                d.isCashoutArmed(), d.cashoutValue(), d.cashoutExpireTick()),
            new FateStatePayload.Meta((byte) st.ordinal(), (byte) d.stanceMastery(st),
                d.skillCdUntil(0), d.skillCdUntil(1),
                (byte) d.getBaseSlots(), d.isAddSlotUnlocked(), d.isMulSlotUnlocked(),
                (byte) d.getSkillSlotsUnlocked(), inCombat),
            new FateStatePayload.Extras(
                d.activePack() == null ? "" : d.activePack().toString(), d.packRemaining(),
                d.isSpiritActive(), (byte) d.spiritLevel(),
                d.lockSlot(), d.lockValue(), gameTick)
        ));
    }

    /** 0.2.0：骰袋内容同步。 */
    public static void sendPouch(ServerPlayer player, PlayerFateData d) {
        PacketDistributor.sendToPlayer(player, new PouchSyncPayload(d.pouchView()));
    }

    public static void sendPool(ServerPlayer player, PlayerFateData d) {
        PacketDistributor.sendToPlayer(player, new PoolSyncPayload(
            (byte) d.getBaseSlots(),
            List.copyOf(d.pool().base()),
            d.pool().addDie(), d.pool().mulDie(), d.pool().skill0(), d.pool().skill1()
        ));
    }

    /** 命势同步：发给 32 格内玩家。 */
    public static void sendPoise(MobFateData data, LivingEntity mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        int range = DFConfig.CONTEST_HUD_RANGE.get();
        PoiseSyncPayload.Entry e = toEntry(data, mob);
        for (Player p : level.players()) {
            if (p instanceof ServerPlayer sp && sp.distanceToSqr(mob) < (double) range * range) {
                PacketDistributor.sendToPlayer(sp, new PoiseSyncPayload(List.of(e)));
            }
        }
    }

    public static PoiseSyncPayload.Entry toEntry(MobFateData data, LivingEntity mob) {
        return new PoiseSyncPayload.Entry(
            mob.getId(), data.poise(), data.maxPoise(),
            (byte) data.state().ordinal(),
            data.decree(), data.decreeThreshold(), data.woundTicks(),
            data.profile() == null ? (byte) 0 : (byte) data.profile().rank().ordinal(),
            (int) mob.level().getGameTime()
        );
    }

    public static MobFateData mobFate(LivingEntity entity) {
        return entity.getData(ModAttachments.MOB_FATE);
    }

    private DFNetwork() {}
}
