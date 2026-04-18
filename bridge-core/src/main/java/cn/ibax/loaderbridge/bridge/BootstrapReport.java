package cn.ibax.loaderbridge.bridge;

import java.util.List;
import java.util.Set;

/**
 * 桥接启动结果。
 */
public record BootstrapReport(
        boolean available,
        String modId,
        String runtimeName,
        Set<BridgeCapability> capabilities,
        List<String> pluginNames,
        String message,
        Throwable error
) {
    public static BootstrapReport notBootstrapped(final String modId) {
        return new BootstrapReport(false, modId, "未初始化", Set.of(), List.of(), "尚未执行桥接初始化", null);
    }

    public static BootstrapReport available(
            final String modId,
            final String runtimeName,
            final Set<BridgeCapability> capabilities,
            final List<String> pluginNames
    ) {
        return new BootstrapReport(true, modId, runtimeName, Set.copyOf(capabilities), List.copyOf(pluginNames), "桥接已启用", null);
    }

    public static BootstrapReport unavailable(
            final String modId,
            final String runtimeName,
            final Set<BridgeCapability> capabilities,
            final List<String> pluginNames,
            final String message
    ) {
        return new BootstrapReport(false, modId, runtimeName, Set.copyOf(capabilities), List.copyOf(pluginNames), message, null);
    }

    public static BootstrapReport failure(
            final String modId,
            final String runtimeName,
            final Set<BridgeCapability> capabilities,
            final List<String> pluginNames,
            final String message,
            final Throwable error
    ) {
        return new BootstrapReport(false, modId, runtimeName, Set.copyOf(capabilities), List.copyOf(pluginNames), message, error);
    }
}
