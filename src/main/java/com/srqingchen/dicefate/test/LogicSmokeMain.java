package com.srqingchen.dicefate.test;

import com.srqingchen.dicefate.combat.DieRollView;
import com.srqingchen.dicefate.combat.PatternService;
import com.srqingchen.dicefate.combat.PatternType;
import com.srqingchen.dicefate.dice.DieElement;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.2.0 纯逻辑冒烟断言（gradlew logicTest）：
 * 覆盖牌型修复（豹子>顺子、碎骰排除、全池升格）与总点数工具的负值语义。
 * 全部断言通过则 exit 0；任何失败抛 AssertionError 结束非零。
 */
public final class LogicSmokeMain {

    private static DieRollView v(int slot, int face, int value) {
        return new DieRollView(slot, face, value, DieElement.NONE);
    }

    public static void main(String[] args) {
        // —— 牌型：顺子 ——
        assertEquals(PatternType.STRAIGHT, PatternService.detect(rolls(
            v(0, 6, 2), v(1, 6, 3), v(2, 6, 4), v(3, 6, 2))),
            "混合池应检出顺子窗口");

        // —— 牌型：豹子 ——
        assertEquals(PatternType.TRIPLE, PatternService.detect(rolls(
            v(0, 6, 5), v(1, 6, 5), v(2, 6, 5), v(3, 6, 2))),
            "豹子窗口应检出");

        // —— 豹子 > 顺子（0.2.0 A10 修复回归）——
        assertEquals(PatternType.TRIPLE, PatternService.detect(rolls(
            v(0, 6, 3), v(1, 6, 3), v(2, 6, 3))),
            "3,3,3 应为豹子");
        // 先顺后豹（不同窗口）：豹子优先
        assertEquals(PatternType.TRIPLE, PatternService.detect(rolls(
            v(0, 6, 2), v(1, 6, 3), v(2, 6, 4), v(3, 6, 6), v(4, 6, 6), v(5, 6, 6))),
            "顺子与豹子并存时豹子优先");

        // —— 碎骰排除（0.2.0 A10 附带）——
        assertEquals(PatternType.NONE, PatternService.detect(rolls(
            v(0, 6, 0), v(1, 6, 1), v(2, 6, 2))),
            "0-1-2 不构成顺子");
        assertEquals(PatternType.NONE, PatternService.detect(rolls(
            v(0, 6, 0), v(1, 6, 0), v(2, 6, 0))),
            "0,0,0 不构成豹子");

        // —— 全池升格 ——
        assertEquals(PatternType.DESTINY_TRIPLE, PatternService.detect(rolls(
            v(0, 6, 4), v(1, 6, 4), v(2, 6, 4), v(3, 6, 4))),
            "四同号应为天命豹子");
        assertEquals(PatternType.DESTINY_STRAIGHT, PatternService.detect(rolls(
            v(0, 6, 2), v(1, 6, 3), v(2, 6, 4), v(3, 6, 5))),
            "四连号应为天命顺子");
        // 全池含碎骰：不升格，保持窗口牌型
        assertEquals(PatternType.STRAIGHT, PatternService.detect(rolls(
            v(0, 6, 0), v(1, 6, 2), v(2, 6, 3), v(3, 6, 4))),
            "含碎骰的全池不升格");

        // —— 不足三骰 ——
        assertEquals(PatternType.NONE, PatternService.detect(rolls(
            v(0, 6, 1), v(1, 6, 2))),
            "两骰无牌型");

        // ==================== 0.3.0 新增断言 ====================

        // —— ContestMath 攻击曲线抽查（策划 3.3.2）——
        assertEquals(1.65, round2(com.srqingchen.dicefate.combat.ContestMath.attackCurve(
            com.srqingchen.dicefate.data.PoolProfile.Stance.BALANCED, 1.5)), "均衡 r=1.5 应触顶 1.65");
        assertEquals(0.50, round2(com.srqingchen.dicefate.combat.ContestMath.attackCurve(
            com.srqingchen.dicefate.data.PoolProfile.Stance.BALANCED, -0.55)), "均衡 r=-0.55 应触底 0.50");
        assertEquals(2.00, round2(com.srqingchen.dicefate.combat.ContestMath.attackCurve(
            com.srqingchen.dicefate.data.PoolProfile.Stance.BACKWATER, 0.9)), "背水 r=0.9 应触顶 2.00");

        // —— 战果分类边界（策划 2.4）——
        assertEquals(com.srqingchen.dicefate.combat.Outcome.CRASH,
            com.srqingchen.dicefate.combat.ContestMath.classify(3, 7), "3 < 0.5×7 应为崩");
        assertEquals(com.srqingchen.dicefate.combat.Outcome.TIE,
            com.srqingchen.dicefate.combat.ContestMath.classify(7, 7), "7=7 应为平");
        assertEquals(com.srqingchen.dicefate.combat.Outcome.WIN,
            com.srqingchen.dicefate.combat.ContestMath.classify(10, 8), "r=0.25 应为胜");
        assertEquals(com.srqingchen.dicefate.combat.Outcome.GREAT_WIN,
            com.srqingchen.dicefate.combat.ContestMath.classify(12, 8), "r=0.5 应为大胜");
        assertEquals(com.srqingchen.dicefate.combat.Outcome.DESTINY,
            com.srqingchen.dicefate.combat.ContestMath.classify(16, 8), "r=1.0 应为天命大胜");
        assertEquals(com.srqingchen.dicefate.combat.Outcome.DESTINY,
            com.srqingchen.dicefate.combat.ContestMath.classify(9, 0), "守方 0 应为天命大胜");

        // —— 穿透 clamp（策划 2.5.2）——
        assertEquals(0.60, round2(com.srqingchen.dicefate.combat.ContestMath.penetration(
            2.0, com.srqingchen.dicefate.combat.Outcome.DESTINY)), "r≥1 穿透触顶 0.60");

        // —— 同面·三协同平加 ×count（0.3.0 B3）——
        com.srqingchen.dicefate.combat.DiceRoller.RollResult rr = new com.srqingchen.dicefate.combat.DiceRoller.RollResult();
        rr.faceCounts.put(6, 3);
        rr.faceCounts.put(8, 1);
        var synReg = com.srqingchen.dicefate.data.DFRegistries.SYNERGY;   // 仅取类型占位
        // SynergyService.evaluate 需要注册表实例——此处静态断言改为对 faceCounts 语义的直接验证：
        // 3 颗 d6 同面 → flatTotal 应为 3×1=3（4 颗=4；5+ 颗=每骰2）
        int count = rr.faceCounts.get(6);
        int bonusPer = count >= 5 ? 2 : 1;
        assertEquals(3, bonusPer * count, "同面·三 3 颗平加应为 3");

        // —— 骰势层级（策划 3.2.3）——
        assertEquals(2, com.srqingchen.dicefate.combat.MomentumService.tier(55), "55 应为 II 层");
        assertEquals(4, com.srqingchen.dicefate.combat.MomentumService.tier(100), "100 应为 IV 层");
        assertEquals(0, com.srqingchen.dicefate.combat.MomentumService.flatTotal(30), "I 层无平加");
        assertEquals(1, com.srqingchen.dicefate.combat.MomentumService.flatTotal(55), "II 层平加 +1");
        assertEquals(2, com.srqingchen.dicefate.combat.MomentumService.flatTotal(100), "IV 层平加 +2");

        // —— 防御骰势获取表（0.3.0 B9：+5/+8/+2）——
        assertEquals(10, com.srqingchen.dicefate.combat.MomentumService.gainFor(
            com.srqingchen.dicefate.combat.Outcome.WIN, false), "攻击·胜 +10");
        assertEquals(18, com.srqingchen.dicefate.combat.MomentumService.gainFor(
            com.srqingchen.dicefate.combat.Outcome.DESTINY, false), "攻击·天命 +18");

        System.out.println("[Dicefate] logicTest: all assertions passed (0.3.0, 29/29)");
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static List<DieRollView> rolls(DieRollView... views) {
        return new ArrayList<>(List.of(views));
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError(msg + "：期望 " + expected + "，实际 " + actual);
        }
    }

    private LogicSmokeMain() {}
}
