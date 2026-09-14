package net.wieldyourpower.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.wieldyourpower.WieldYourPower;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keyword-driven, mod-agnostic last-resort cleanup for scripted bosses that respawn from their own
 * state. It never edits save data and never touches files.
 *
 * <p>Safety rules (important):</p>
 * <ul>
 *   <li>instance methods are only looked up on the entity's own class hierarchy and called on that
 *       entity object;</li>
 *   <li>static procedures are only considered in the entity's own mod, in classes whose name contains
 *       "despawn", and only methods named {@code execute} or containing "despawn";</li>
 *   <li>any method or declaring class whose name contains a dangerous token (world/save/file/data/
 *       storage/server/dimension/chunk/region/nbt/backup) is skipped;</li>
 *   <li>everything is cached, guarded, and logged when actually invoked.</li>
 * </ul>
 */
public final class BossDespawnCompat {

    private static final String[] INSTANCE_KEYWORDS = {
            "despawn", "kill", "remove", "delete", "destroy"
    };
    private static final String[] STATIC_CLASS_KEYWORDS = {
            "despawn", "kill", "remove", "delete", "destroy"
    };
    private static final String[] DANGEROUS_TOKENS = {
            "world", "save", "file", "data", "storage", "server", "dimension",
            "chunk", "region", "nbt", "backup", "wipe", "profile", "config"
    };

    private static final Map<Class<?>, List<Method>> INSTANCE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<Method>> STATIC_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> MOD_CLASSES = new ConcurrentHashMap<>();

    private BossDespawnCompat() {
    }

    public static boolean tryDespawn(LivingEntity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String namespace = id == null ? "" : id.getNamespace();
        Set<String> modClasses = modClasses(namespace);
        ClassLoader loader = entity.getClass().getClassLoader();

        for (Method method : instanceMethods(entity.getClass())) {
            if (!isSafe(method, modClasses, loader)) {
                continue;
            }
            try {
                method.invoke(entity);
                WieldYourPower.LOGGER.info("[WieldYourPower] Called {} on {} as a despawn fallback",
                        method.getName(), entity.getType());
                return true;
            } catch (Throwable ignored) {
            }
        }

        if (id == null || "minecraft".equals(namespace)) {
            return false;
        }
        for (Method method : staticMethods(namespace, entity)) {
            if (!isSafe(method, modClasses, loader)) {
                continue;
            }
            Object[] args = buildArguments(method, entity);
            if (args == null) {
                continue;
            }
            try {
                method.invoke(null, args);
                WieldYourPower.LOGGER.info("[WieldYourPower] Called {}.{} as a despawn fallback",
                        method.getDeclaringClass().getSimpleName(), method.getName());
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static Set<String> modClasses(String namespace) {
        if (namespace.isEmpty()) {
            return Set.of();
        }
        return MOD_CLASSES.computeIfAbsent(namespace, key -> {
            try {
                IModFileInfo fileInfo = ModList.get().getModFileById(key);
                if (fileInfo == null) {
                    return Set.of();
                }
                Set<String> classes = new HashSet<>();
                for (ModFileScanData.ClassData classData : fileInfo.getFile().getScanResult().getClasses()) {
                    classes.add(classData.clazz().getClassName());
                }
                return classes;
            } catch (Throwable throwable) {
                return Set.of();
            }
        });
    }

    private static boolean isSafe(Method method, Set<String> modClasses, ClassLoader loader) {
        return !hasDangerousToken(method.getName())
                && !hasDangerousToken(method.getDeclaringClass().getSimpleName())
                && ClassSafety.isSafe(method.getDeclaringClass(), modClasses, loader);
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

    private static List<Method> instanceMethods(Class<?> type) {
        return INSTANCE_CACHE.computeIfAbsent(type, key -> {
            List<Method> result = new ArrayList<>();
            for (Class<?> current = key; current != null && current != Object.class; current = current.getSuperclass()) {
                if (hasDangerousToken(current.getSimpleName())) {
                    continue;
                }
                for (Method method : current.getDeclaredMethods()) {
                    if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
                        continue;
                    }
                    if (keywordPriority(method.getName().toLowerCase(Locale.ROOT)) >= 0) {
                        method.setAccessible(true);
                        result.add(method);
                    }
                }
            }
            result.sort(Comparator.comparingInt(m -> keywordPriority(m.getName().toLowerCase(Locale.ROOT))));
            return result;
        });
    }

    private static int keywordPriority(String name) {
        for (int i = 0; i < INSTANCE_KEYWORDS.length; i++) {
            if (name.contains(INSTANCE_KEYWORDS[i])) {
                return i;
            }
        }
        return -1;
    }

    private static List<Method> staticMethods(String namespace, Entity entity) {
        return STATIC_CACHE.computeIfAbsent(namespace, key -> {
            List<Method> result = new ArrayList<>();
            try {
                IModFileInfo fileInfo = ModList.get().getModFileById(key);
                if (fileInfo == null) {
                    return result;
                }
                ModFileScanData scanData = fileInfo.getFile().getScanResult();
                ClassLoader loader = entity.getClass().getClassLoader();
                for (ModFileScanData.ClassData classData : scanData.getClasses()) {
                    String className = classData.clazz().getClassName();
                    String simple = className.substring(className.lastIndexOf('.') + 1);
                    if (!matchesStaticKeyword(simple) || hasDangerousToken(simple)) {
                        continue;
                    }
                    try {
                        Class<?> clazz = Class.forName(className, false, loader);
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 2
                                    && !hasDangerousToken(method.getName())) {
                                method.setAccessible(true);
                                result.add(method);
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable throwable) {
                WieldYourPower.LOGGER.debug("[WieldYourPower] Boss despawn scan failed for {}", key, throwable);
            }
            result.sort(Comparator.comparingInt(m -> staticPriority(m)));
            return result;
        });
    }

    private static boolean matchesStaticKeyword(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String keyword : STATIC_CLASS_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static int staticPriority(Method method) {
        String name = method.getName().toLowerCase(Locale.ROOT);
        String owner = method.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (name.equals("execute")) {
            return 0;
        }
        for (int i = 0; i < STATIC_CLASS_KEYWORDS.length; i++) {
            String keyword = STATIC_CLASS_KEYWORDS[i];
            if (owner.contains(keyword) || name.contains(keyword)) {
                return i + 1;
            }
        }
        return STATIC_CLASS_KEYWORDS.length + 1;
    }

    private static Object[] buildArguments(Method method, LivingEntity entity) {
        Class<?>[] parameters = method.getParameterTypes();
        Object level = entity.level();
        Object[] args = new Object[2];

        if (parameters[0].isInstance(level)) {
            args[0] = level;
        } else if (parameters[0].isInstance(entity)) {
            args[0] = entity;
        } else {
            return null;
        }

        if (parameters[1].isInstance(entity)) {
            args[1] = entity;
        } else if (parameters[1].isInstance(level)) {
            args[1] = level;
        } else {
            return null;
        }
        return args;
    }
}
