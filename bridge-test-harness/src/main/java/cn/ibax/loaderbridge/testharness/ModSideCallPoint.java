package cn.ibax.loaderbridge.testharness;

import cn.ibax.loaderbridge.bridge.BridgeManager;

import java.util.Optional;

/**
 * 模组侧的调用入口，用于验证桥接调用链。
 */
public final class ModSideCallPoint {
    public Optional<String> greetPlugin(final String pluginName, final String targetName) {
        return BridgeManager.invokePluginInstance(pluginName, "greet", new Class<?>[]{String.class}, targetName)
                .asOptional()
                .map(String::valueOf);
    }
}
