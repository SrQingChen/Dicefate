package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.dice.DieElement;
import com.srqingchen.dicefate.dice.DieRune;

import javax.annotation.Nullable;

/**
 * 单颗骰子的掷骰结果视图（HUD 与结算共用）。
 * value=0 表示碎裂/冻结/归零。
 * 0.2.0：triggeredRune 记录本骰触发的骰纹（HUD 触发展示）。
 */
public final class DieRollView {
    public final int slotIndex;
    public final int face;
    public int value;                // 展示值（可为 0/负）
    public final DieElement element;
    public boolean crashed;         // 碎裂（散竭/冻结/归零）
    public boolean pityRolled;      // 保底取高标记
    public boolean nudged;          // 骰灵修正标记
    public boolean lockedUsed;      // 定命值使用标记
    @Nullable
    public DieRune triggeredRune;   // 触发的骰纹

    public DieRollView(int slotIndex, int face, int value, DieElement element) {
        this.slotIndex = slotIndex;
        this.face = face;
        this.value = value;
        this.element = element;
    }
}
