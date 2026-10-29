package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.attachment.MobFateData;
import com.srqingchen.dicefate.attachment.PlayerFateData;
import com.srqingchen.dicefate.data.DFRegistries;
import com.srqingchen.dicefate.data.PackDefinition;
import com.srqingchen.dicefate.data.PoolProfile;
import com.srqingchen.dicefate.item.DiceItem;
import com.srqingchen.dicefate.dice.DiceLookup;
import com.srqingchen.dicefate.dice.DieDefinition;
import com.srqingchen.dicefate.dice.DieElement;
import com.srqingchen.dicefate.dice.DieState;
import com.srqingchen.dicefate.dice.SkillFamily;
import com.srqingchen.dicefate.registry.ModAttachments;
import com.srqingchen.dicefate.registry.ModDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * 快照工厂：玩家命盘/骰包/定命/兑现 → PoolSnapshot；怪物档案 → PoolSnapshot。
 * 0.2.0：骰包"整个替换"战斗骰池（不再逐槽顶替+回落）；定命骰消费打标记；
 * 技能骰视图带实际骰面；非 Mob LivingEntity 通用快照（玩家 vs 盔甲架等完整拼点）。
 */
public final class SnapshotFactory {
    private SnapshotFactory() {}

    // ==================== 玩家侧 ====================

    public static PoolSnapshot playerSnapshot(ServerPlayer player, PlayerFateData data, boolean attacking,
                                              double coRollChance) {
        PoolSnapshot snap = new PoolSnapshot();
        snap.coRollChance = coRollChance;
        HolderLookup.Provider regs = player.level().registryAccess();

        // 骰包：临时替换整个战斗骰池（规划 3.8.1；0.3.0 A5：槽位数不超过玩家基础骰位）
        PackDefinition pack = null;
        if (data.activePack() != null) {
            pack = regs.lookup(DFRegistries.PACK)
                .flatMap(r -> r.get(ResourceKey.create(DFRegistries.PACK, data.activePack())))
                .map(h -> h.value()).orElse(null);
        }

        if (pack != null) {
            int count = Math.min(pack.slots().size(), data.getBaseSlots());
            for (int i = 0; i < count; i++) {
                var ps = pack.slots().get(i);
                DieElement elem = DieElement.byName(ps.element());
                snap.slots.add(new PoolSnapshot.SlotView(i, packDieDef(ps.face(), ps.faceValues(), elem), null));
            }
        } else {
            for (int i = 0; i < data.getBaseSlots(); i++) {
                ItemStack stack = data.pool().baseAt(i);
                if (stack.isEmpty()) continue;
                if (!(stack.getItem() instanceof DiceItem diceItem)) continue;
                DieDefinition def = DiceLookup.definition(regs, diceItem.dieId);
                DieState state = stack.get(ModDataComponents.DIE_STATE.get());
                if (def == null) continue;
                PoolSnapshot.SlotView sv = new PoolSnapshot.SlotView(i, def, state);
                // 被冰封的骰位 / 咒纹裂面（本场禁用）
                if (data.frozenSlotIdx() == i) sv.frozen = true;
                if (state != null && data.isCrackedThisBattle(state.uuid())) sv.frozen = true;
                // 定命：锁定的基础骰（首场首拼）
                if (data.lockSlot() == i && !data.isLockSpent() && data.lockValue() > 0) {
                    sv.forcedValue = data.lockValue();
                }
                // 定命骰：下次该骰取最大面（未消费才生效——0.3.0 A1：消费后不再强制）
                if (data.skillLockSlot() == i && !data.isSkillLockConsumed()) {
                    sv.forcedValue = def.maxFace();
                    data.markSkillLockConsumed();
                }
                snap.slots.add(sv);
            }
        }

        // 加算槽
        if (data.isAddSlotUnlocked() && !data.pool().addDie().isEmpty()
            && data.pool().addDie().getItem() instanceof DiceItem add) {
            DieDefinition def = DiceLookup.definition(regs, add.dieId);
            if (def != null) snap.addSlots.add(new PoolSnapshot.SlotView(10, def,
                data.pool().addDie().get(ModDataComponents.DIE_STATE.get())));
        }
        // 乘算槽
        if (data.isMulSlotUnlocked() && !data.pool().mulDie().isEmpty()
            && data.pool().mulDie().getItem() instanceof DiceItem mul) {
            DieDefinition def = DiceLookup.definition(regs, mul.dieId);
            if (def != null) snap.mulSlots.add(new PoolSnapshot.SlotView(11, def,
                data.pool().mulDie().get(ModDataComponents.DIE_STATE.get())));
        }
        // 技能骰
        for (int s = 0; s < 2; s++) {
            ItemStack sk = data.pool().skillAt(s);
            if (sk.isEmpty() || !(sk.getItem() instanceof DiceItem dice)) continue;
            DieDefinition def = DiceLookup.definition(regs, dice.dieId);
            if (def != null && def.skillFamily() != null) {
                snap.skills.add(new PoolSnapshot.SkillView(def.id(), def.skillFamily(), def.skillTier(), def.face()));
            }
        }

        snap.stance = data.stance();
        snap.mastery = data.stanceMastery(data.stance());
        return snap;
    }

    private static DieDefinition packDieDef(int face, List<Integer> faceValues, DieElement elem) {
        return new DieDefinition(
            Identifier.fromNamespaceAndPath("dicefate", "pack_die"),
            face, elem.isReal() ? com.srqingchen.dicefate.dice.DieKind.ELEMENT : com.srqingchen.dicefate.dice.DieKind.BASIC,
            com.srqingchen.dicefate.dice.DieMaterial.IRON,
            com.srqingchen.dicefate.dice.DFRarity.UNCOMMON, elem,
            faceValues == null ? List.of() : faceValues, Optional.empty(), 0);
    }

    // ==================== 怪物侧 ====================

    @Nullable
    public static PoolProfile profileFor(Mob mob, HolderLookup.Provider regs) {
        Identifier type = Identifier.parse(mob.getType().builtInRegistryHolder().getRegisteredName());
        return regs.lookup(DFRegistries.POOL_PROFILE)
            .flatMap(r -> r.get(ResourceKey.create(DFRegistries.POOL_PROFILE, type)))
            .map(h -> h.value())
            .orElse(null);
    }

    public static PoolSnapshot mobSnapshot(Mob mob, MobFateData data, boolean lightContest) {
        PoolSnapshot snap = new PoolSnapshot();
        PoolProfile profile = data.profile();
        if (profile == null) {
            // 兜底档案（规划 8.10.4）
            int slots = (int) Math.max(1, Math.min(6, 1 + mob.getMaxHealth() / 40));
            int face = (int) Math.max(4, Math.min(12, 4 + mob.getMaxHealth() / 20));
            for (int i = 0; i < slots; i++) {
                snap.slots.add(new PoolSnapshot.SlotView(i, mobDieDef(face, DieElement.NONE), null));
            }
            snap.stance = PoolProfile.Stance.BALANCED;
            snap.exhausted = data.isExhausted();
            return snap;
        }
        List<PoolProfile.SlotSpec> slots = profile.slots();
        for (int i = 0; i < slots.size(); i++) {
            if (lightContest && i > 0) break;   // 轻拼只用第 1 槽
            PoolProfile.SlotSpec spec = slots.get(i);
            DieElement elem = DieElement.byName(spec.element());
            snap.slots.add(new PoolSnapshot.SlotView(i, mobDieDef(spec.face(), elem), null));
        }
        snap.stance = profile.stance();
        snap.exhausted = data.isExhausted();
        for (Identifier skill : profile.skillDice()) {
            SkillFamily fam = SkillFamily.byName(skill.getPath().replace("skill/", ""));
            snap.skills.add(new PoolSnapshot.SkillView(skill, fam, 0, 6));
        }
        return snap;
    }

    /** 0.2.0：非 Mob LivingEntity（盔甲架等）的防御快照——单槽 d6 兜底。 */
    public static PoolSnapshot genericSnapshot(LivingEntity entity) {
        PoolSnapshot snap = new PoolSnapshot();
        int face = (int) Math.max(4, Math.min(12, 4 + entity.getMaxHealth() / 20));
        snap.slots.add(new PoolSnapshot.SlotView(0, mobDieDef(face, DieElement.NONE), null));
        snap.stance = PoolProfile.Stance.BALANCED;
        return snap;
    }

    private static DieDefinition mobDieDef(int face, DieElement elem) {
        return new DieDefinition(
            Identifier.fromNamespaceAndPath("dicefate", "mob_die"),
            face, com.srqingchen.dicefate.dice.DieKind.BASIC,
            com.srqingchen.dicefate.dice.DieMaterial.BONE,
            com.srqingchen.dicefate.dice.DFRarity.COMMON, elem,
            List.of(), Optional.empty(), 0);
    }
}
