package com.srqingchen.dicefate.combat;

import java.util.List;

/**
 * 牌型检测（规划 4.9）：连续 3 基础骰位窗口；全池升格天命。
 * 0.2.0：优先级修复——全池 > 豹子 > 顺子（原实现顺子 break 压过豹子）；
 * 窗口与全池升格两处均排除 value<=0 的碎裂/冻结骰。
 */
public final class PatternService {
    private PatternService() {}

    public static PatternType detect(List<DieRollView> baseRolls) {
        if (baseRolls.size() < 3) return PatternType.NONE;
        PatternType window = PatternType.NONE;
        for (int i = 0; i + 2 < baseRolls.size(); i++) {
            int a = baseRolls.get(i).value;
            int b = baseRolls.get(i + 1).value;
            int c = baseRolls.get(i + 2).value;
            if (a <= 0 || b <= 0 || c <= 0) continue;   // 碎裂骰不构成牌型
            boolean straight = b == a + 1 && c == b + 1;
            boolean triple = a == b && b == c;
            if (triple) {
                window = PatternType.TRIPLE;            // 豹子 > 顺子：命中后继续找更优窗口
            } else if (straight && window != PatternType.TRIPLE) {
                window = PatternType.STRAIGHT;
            }
        }
        if (window == PatternType.NONE) return PatternType.NONE;

        // 全池升格（n≥4 所有基础骰构成顺子/豹子；碎骰即失败）
        if (baseRolls.size() >= 4) {
            boolean allStraight = true, allTriple = true;
            int first = baseRolls.get(0).value;
            if (first <= 0) return window;
            for (int i = 0; i < baseRolls.size(); i++) {
                int v = baseRolls.get(i).value;
                if (v <= 0) { allStraight = false; allTriple = false; break; }
                if (v != first + i) allStraight = false;
                if (v != first) allTriple = false;
            }
            if (allTriple && first > 0) return PatternType.DESTINY_TRIPLE;
            if (allStraight) return PatternType.DESTINY_STRAIGHT;
        }
        return window;
    }
}
