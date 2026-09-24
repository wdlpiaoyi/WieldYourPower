package net.wieldyourpower.compat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.wieldyourpower.WYPConfig;
import net.wieldyourpower.WieldYourPower;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

/**
 * Keyword-driven block-protection bypass.
 *
 * <p>Some protection mods ship a static "bypass" switch (usually a {@code ThreadLocal}) that their own
 * code checks before allowing a protected block to change. We do not name any mod or class: we scan
 * every loaded mod's classes for one whose name looks like a protection/guard class and which exposes a
 * static {@code *bypass*(boolean)} method, verify that method's bytecode via
 * {@link ClassSafety#isMethodSafe}, then toggle it around a forced removal.</p>
 *
 * <p>When no candidate is present the calls are a no-op and forced removal falls back to a plain
 * server-side block change. Resolution is cached after the first attempt.</p>
 */
public final class BlockProtectionBypass {

    private static final String[] CLASS_KEYWORDS = { "protect", "guard", "bypass" };
    private static final String[] DANGEROUS_TOKENS = {
            "world", "save", "file", "data", "storage", "server", "dimension",
            "chunk", "region", "nbt", "backup", "wipe", "profile", "config"
    };

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private static boolean resolved;
    private static Method setter;

    private BlockProtectionBypass() {
    }

    /**
     * @return {@code true} when a bypass was actually turned on, to be passed back to {@link #exit}.
     */
    public static boolean enter() {
        if (!WYPConfig.COMMON.blockProtectionBypass.get() || !resolve()) {
            return false;
        }
        if (DEPTH.get() == 0) {
            invoke(true);
        }
        DEPTH.set(DEPTH.get() + 1);
        return true;
    }

    public static void exit(boolean active) {
        if (!active) {
            return;
        }
        int depth = DEPTH.get() - 1;
        if (depth <= 0) {
            DEPTH.remove();
            invoke(false);
        } else {
            DEPTH.set(depth);
        }
    }

    private static synchronized boolean resolve() {
        if (resolved) {
            return setter != null;
        }
        resolved = true;
        try {
            // Forge loads every mod through one transforming class loader, so this mod's own loader can
            // resolve any other mod's classes.
            ClassLoader loader = BlockProtectionBypass.class.getClassLoader();
            for (IModFileInfo fileInfo : ModList.get().getModFiles()) {
                for (ModFileScanData.ClassData classData : fileInfo.getFile().getScanResult().getClasses()) {
                    String className = classData.clazz().getClassName();
                    String simple = className.substring(className.lastIndexOf('.') + 1);
                    if (hasDangerousToken(simple) || !matchesClass(simple)) {
                        continue;
                    }
                    Method candidate = findBypass(className, loader);
                    if (candidate != null) {
                        setter = candidate;
                        WieldYourPower.LOGGER.info("[WieldYourPower] Block-protection bypass resolved: {}#{}",
                                candidate.getDeclaringClass().getName(), candidate.getName());
                        return true;
                    }
                }
            }
        } catch (Throwable throwable) {
            WieldYourPower.LOGGER.debug("[WieldYourPower] block-protection bypass scan failed", throwable);
        }
        WieldYourPower.LOGGER.debug("[WieldYourPower] no block-protection bypass switch found");
        return setter != null;
    }

    private static Method findBypass(String className, ClassLoader loader) {
        try {
            Class<?> clazz = Class.forName(className, false, loader);
            for (Method method : clazz.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != boolean.class) {
                    continue;
                }
                if (!method.getName().toLowerCase(Locale.ROOT).contains("bypass")) {
                    continue;
                }
                if (!ClassSafety.isMethodSafe(method)) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void invoke(boolean value) {
        Method method = setter;
        if (method == null) {
            return;
        }
        try {
            method.invoke(null, value);
        } catch (Throwable throwable) {
            setter = null;
            WieldYourPower.LOGGER.debug("[WieldYourPower] block-protection bypass invocation failed", throwable);
        }
    }

    private static boolean matchesClass(String simple) {
        String lower = simple.toLowerCase(Locale.ROOT);
        for (String keyword : CLASS_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDangerousToken(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String token : DANGEROUS_TOKENS) {
            if (lower.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
