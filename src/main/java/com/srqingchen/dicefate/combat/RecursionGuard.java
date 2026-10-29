package com.srqingchen.dicefate.combat;

/** 拼点递归保护（规划 2.7.3）：结算中产生的伤害不再进入拼点。 */
public final class RecursionGuard {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private RecursionGuard() {}

    public static boolean isResolving() { return DEPTH.get() > 0; }

    /** 0.3.2 诊断：当前递归深度。 */
    public static int depth() { return DEPTH.get(); }

    public static void enter() { DEPTH.set(DEPTH.get() + 1); }

    public static void exit() {
        int d = DEPTH.get() - 1;
        if (d <= 0) DEPTH.remove(); else DEPTH.set(d);
    }
}
