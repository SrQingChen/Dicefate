package com.srqingchen.dicefate.dice;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 命盘：玩家装备的骰子集合。
 * 基础 1..6 位；专属槽：加算 1、乘算 1、技能 2。均存 ItemStack（空 = 未装备）。
 */
public class DicePool {
    public static final int MAX_BASE_SLOTS = 6;

    private final List<ItemStack> base = new ArrayList<>(MAX_BASE_SLOTS);
    private ItemStack addDie = ItemStack.EMPTY;
    private ItemStack mulDie = ItemStack.EMPTY;
    private ItemStack skill0 = ItemStack.EMPTY;
    private ItemStack skill1 = ItemStack.EMPTY;

    public DicePool() {
        for (int i = 0; i < MAX_BASE_SLOTS; i++) base.add(ItemStack.EMPTY);
    }

    public List<ItemStack> base() { return base; }

    public ItemStack baseAt(int idx) { return base.get(idx); }

    public void setBase(int idx, ItemStack stack) { base.set(idx, stack == null ? ItemStack.EMPTY : stack); }

    public ItemStack addDie() { return addDie; }
    public ItemStack mulDie() { return mulDie; }
    public ItemStack skill0() { return skill0; }
    public ItemStack skill1() { return skill1; }
    public void setAddDie(ItemStack s) { addDie = orEmpty(s); }
    public void setMulDie(ItemStack s) { mulDie = orEmpty(s); }
    public void setSkill0(ItemStack s) { skill0 = orEmpty(s); }
    public void setSkill1(ItemStack s) { skill1 = orEmpty(s); }

    /** 技能槽 0/1。 */
    public ItemStack skillAt(int idx) { return idx == 0 ? skill0 : skill1; }
    public void setSkillAt(int idx, ItemStack s) { if (idx == 0) setSkill0(s); else setSkill1(s); }

    /** 前 baseSlots 个基础位中的已装备骰（快照/协同遍历用）。 */
    public List<ItemStack> equippedBase(int baseSlots) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < Math.min(baseSlots, base.size()); i++) {
            ItemStack s = base.get(i);
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    public List<ItemStack> skills() {
        List<ItemStack> out = new ArrayList<>(2);
        if (!skill0.isEmpty()) out.add(skill0);
        if (!skill1.isEmpty()) out.add(skill1);
        return out;
    }

    public boolean isEmpty() {
        return equippedBase(MAX_BASE_SLOTS).isEmpty() && addDie.isEmpty() && mulDie.isEmpty()
            && skill0.isEmpty() && skill1.isEmpty();
    }

    /** 全部非空骰（磨损/耐久处理等）。 */
    public List<ItemStack> allDice() {
        List<ItemStack> out = new ArrayList<>(10);
        out.addAll(equippedBase(MAX_BASE_SLOTS));
        if (!addDie.isEmpty()) out.add(addDie);
        if (!mulDie.isEmpty()) out.add(mulDie);
        out.addAll(skills());
        return out;
    }

    public void copyFrom(DicePool other) {
        for (int i = 0; i < MAX_BASE_SLOTS; i++) base.set(i, other.base.get(i).copy());
        addDie = other.addDie.copy();
        mulDie = other.mulDie.copy();
        skill0 = other.skill0.copy();
        skill1 = other.skill1.copy();
    }

    private static ItemStack orEmpty(ItemStack s) { return s == null ? ItemStack.EMPTY : s; }
}
