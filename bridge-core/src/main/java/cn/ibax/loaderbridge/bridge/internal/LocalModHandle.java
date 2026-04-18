package cn.ibax.loaderbridge.bridge.internal;

import cn.ibax.loaderbridge.bridge.BridgeResult;
import cn.ibax.loaderbridge.bridge.ModHandle;

import java.util.Optional;
import java.util.Objects;

public final class LocalModHandle extends AbstractClassLoaderHandle implements ModHandle {
    private final String modId;
    private final String mainClassName;

    public LocalModHandle(final String modId, final Object modAnchor) {
        super(Objects.requireNonNullElse(modAnchor.getClass().getClassLoader(), LocalModHandle.class.getClassLoader()), modAnchor);
        this.modId = modId;
        this.mainClassName = modAnchor.getClass().getName();
    }

    @Override
    public String modId() {
        return modId;
    }

    @Override
    public String mainClassName() {
        return mainClassName;
    }

    @Override
    public ClassLoader classLoader() {
        return classLoader;
    }

    @Override
    public Optional<Object> instance() {
        return Optional.of(anchor);
    }

    @Override
    public Optional<Class<?>> findClass(final String className) {
        return findClassInternal(className);
    }

    @Override
    public BridgeResult<Object> invokeStatic(final String className, final String methodName, final Class<?>[] parameterTypes, final Object... args) {
        return invokeStaticInternal(className, methodName, parameterTypes, args);
    }

    @Override
    public BridgeResult<Object> invokeInstance(final Object target, final String methodName, final Class<?>[] parameterTypes, final Object... args) {
        return invokeInstanceInternal(target, methodName, parameterTypes, args);
    }

    @Override
    public BridgeResult<Object> newInstance(final String className, final Class<?>[] parameterTypes, final Object... args) {
        return newInstanceInternal(className, parameterTypes, args);
    }
}
