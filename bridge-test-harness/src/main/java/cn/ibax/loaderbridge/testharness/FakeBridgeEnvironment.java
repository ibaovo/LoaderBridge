package cn.ibax.loaderbridge.testharness;

import cn.ibax.loaderbridge.bridge.BridgeCapability;
import cn.ibax.loaderbridge.bridge.BridgeEnvironment;
import cn.ibax.loaderbridge.bridge.ModHandle;
import cn.ibax.loaderbridge.bridge.PluginHandle;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 测试用的伪桥接环境。
 */
public final class FakeBridgeEnvironment implements BridgeEnvironment {
    private final ModHandle currentMod;
    private final List<PluginHandle> plugins;
    private final Map<Class<?>, Object> services;

    public FakeBridgeEnvironment(final ModHandle currentMod, final List<PluginHandle> plugins, final Map<Class<?>, Object> services) {
        this.currentMod = currentMod;
        this.plugins = List.copyOf(plugins);
        this.services = Map.copyOf(services);
    }

    public static FakeBridgeEnvironment standard() {
        TestPlugin plugin = new TestPlugin();
        FakePluginHandle pluginHandle = new FakePluginHandle(plugin);
        FakeModHandle modHandle = new FakeModHandle("loaderbridge", new TestModAnchor());
        return new FakeBridgeEnvironment(modHandle, List.of(pluginHandle), Map.of(TestService.class, new TestService("已注入服务")));
    }

    public static FakeBridgeEnvironment withPlaceholderApi() {
        TestPlugin plugin = new TestPlugin();
        FakePlaceholderApiPlugin placeholderApiPlugin = FakePlaceholderApiPlugin.instance();
        FakePluginHandle pluginHandle = new FakePluginHandle(plugin);
        FakePluginHandle placeholderApiHandle = new FakePluginHandle(placeholderApiPlugin);
        FakeModHandle modHandle = new FakeModHandle("loaderbridge", new TestModAnchor());
        return new FakeBridgeEnvironment(
                modHandle,
                List.of(pluginHandle, placeholderApiHandle),
                Map.of(TestService.class, new TestService("已注入服务"))
        );
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String runtimeName() {
        return "测试运行时";
    }

    @Override
    public Set<BridgeCapability> capabilities() {
        return EnumSet.of(
                BridgeCapability.MOD_HANDLE,
                BridgeCapability.PLUGIN_LOOKUP,
                BridgeCapability.PLUGIN_CLASS_LOADING,
                BridgeCapability.PLUGIN_INSTANCE_ACCESS,
                BridgeCapability.METHOD_INVOCATION,
                BridgeCapability.SERVICE_LOOKUP
        );
    }

    @Override
    public Optional<ModHandle> currentMod() {
        return Optional.ofNullable(currentMod);
    }

    @Override
    public List<PluginHandle> plugins() {
        return plugins;
    }

    @Override
    public Optional<PluginHandle> findPlugin(final String pluginName) {
        return plugins.stream().filter(handle -> handle.name().equals(pluginName)).findFirst();
    }

    @Override
    public Optional<Object> findService(final Class<?> serviceType) {
        return Optional.ofNullable(services.get(serviceType));
    }
}
