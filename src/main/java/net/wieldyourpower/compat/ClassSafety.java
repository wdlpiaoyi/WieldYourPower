package net.wieldyourpower.compat;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bytecode safety scan. Before we ever invoke a method reflectively, we inspect the declaring class's
 * bytecode and reject it if it touches dangerous APIs (file I/O, network, processes, reflection,
 * Unsafe, class loaders, ...) or declares {@code native} methods.
 *
 * <p>It can also scan transitively across the target mod's own classes, so a "despawn" method that
 * delegates to a helper which deletes files is still caught. Static analysis cannot stop a native
 * payload or arbitrary JDK internals, so the whole feature is opt-in and off by default.</p>
 */
final class ClassSafety {

    private static final Map<Class<?>, Direct> DIRECT_CACHE = new ConcurrentHashMap<>();

    private ClassSafety() {
    }

    /**
     * @param modClasses dotted names of all classes in the target mod (used for transitive scanning)
     * @param loader     class loader able to load the mod's classes
     */
    static boolean isSafe(Class<?> root, Set<String> modClasses, ClassLoader loader) {
        Deque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(root.getName());
        while (!queue.isEmpty()) {
            String name = queue.poll();
            if (!visited.add(name)) {
                continue;
            }
            Class<?> clazz = name.equals(root.getName()) ? root : tryLoad(name, loader);
            if (clazz == null) {
                return false;
            }
            Direct direct = DIRECT_CACHE.computeIfAbsent(clazz, ClassSafety::scan);
            if (direct.unsafe || direct.nativeMethod) {
                return false;
            }
            for (String owner : direct.owners) {
                if (modClasses.contains(owner)) {
                    queue.add(owner);
                }
            }
        }
        return true;
    }

    /**
     * Method-level variant: scan only the given method's own bytecode. Used for tiny toggle methods on
     * classes that legitimately use reflection elsewhere (a whole-class scan would reject them).
     */
    static boolean isMethodSafe(Method method) {
        if (method == null) {
            return false;
        }
        Class<?> owner = method.getDeclaringClass();
        try (InputStream stream = owner.getResourceAsStream("/" + owner.getName().replace('.', '/') + ".class")) {
            if (stream == null) {
                return false;
            }
            String targetName = method.getName();
            String targetDesc = Type.getMethodDescriptor(method);
            boolean[] safe = { true };
            ClassReader reader = new ClassReader(stream);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    if (!name.equals(targetName) || !descriptor.equals(targetDesc)) {
                        return null;
                    }
                    if ((access & Opcodes.ACC_NATIVE) != 0) {
                        safe[0] = false;
                        return null;
                    }
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String methodName,
                                                    String methodDescriptor, boolean isInterface) {
                            if (dangerousCall(owner, methodName)) {
                                safe[0] = false;
                            }
                        }

                        @Override
                        public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
                            if (dangerousOwner(owner)) {
                                safe[0] = false;
                            }
                        }

                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            if (dangerousOwner(type)) {
                                safe[0] = false;
                            }
                        }

                        @Override
                        public void visitLdcInsn(Object value) {
                            if (value instanceof String text && looksLikeFilePath(text)) {
                                safe[0] = false;
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return safe[0];
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static Class<?> tryLoad(String dottedName, ClassLoader loader) {
        try {
            return Class.forName(dottedName, false, loader);
        } catch (Throwable throwable) {
            return null;
        }
    }

    private static Direct scan(Class<?> clazz) {
        Direct result = new Direct();
        try (InputStream stream = clazz.getResourceAsStream("/" + clazz.getName().replace('.', '/') + ".class")) {
            if (stream == null) {
                result.unsafe = true;
                return result;
            }
            ClassReader reader = new ClassReader(stream);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    if ((access & Opcodes.ACC_NATIVE) != 0) {
                        result.nativeMethod = true;
                    }
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String methodName,
                                                    String methodDescriptor, boolean isInterface) {
                            result.owners.add(owner.replace('/', '.'));
                            if (dangerousCall(owner, methodName)) {
                                result.unsafe = true;
                            }
                        }

                        @Override
                        public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
                            result.owners.add(owner.replace('/', '.'));
                            if (dangerousOwner(owner)) {
                                result.unsafe = true;
                            }
                        }

                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            result.owners.add(type.replace('/', '.'));
                            if (dangerousOwner(type)) {
                                result.unsafe = true;
                            }
                        }

                        @Override
                        public void visitLdcInsn(Object value) {
                            if (value instanceof String text && looksLikeFilePath(text)) {
                                result.unsafe = true;
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (Throwable throwable) {
            result.unsafe = true;
        }
        return result;
    }

    private static boolean dangerousCall(String owner, String name) {
        if (dangerousOwner(owner)) {
            return true;
        }
        if (owner.equals("java/lang/System")) {
            return name.equals("exit") || name.equals("load") || name.equals("loadLibrary")
                    || name.equals("setSecurityManager") || name.equals("setProperties");
        }
        if (owner.equals("java/lang/Class")) {
            return name.equals("forName");
        }
        if (owner.equals("java/lang/Thread")) {
            return name.equals("start");
        }
        if (owner.startsWith("java/lang/Process")) {
            return true;
        }
        return false;
    }

    private static boolean dangerousOwner(String owner) {
        if (owner == null) {
            return true;
        }
        return owner.startsWith("java/io/")
                || owner.startsWith("java/nio/")
                || owner.startsWith("java/net/")
                || owner.startsWith("java/util/zip/")
                || owner.startsWith("java/security/")
                || owner.startsWith("java/lang/reflect/")
                || owner.startsWith("java/lang/invoke/")
                || owner.startsWith("sun/")
                || owner.startsWith("jdk/")
                || owner.startsWith("org/apache/commons/io/")
                || owner.equals("java/lang/Runtime")
                || owner.equals("java/lang/Process")
                || owner.equals("java/lang/ProcessBuilder")
                || owner.equals("java/lang/ClassLoader");
    }

    private static boolean looksLikeFilePath(String value) {
        String text = value.replace('\\', '/').toLowerCase(Locale.ROOT);
        return text.startsWith("/")
                || text.contains(":/")
                || text.contains("../")
                || text.contains(".minecraft")
                || text.contains("appdata")
                || text.contains(".config")
                || text.contains(".jar")
                || text.contains(".dat")
                || text.contains(".log");
    }

    private static final class Direct {
        private boolean unsafe;
        private boolean nativeMethod;
        private final Set<String> owners = new HashSet<>();
    }
}
