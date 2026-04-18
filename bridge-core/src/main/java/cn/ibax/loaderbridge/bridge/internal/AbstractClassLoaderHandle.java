package cn.ibax.loaderbridge.bridge.internal;

import cn.ibax.loaderbridge.bridge.BridgeResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

abstract class AbstractClassLoaderHandle {
    protected final ClassLoader classLoader;
    protected final Object anchor;

    protected AbstractClassLoaderHandle(final ClassLoader classLoader, final Object anchor) {
        this.classLoader = Objects.requireNonNull(classLoader, "类加载器不能为空");
        this.anchor = Objects.requireNonNull(anchor, "锚点对象不能为空");
    }

    protected Optional<Class<?>> findClassInternal(final String className) {
        try {
            return Optional.of(Class.forName(className, false, classLoader));
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        }
    }

    protected BridgeResult<Object> invokeStaticInternal(
            final String className,
            final String methodName,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        return findClassInternal(className)
                .map(type -> invokeOnType(type, null, methodName, parameterTypes, args))
                .orElseGet(() -> BridgeResult.failure("未找到指定类：" + className));
    }

    protected BridgeResult<Object> invokeInstanceInternal(
            final Object target,
            final String methodName,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        if (target == null) {
            return BridgeResult.failure("目标实例不能为空");
        }
        return invokeOnType(target.getClass(), target, methodName, parameterTypes, args);
    }

    protected BridgeResult<Object> newInstanceInternal(
            final String className,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        return findClassInternal(className)
                .map(type -> {
                    try {
                        Constructor<?> constructor = type.getDeclaredConstructor(normalizeTypes(parameterTypes, args));
                        constructor.setAccessible(true);
                        return BridgeResult.success(constructor.newInstance(args), "已创建实例");
                    } catch (ReflectiveOperationException exception) {
                        return BridgeResult.failure("创建实例失败：" + className, exception);
                    }
                })
                .orElseGet(() -> BridgeResult.failure("未找到指定类：" + className));
    }

    private BridgeResult<Object> invokeOnType(
            final Class<?> type,
            final Object target,
            final String methodName,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        try {
            Method method = type.getDeclaredMethod(methodName, normalizeTypes(parameterTypes, args));
            method.setAccessible(true);
            if (Modifier.isStatic(method.getModifiers()) || target != null) {
                return BridgeResult.success(method.invoke(target, args), "已调用方法");
            }
            return BridgeResult.failure("目标方法不是静态方法，且未提供实例：" + methodName);
        } catch (ReflectiveOperationException exception) {
            return BridgeResult.failure("调用方法失败：" + type.getName() + '#' + methodName, exception);
        }
    }

    private Class<?>[] normalizeTypes(final Class<?>[] parameterTypes, final Object[] args) {
        if (parameterTypes != null) {
            return parameterTypes;
        }
        if (args == null || args.length == 0) {
            return new Class<?>[0];
        }
        return Arrays.stream(args)
                .map(argument -> argument == null ? Object.class : argument.getClass())
                .toArray(Class<?>[]::new);
    }
}
