package cn.ibax.loaderbridge.bridge.internal;

import cn.ibax.loaderbridge.bridge.BridgeResult;
import cn.ibax.loaderbridge.bridge.PluginHandle;

import java.util.Optional;

final class BukkitPluginHandle extends AbstractClassLoaderHandle implements PluginHandle {
    private final String pluginName;
    private final String mainClassName;

    BukkitPluginHandle(final Object plugin) {
        super(plugin.getClass().getClassLoader(), plugin);
        this.pluginName = resolvePluginName(plugin);
        this.mainClassName = plugin.getClass().getName();
    }

    @Override
    public String name() {
        return pluginName;
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

    private String resolvePluginName(final Object plugin) {
        try {
            Object name = ReflectionSupport.invokeNoArg(plugin, "getName").orElse(null);
            if (name != null) {
                return String.valueOf(name);
            }
        } catch (RuntimeException ignored) {
            return plugin.getClass().getSimpleName();
        }
        return plugin.getClass().getSimpleName();
    }
}
