package com.srqingchen.dicefate.combat;

import com.srqingchen.dicefate.data.PoolProfile;
import com.srqingchen.dicefate.dice.DieDefinition;
import com.srqingchen.dicefate.dice.DieElement;
import com.srqingchen.dicefate.dice.DieState;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼点开始时的一方骰池只读快照（规划 2.2）。
 * 玩家侧由 PlayerFateData+骰包构建；怪物侧由 PoolProfile 构建。
 */
public final class PoolSnapshot {
    /** 基础战斗槽（按骰序）。 */
    public final List<SlotView> slots = new ArrayList<>();
    /** 加算槽。 */
    public final List<SlotView> addSlots = new ArrayList<>();
    /** 乘算槽。 */
    public final List<SlotView> mulSlots = new ArrayList<>();
    /** 技能骰（族+阶）。 */
    public final List<SkillView> skills = new ArrayList<>();

    public PoolProfile.Stance stance = PoolProfile.Stance.BALANCED;
    public int mastery;
    /** 命气散竭：全部骰掷 0。 */
    public boolean exhausted;
    /** 命运包取高概率（0.3.0：DiceRoller 消费——每槽按此概率双掷取高）。 */
    public double coRollChance;

    /** 单槽视图：定义 + 动态状态 + 临时效果标记。 */
    public static final class SlotView {
        public final int slotIndex;
        public final DieDefinition def;
        public final DieState state;        // 可能为 null（怪物侧无组件）
        public boolean frozen;              // 被冰封 → 0
        public int forcedValue = -1;        // 定命/兑现/归零等强制值

        public SlotView(int slotIndex, DieDefinition def, DieState state) {
            this.slotIndex = slotIndex;
            this.def = def;
            this.state = state;
        }

        public DieElement element() { return def == null ? DieElement.NONE : def.element(); }
        public int face() { return def == null ? 6 : def.face(); }
        /** 磨损裂面：点数 -1（最低 1）。 */
        public boolean cracked() {
            return state != null && def != null && state.wear() >= def.material().baseWear();
        }
    }

    /** 技能骰视图（0.2.0：带实际骰面）。 */
    public record SkillView(Identifier dieId, com.srqingchen.dicefate.dice.SkillFamily family, int tier, int face) {}
}
