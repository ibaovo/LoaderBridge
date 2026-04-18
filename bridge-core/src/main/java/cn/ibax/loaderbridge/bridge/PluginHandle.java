package cn.ibax.loaderbridge.bridge;

import java.util.Optional;

/**
 * 被桥接层识别到的插件句柄。
 */
public interface PluginHandle {
    String name();

    String mainClassName();

    ClassLoader classLoader();

    Optional<Object> instance();

    Optional<Class<?>> findClass(String className);

    BridgeResult<Object> invokeStatic(String className, String methodName, Class<?>[] parameterTypes, Object... args);

    BridgeResult<Object> invokeInstance(Object target, String methodName, Class<?>[] parameterTypes, Object... args);

    BridgeResult<Object> newInstance(String className, Class<?>[] parameterTypes, Object... args);
}
