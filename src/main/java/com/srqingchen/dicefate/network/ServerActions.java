package com.srqingchen.dicefate.network;

import com.srqingchen.dicefate.attachment.PlayerFateData;
import com.srqingchen.dicefate.combat.CashOutService;
import com.srqingchen.dicefate.data.DFRegistries;
import com.srqingchen.dicefate.data.PoolProfile;
import com.srqingchen.dicefate.dice.DiceLookup;
import com.srqingchen.dicefate.item.DiceItem;
import com.srqingchen.dicefate.dice.DieKind;
import com.srqingchen.dicefate.dice.DieState;
import com.srqingchen.dicefate.dice.DicePool;
import com.srqingchen.dicefate.registry.ModAttachments;
import com.srqingchen.dicefate.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * 服务端意图处理：命盘装配/卸下/排序、姿态切换、预设、定命、兑现、骰灵。
 * 全量校验（战斗中锁定、槽类型、解锁状态、冷却）。
 */
final class ServerActions {

    static void handle(ServerPlayer player, ClientActionPayload payload) {
        if (player == null) return;
        // 0.3.0 验收修复③：恶意包序号越界防护
        if (payload.op() < 0 || payload.op() >= ClientActionPayload.Op.values().length) return;
        com.srqingchen.dicefate.util.DFDebug.log("收到客户端操作: op={} slot={}",
            ClientActionPayload.Op.values()[payload.op()], payload.slotIdx());
        PlayerFateData data = player.getData(ModAttachments.PLAYER_FATE);
        long tick = player.level().getGameTime();

        // 战斗中（最近 8s 有拼点）锁定命盘编辑与姿态
        // 0.2.0 验收修复 BUG B：MIN_VALUE（从未拼点）下减法溢出为负会误判战斗中
        boolean inCombat = data.lastContestTick() != Long.MIN_VALUE
            && tick - data.lastContestTick() < 160;

        switch (ClientActionPayload.Op.values()[payload.op()]) {
            case EQUIP_BASE -> equipBase(player, data, payload, inCombat);
            case UNEQUIP_BASE -> unequipBase(player, data, payload, inCombat);
            case SWAP_BASE -> swapBase(player, data, payload, inCombat);
            case EQUIP_ADD, EQUIP_MUL -> equipSpecial(player, data, payload, inCombat);
            case UNEQUIP_ADD, UNEQUIP_MUL -> unequipSpecial(player, data, payload, inCombat);
            case EQUIP_SKILL, UNEQUIP_SKILL -> skillSlot(player, data, payload, inCombat);
            case SET_STANCE -> setStance(player, data, payload, tick, inCombat);
            case SAVE_PRESET -> savePreset(player, data, payload);
            case LOAD_PRESET -> loadPreset(player, data, payload, inCombat);
            case LOCK_DIE -> lockDie(player, data, payload, inCombat);
            case CASHOUT -> {
                if (!CashOutService.tryArm(player, data)) {
                    player.sendOverlayMessage(Component.translatable("message.dicefate.cashout_fail"));
                }
            }
            case SPIRIT_TOGGLE -> {
                data.setSpiritActive(!data.isSpiritActive());
                DFNetwork.sendFateState(player, data, tick);
            }
            case POUCH_STORE -> pouchStore(player, data, payload);
            case POUCH_TAKE -> pouchTake(player, data, payload);
            case OPEN_CODEX -> { /* 图鉴界面纯客户端 */ }
        }
    }

    // ==================== 骰袋（0.2.0） ====================

    private static void pouchStore(ServerPlayer player, PlayerFateData data, ClientActionPayload payload) {
        if (data.lastContestTick() != Long.MIN_VALUE
            && player.level().getGameTime() - data.lastContestTick() < 160) { denyInCombat(player); return; }
        if (!com.srqingchen.dicefate.item.PouchItem.canUse(player)) {
            player.sendOverlayMessage(Component.translatable("message.dicefate.pouch_not_equipped"));
            return;
        }
        int slot = payload.slotIdx();
        if (slot < 0 || slot > 5) return;
        ItemStack die = payload.stack();
        if (!(die.getItem() instanceof DiceItem diceItem)) return;
        var def = DiceLookup.definition(player.level().registryAccess(), diceItem.dieId);
        if (def == null) return;
        if (consumeFromInventoryByUuid(player, die)) {
            ItemStack old = data.pouchAt(slot);
            data.setPouchAt(slot, die.copy());
            if (!old.isEmpty()) giveBack(player, old);
            DFNetwork.sendPouch(player, data);
        }
    }

    private static void pouchTake(ServerPlayer player, PlayerFateData data, ClientActionPayload payload) {
        if (data.lastContestTick() != Long.MIN_VALUE
            && player.level().getGameTime() - data.lastContestTick() < 160) { denyInCombat(player); return; }
        if (!com.srqingchen.dicefate.item.PouchItem.canUse(player)) return;
        int slot = payload.slotIdx();
        if (slot < 0 || slot > 5) return;
        ItemStack stack = data.pouchAt(slot);
        if (stack.isEmpty()) return;
        data.setPouchAt(slot, ItemStack.EMPTY);
        giveBack(player, stack);
        DFNetwork.sendPouch(player, data);
    }

    /** 按 (item, DieState.uuid) 精确移除背包里的一颗骰。 */
    private static boolean consumeFromInventoryByUuid(ServerPlayer player, ItemStack die) {
        var state = die.get(ModDataComponents.DIE_STATE.get());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() == die.getItem() && s.getCount() == 1) {
                var sState = s.get(ModDataComponents.DIE_STATE.get());
                if (state == null || sState == null || state.uuid().equals(sState.uuid())) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== 基础槽 ====================

    private static void equipBase(ServerPlayer player, PlayerFateData data, ClientActionPayload payload, boolean inCombat) {
        if (inCombat) { denyInCombat(player); return; }
        int slot = payload.slotIdx();
        if (slot < 0 || slot >= data.getBaseSlots()) return;
        ItemStack die = payload.stack();
        if (!(die.getItem() instanceof DiceItem diceItem)) return;
        var def = DiceLookup.definition(player.level().registryAccess(), diceItem.dieId);
        if (def == null || !def.kind().fitsBase()) return;

        // 唯一骰限 1 / 同名骰限 3 / 咒纹锁定检查
        if (!duplicateCheck(player, data, def.id(), def.kind() == DieKind.UNIQUE ? 1 : 3)) {
            player.sendOverlayMessage(Component.translatable("message.dicefate.duplicate_limit"));
            return;
        }
        DicePool pool = data.pool();
        // 手持→骰位：把玩家背包里那颗移除（客户端已同步移除；服务端权威再扣一次）
        consumeFromInventory(player, die);
        ItemStack old = pool.baseAt(slot);
        pool.setBase(slot, die.copy());
        if (!old.isEmpty()) giveBack(player, old);
        DFNetwork.sendPool(player, data);
        DFNetwork.sendFateState(player, data, player.level().getGameTime());
    }

    private static void unequipBase(ServerPlayer player, PlayerFateData data, ClientActionPayload payload, boolean inCombat) {
        if (inCombat) { denyInCombat(player); return; }
        int slot = payload.slotIdx();
        if (slot < 0 || slot >= data.getBaseSlots()) return;
        // 定命中的骰不可卸下
        if (data.lockSlot() == slot) {
            player.sendOverlayMessage(Component.translatable("message.dicefate.locked_die"));
            return;
        }
        ItemStack stack = data.pool().baseAt(slot);
        if (stack.isEmpty()) return;
        data.pool().setBase(slot, ItemStack.EMPTY);
        giveBack(player, stack);
        DFNetwork.sendPool(player, data);
    }

    private static void swapBase(ServerPlayer player, PlayerFateData data, ClientActionPayload payload, boolean inCombat) {
        if (inCombat) { denyInCombat(player); return; }
        // 0.2.0：slotIdx = a*10 + b（两个基础槽 0..5），不再借 stack.count
        int code = payload.slotIdx();
        int a = code / 10, b = code % 10;
        if (code < 0 || a < 0 || b < 0 || a >= 6 || b >= 6 || a == b) return;
        if (a >= data.getBaseSlots() || b >= data.getBaseSlots()) return;   // 0.3.0 验收修复（未解锁槽不可交换）
        DicePool pool = data.pool();
        ItemStack tmp = pool.baseAt(a);
        pool.setBase(a, pool.baseAt(b));
        pool.setBase(b, tmp);
        DFNetwork.sendPool(player, data);
    }

    // ==================== 专属槽 ====================

    private static void equipSpecial(ServerPlayer player, PlayerFateData data, ClientActionPayload payload, boolean inCombat) {
        if (inCombat) { denyInCombat(player); return; }
        boolean add = ClientActionPayload.Op.values()[payload.op()] == ClientActionPayload.Op.EQUIP_ADD;
        if (add && !data.isAddSlotUnlocked()) return;
        if (!add && !data.isMulSlotUnlocked()) return;
        ItemStack die = payload.stack();
        if (!(die.getItem() instanceof DiceItem diceItem)) return;
        var def = DiceLookup.definition(player.level().registryAccess(), diceItem.dieId);
        if (def == null) return;
        DieKind want = add ? DieKind.ADD : DieKind.MUL;
        if (def.kind() != want) return;
        consumeFromInventory(player, die);
        ItemStack old = add ? data.pool().addDie() : data.pool().mulDie();
        if (add) data.pool().setAddDie(die.copy()); else data.pool().setMulDie(die.copy());
        if (!old.isEmpty()) giveBack(player, old);
        DFNetwork.sendPool(player, data);
    }

    private static void unequipSpecial(ServerPlayer player, PlayerFateData data, ClientActionPayload payload, boolean inCombat) {
        if (inCombat) return;
        boolean add = ClientActionPayload.Op.values()[payload.op()] == ClientActionPayload.Op.UNEQUIP_ADD;
        ItemStack stack = add ? data.pool().addDie() : data.pool().mulDie();
        if (stack.isEmpty()) return;
        if (add) data.pool().setAddDie(ItemStack.EMPTY); else data.pool().setMulDie(ItemStack.EMPTY);
        giveBack(player, stack);
        DFNetwork.sendPool(player, data);
    }

    private static void skillSlot(ServerPlayer player, PlayerFateData data, ClientActionPayload payload, boolean inCombat) {
        boolean equip = ClientActionPayload.Op.values()[payload.op()] == ClientActionPayload.Op.EQUIP_SKILL;
        int slot = payload.slotIdx();
        // 0.2.0：兼容客户端 20/21 技能槽编号（协议错位修复）
        if (slot >= 20) slot -= 20;
        if (slot < 0 || slot > 1) return;
        if (equip) {
            if (inCombat) { denyInCombat(player); return; }
            if (slot >= data.getSkillSlotsUnlocked()) return;
            ItemStack die = payload.stack();
            if (!(die.getItem() instanceof DiceItem diceItem)) return;
            var def = DiceLookup.definition(player.level().registryAccess(), diceItem.dieId);
            if (def == null || def.kind() != DieKind.SKILL) return;
            consumeFromInventory(player, die);
            ItemStack old = data.pool().skillAt(slot);
            data.pool().setSkillAt(slot, die.copy());
            if (!old.isEmpty()) giveBack(player, old);
        } else {
            if (inCombat) { denyInCombat(player); return; }
            ItemStack old = data.pool().skillAt(slot);
            if (old.isEmpty()) return;
            data.pool().setSkillAt(slot, ItemStack.EMPTY);
            giveBack(player, old);
        }
        DFNetwork.sendPool(player, data);
    }

    // ==================== 姿态 ====================

    private static void setStance(ServerPlayer player, PlayerFateData data, ClientActionPayload payload, long tick, boolean inCombat) {
        if (payload.slotIdx() < 0 || payload.slotIdx() >= PoolProfile.Stance.values().length) return;   // 0.3.0 验收修复③
        PoolProfile.Stance want = PoolProfile.Stance.values()[payload.slotIdx()];
        if (want == data.stance()) return;
        if (inCombat) {
            if (tick < data.stanceSwapReadyTick()) {
                player.sendOverlayMessage(Component.translatable("message.dicefate.stance_cd"));
                return;
            }
            data.setStanceSwapReadyTick(tick + 200);      // 10s 冷却
            data.setSwapHardUntilTick(tick + 30);         // 1.5s 换势硬直
        }
        data.setStance(want);
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
        DFNetwork.sendFateState(player, data, tick);
    }

    // ==================== 预设 ====================

    private static void savePreset(ServerPlayer player, PlayerFateData data, ClientActionPayload payload) {
        int idx = payload.slotIdx();
        if (idx < 0 || idx > 2) return;
        data.presets().get(idx).copyFrom(data.pool());
        player.sendOverlayMessage(Component.translatable("message.dicefate.preset_saved", idx + 1));
    }

    private static void loadPreset(ServerPlayer player, PlayerFateData data, ClientActionPayload payload, boolean inCombat) {
        if (inCombat) return;
        int idx = payload.slotIdx();
        if (idx < 0 || idx > 2) return;
        data.pool().copyFrom(data.presets().get(idx));
        DFNetwork.sendPool(player, data);
        player.sendOverlayMessage(Component.translatable("message.dicefate.preset_loaded", idx + 1));
    }

    // ==================== 定命 ====================

    private static void lockDie(ServerPlayer player, PlayerFateData data, ClientActionPayload payload, boolean inCombat) {
        // 0.2.0：定命指环（fate_lock_ring）也可开启定命，等效定命符
        boolean canLock = data.isLockUnlocked()
            || com.srqingchen.dicefate.compat.CuriosCompat.hasFateLockRing(player);
        // 0.3.0 验收修复⑥：每场 1 次；定命指环 +1（策划 3.6）
        int limit = com.srqingchen.dicefate.compat.CuriosCompat.hasFateLockRing(player) ? 2 : 1;
        if (inCombat || !canLock || data.lockSlot() >= 0) return;
        if (data.locksUsedThisBattle() >= limit) {
            player.sendOverlayMessage(Component.translatable("message.dicefate.lock_used"));
            return;
        }
        int slot = payload.slotIdx();
        if (slot < 0 || slot >= data.getBaseSlots()) return;
        ItemStack stack = data.pool().baseAt(slot);
        DieState state = stack.get(ModDataComponents.DIE_STATE.get());
        if (state == null || state.lastRoll() <= 0) {
            player.sendOverlayMessage(Component.translatable("message.dicefate.no_last_roll"));
            return;
        }
        if (data.activePack() != null) {
            player.sendOverlayMessage(Component.translatable("message.dicefate.pack_conflict"));
            return;
        }
        data.setLock(slot, state.lastRoll());
        data.addLockUsedThisBattle();
        DFNetwork.sendFateState(player, data, player.level().getGameTime());
    }

    // ==================== 工具 ====================

    /** 0.2.0：战斗中锁定的统一反馈（原静默 return）。 */
    private static void denyInCombat(ServerPlayer player) {
        player.sendOverlayMessage(Component.translatable("message.dicefate.in_combat_lock"));
    }

    private static boolean duplicateCheck(ServerPlayer player, PlayerFateData data, net.minecraft.resources.Identifier dieId, int limit) {
        int count = 0;
        for (ItemStack s : data.pool().equippedBase(DicePool.MAX_BASE_SLOTS)) {
            if (s.getItem() instanceof DiceItem d && d.dieId.equals(dieId)) count++;
        }
        return count < limit;
    }

    private static void consumeFromInventory(ServerPlayer player, ItemStack die) {
        // 客户端拖入的 stack 已在客户端移除；服务端按 (item, uuid) 匹配移除 1 个
        var state = die.get(ModDataComponents.DIE_STATE.get());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() == die.getItem() && s.getCount() == 1) {
                var sState = s.get(ModDataComponents.DIE_STATE.get());
                if (state == null || sState == null || state.uuid().equals(sState.uuid())) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    return;
                }
            }
        }
    }

    private static void giveBack(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private ServerActions() {}
}
