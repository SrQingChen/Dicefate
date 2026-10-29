package com.srqingchen.dicefate.entity.boss;

import com.srqingchen.dicefate.combat.PoiseService;
import com.srqingchen.dicefate.data.DFRegistries;
import com.srqingchen.dicefate.data.PoolProfile;
import com.srqingchen.dicefate.registry.ModAttachments;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Boss 基类：血条、命盘档案绑定、近战 AI 骨架。
 * 专属机制（抛硬币/熔铸/冰封/归零）由 ContestService 按实体类型挂钩。
 */
public abstract class BossEntity extends Monster {
    private final ServerBossEvent bossEvent;
    @Nullable
    private PoolProfile cachedProfile;

    protected BossEntity(EntityType<? extends Monster> type, Level level, String barColorKey) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(net.minecraft.util.Mth.createInsecureUUID(this.random),
            this.getDisplayName(), BossEvent.BossBarColor.valueOf(barColorKey), BossEvent.BossBarOverlay.PROGRESS);
        this.xpReward = 100;
        this.setPersistenceRequired();
    }

    /** 数据包档案 id（data/dicefate/pool_profile/<id>.json）。 */
    protected abstract Identifier profileId();

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 200.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.FOLLOW_RANGE, 48.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;
        // 惰性绑定档案（数据包就绪后）
        var data = this.getData(ModAttachments.MOB_FATE);
        if (!data.isInitialized()) {
            PoolProfile profile = lookupProfile(this.level().registryAccess());
            if (profile != null) {
                PoiseService.ensureInit(this, profile);
            }
        } else {
            PoiseService.tick(this, this.level().getGameTime());
        }
        // 阶段 2（50% 血量）：命势上限 +5%
        if (data.isInitialized() && data.profile() != null) {
            float frac = this.getHealth() / this.getMaxHealth();
            if (frac <= 0.5f && !phaseTwo) {
                phaseTwo = true;
                onPhaseTwo();
            }
        }
    }

    private boolean phaseTwo;

    /** 阶段 2（50% 血）钩子。 */
    protected void onPhaseTwo() {
        var data = this.getData(ModAttachments.MOB_FATE);
        if (data.isInitialized()) {
            int newMax = (int) (data.maxPoise() * 1.05);
            data.init(data.profile(), newMax, data.decreeThreshold());
        }
    }

    public boolean isPhaseTwo() { return phaseTwo; }

    @Nullable
    public PoolProfile lookupProfile(HolderLookup.Provider provider) {
        if (cachedProfile != null) return cachedProfile;
        var key = net.minecraft.resources.ResourceKey.create(DFRegistries.POOL_PROFILE, profileId());
        PoolProfile p = provider.lookup(DFRegistries.POOL_PROFILE)
            .flatMap(reg -> reg.get(key))
            .map(ref -> ref.value())
            .orElse(null);
        cachedProfile = p;
        return p;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        this.bossEvent.setName(this.getDisplayName());
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }

    @Override
    public void die(DamageSource source) {
        PoiseService.cleanup(this);
        super.die(source);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("PhaseTwo", phaseTwo);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        phaseTwo = input.getBooleanOr("PhaseTwo", false);
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }
}
