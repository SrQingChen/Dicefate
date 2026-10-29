package com.srqingchen.dicefate.util;

import com.srqingchen.dicefate.DiceFate;

/**
 * 0.3.2 全管线诊断日志（[DF诊断] 前缀）。
 * 由 config misc.debugVerbose 控制（默认 true——生产环境"拼点完全无效"根因定位期间；
 * 根因确认后默认改 false）。所有调用点为一次性/低频事件，热路径（每 tick）不接入。
 */
public final class DFDebug {

    private static Boolean enabled;

    public static boolean enabled() {
        if (enabled == null) {
            try {
                enabled = com.srqingchen.dicefate.DFConfig.DEBUG_VERBOSE.get();
            } catch (IllegalStateException e) {
                enabled = true;   // config 未加载（客户端早期/纯 JVM 测试）时保守开启
            }
        }
        return enabled;
    }

    public static void log(String fmt, Object... args) {
        if (enabled()) {
            DiceFate.LOGGER.info("[DF诊断] " + safe(fmt), safeArgs(args));
        }
    }

    /** 无条件输出（网络收发等关键证据点）。 */
    public static void always(String fmt, Object... args) {
        DiceFate.LOGGER.info("[DF诊断] " + safe(fmt), safeArgs(args));
    }

    public static void err(String fmt, Object... args) {
        DiceFate.LOGGER.error("[DF诊断] " + safe(fmt), safeArgs(args));
    }

    private static String safe(String fmt) {
        return fmt == null ? "null" : fmt;
    }

    private static Object[] safeArgs(Object... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) args[i] = "null";
        }
        return args;
    }

    private DFDebug() {}
}
