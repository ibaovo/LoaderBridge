package cn.ibax.loaderbridge.testharness;

import cn.ibax.loaderbridge.bridge.BridgeResult;
import cn.ibax.loaderbridge.bridge.ModHandle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 测试用的模组句柄。
 */
public final class FakeModHandle implements ModHandle {
    private final String modId;
    private final Object instance;

    public FakeModHandle(final String modId, final Object instance) {
        this.modId = modId;
        this.instance = instance;
    }

    @Override
    public String modId() {
        return modId;
    }

    @Override
    public String mainClassName() {
        return instance.getClass().getName();
    }

    @Override
    public ClassLoader classLoader() {
        return instance.getClass().getClassLoader();
    }

    @Override
    public Optional<Object> instance() {
        return Optional.of(instance);
    }

    @Override
    public Optional<Class<?>> findClass(final String className) {
        try {
            return Optional.of(Class.forName(className, false, classLoader()));
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public BridgeResult<Object> invokeStatic(final String className, final String methodName, final Class<?>[] parameterTypes, final Object... args) {
        return invoke(className, null, methodName, parameterTypes, args);
    }

    @Override
    public BridgeResult<Object> invokeInstance(final Object target, final String methodName, final Class<?>[] parameterTypes, final Object... args) {
        if (target == null) {
            return BridgeResult.failure("目标实例不能为空");
        }
        return invoke(target.getClass().getName(), target, methodName, parameterTypes, args);
    }

    @Override
    public BridgeResult<Object> newInstance(final String className, final Class<?>[] parameterTypes, final Object... args) {
        try {
            Class<?> type = Class.forName(className, false, classLoader());
            Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes == null ? new Class<?>[0] : parameterTypes);
            constructor.setAccessible(true);
            return BridgeResult.success(constructor.newInstance(args), "已创建实例");
        } catch (ReflectiveOperationException exception) {
            return BridgeResult.failure("创建实例失败：" + className, exception);
        }
    }

    private BridgeResult<Object> invoke(
            final String className,
            final Object target,
            final String methodName,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        try {
            Class<?> type = Class.forName(className, false, classLoader());
            Method method = type.getDeclaredMethod(methodName, parameterTypes == null ? new Class<?>[0] : parameterTypes);
            method.setAccessible(true);
            return BridgeResult.success(method.invoke(target, args), "已调用方法");
        } catch (ReflectiveOperationException exception) {
            return BridgeResult.failure("调用方法失败：" + className + '#' + methodName, exception);
        }
    }
}
