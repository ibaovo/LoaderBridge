package cn.ibax.loaderbridge.bridge;

import cn.ibax.loaderbridge.bridge.internal.UnavailableBridgeEnvironment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 桥接层的全局访问入口。
 */
public final class BridgeManager {
    private static final AtomicReference<BridgeEnvironment> ENVIRONMENT = new AtomicReference<>(UnavailableBridgeEnvironment.uninitialized());

    private BridgeManager() {
    }

    public static void install(final BridgeEnvironment environment) {
        ENVIRONMENT.set(Objects.requireNonNull(environment, "桥接环境不能为空"));
    }

    public static BridgeEnvironment environment() {
        return ENVIRONMENT.get();
    }

    public static boolean available() {
        return environment().available();
    }

    public static String runtimeName() {
        return environment().runtimeName();
    }

    public static Set<BridgeCapability> capabilities() {
        return environment().capabilities();
    }

    public static Optional<ModHandle> currentMod() {
        return environment().currentMod();
    }

    public static List<PluginHandle> plugins() {
        return environment().plugins();
    }

    public static Optional<PluginHandle> findPlugin(final String pluginName) {
        return environment().findPlugin(pluginName);
    }

    public static Optional<Object> findService(final Class<?> serviceType) {
        return environment().findService(serviceType);
    }

    public static Optional<Class<?>> findCurrentModClass(final String className) {
        return currentMod().flatMap(modHandle -> modHandle.findClass(className));
    }

    public static BridgeResult<Object> invokeCurrentModStatic(
            final String className,
            final String methodName,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        return currentMod()
                .map(modHandle -> modHandle.invokeStatic(className, methodName, parameterTypes, args))
                .orElseGet(() -> BridgeResult.failure("当前模组句柄不可用"));
    }

    public static BridgeResult<Object> invokeCurrentModInstance(
            final String methodName,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        Optional<ModHandle> modHandle = currentMod();
        if (modHandle.isEmpty()) {
            return BridgeResult.failure("当前模组句柄不可用");
        }

        return modHandle.get().instance()
                .map(instance -> modHandle.get().invokeInstance(instance, methodName, parameterTypes, args))
                .orElseGet(() -> BridgeResult.failure("当前模组实例不可用"));
    }

    public static Optional<Class<?>> findPluginClass(final String pluginName, final String className) {
        return findPlugin(pluginName).flatMap(pluginHandle -> pluginHandle.findClass(className));
    }

    public static BridgeResult<Object> invokePluginStatic(
            final String pluginName,
            final String className,
            final String methodName,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        return findPlugin(pluginName)
                .map(pluginHandle -> pluginHandle.invokeStatic(className, methodName, parameterTypes, args))
                .orElseGet(() -> BridgeResult.failure("未找到指定插件：" + pluginName));
    }

    public static BridgeResult<Object> invokePluginInstance(
            final String pluginName,
            final String methodName,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        Optional<PluginHandle> pluginHandle = findPlugin(pluginName);
        if (pluginHandle.isEmpty()) {
            return BridgeResult.failure("未找到指定插件：" + pluginName);
        }

        return pluginHandle.get().instance()
                .map(instance -> pluginHandle.get().invokeInstance(instance, methodName, parameterTypes, args))
                .orElseGet(() -> BridgeResult.failure("指定插件实例不可用：" + pluginName));
    }

    public static BridgeResult<Object> createPluginInstance(
            final String pluginName,
            final String className,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        return findPlugin(pluginName)
                .map(pluginHandle -> pluginHandle.newInstance(className, parameterTypes, args))
                .orElseGet(() -> BridgeResult.failure("未找到指定插件：" + pluginName));
    }
}
