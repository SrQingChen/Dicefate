package com.srqingchen.dicefate.client.hud;

import com.srqingchen.dicefate.network.CombatTextPayload;

/** 飘字文案与配色。 */
final class CombatTexts {
    private CombatTexts() {}

    static String format(CombatTextPayload p) {
        CombatTextPayload.TextType t = CombatTextPayload.TextType.values()[p.kind() & 0xFF];
        return switch (t) {
            case DAMAGE -> String.valueOf(p.value());
            case MISS -> "Miss!";
            case POISE -> "命势-" + p.value();
            case POISE_BREAK -> "破势！";
            case FATEBREAK -> "命破 " + p.value();
            case WOUND -> "命损";
            case EXHAUSTED -> "命气散竭";
            case PATTERN -> "牌型！";
            case REACTION -> "元素反应";
            case SKILL -> "技能骰";
            case MOMENTUM -> "骰势";
            case CASHOUT -> "兑现！";
            case LOCK -> "定命";
            case SPIRIT -> "骰灵";
            case PITY -> "命佑";
            case SHIELD -> "完美格挡！";
        };
    }

    static int color(CombatTextPayload p) {
        return switch (p.color()) {
            case 1 -> 0xFFFFE9B0;
            case 2 -> 0xFF7FE3B2;
            case 3 -> 0xFFFFD966;
            case 4 -> 0xFFFF7A7A;
            case 5 -> 0xFFB8B8B8;
            case 6 -> 0xFFC77AD4;
            case 7 -> 0xFF6FB3D4;
            default -> 0xFFF0F0F0;
        };
    }
}
