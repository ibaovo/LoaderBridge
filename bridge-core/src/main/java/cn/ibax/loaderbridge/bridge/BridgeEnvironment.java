package cn.ibax.loaderbridge.bridge;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 运行时桥接环境。
 */
public interface BridgeEnvironment {
    boolean available();

    String runtimeName();

    Set<BridgeCapability> capabilities();

    Optional<ModHandle> currentMod();

    List<PluginHandle> plugins();

    Optional<PluginHandle> findPlugin(String pluginName);

    Optional<Object> findService(Class<?> serviceType);
}
