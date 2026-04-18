package cn.ibax.loaderbridge.bridge.internal;

import java.lang.reflect.Method;
import java.util.Optional;

public final class ReflectionSupport {
    private ReflectionSupport() {
    }

    public static Optional<Class<?>> tryLoad(final String className, final ClassLoader classLoader) {
        try {
            return Optional.of(Class.forName(className, false, classLoader));
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        }
    }

    public static Method findStaticMethod(final Class<?> type, final String methodName, final Class<?>... parameterTypes) {
        try {
            Method method = type.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    public static Optional<Object> invokeNoArg(final Object target, final String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return Optional.ofNullable(method.invoke(target));
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }
}
