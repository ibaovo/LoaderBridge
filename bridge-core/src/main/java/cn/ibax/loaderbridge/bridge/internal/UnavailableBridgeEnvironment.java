package cn.ibax.loaderbridge.bridge.internal;

import cn.ibax.loaderbridge.bridge.BridgeCapability;
import cn.ibax.loaderbridge.bridge.BridgeEnvironment;
import cn.ibax.loaderbridge.bridge.ModHandle;
import cn.ibax.loaderbridge.bridge.PluginHandle;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class UnavailableBridgeEnvironment implements BridgeEnvironment {
    private final LocalModHandle currentMod;
    private final String message;

    public UnavailableBridgeEnvironment(final LocalModHandle currentMod, final String message) {
        this.currentMod = currentMod;
        this.message = message;
    }

    public static UnavailableBridgeEnvironment uninitialized() {
        return new UnavailableBridgeEnvironment(null, "尚未执行桥接初始化");
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public String runtimeName() {
        return currentMod == null ? "未初始化" : "纯 Forge/未知运行时";
    }

    @Override
    public Set<BridgeCapability> capabilities() {
        if (currentMod == null) {
            return Set.of();
        }
        return Set.of(BridgeCapability.MOD_HANDLE);
    }

    @Override
    public Optional<ModHandle> currentMod() {
        return Optional.ofNullable(currentMod);
    }

    @Override
    public List<PluginHandle> plugins() {
        return List.of();
    }

    @Override
    public Optional<PluginHandle> findPlugin(final String pluginName) {
        return Optional.empty();
    }

    @Override
    public Optional<Object> findService(final Class<?> serviceType) {
        return Optional.empty();
    }

    public String message() {
        return message;
    }
}
