package cn.ibax.loaderbridge.bridge.internal;

import cn.ibax.loaderbridge.bridge.BridgeCapability;
import cn.ibax.loaderbridge.bridge.BridgeEnvironment;
import cn.ibax.loaderbridge.bridge.ModHandle;
import cn.ibax.loaderbridge.bridge.PluginHandle;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class BukkitBridgeEnvironment implements BridgeEnvironment {
    private final LocalModHandle currentMod;
    private final Class<?> bukkitClass;
    private final Method getPluginManagerMethod;
    private final Method getServicesManagerMethod;
    private final Method getServerMethod;

    public BukkitBridgeEnvironment(
            final LocalModHandle currentMod,
            final Class<?> bukkitClass,
            final Method getPluginManagerMethod,
            final Method getServicesManagerMethod,
            final Method getServerMethod
    ) {
        this.currentMod = Objects.requireNonNull(currentMod, "当前模组句柄不能为空");
        this.bukkitClass = Objects.requireNonNull(bukkitClass, "Bukkit 类不能为空");
        this.getPluginManagerMethod = Objects.requireNonNull(getPluginManagerMethod, "插件管理器方法不能为空");
        this.getServicesManagerMethod = Objects.requireNonNull(getServicesManagerMethod, "服务管理器方法不能为空");
        this.getServerMethod = Objects.requireNonNull(getServerMethod, "服务器方法不能为空");
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String runtimeName() {
        try {
            Object server = getServerMethod.invoke(null);
            if (server != null) {
                Optional<Object> name = ReflectionSupport.invokeNoArg(server, "getName");
                if (name.isPresent()) {
                    return String.valueOf(name.get());
                }
                return server.getClass().getSimpleName();
            }
            return "Bukkit 兼容运行时";
        } catch (ReflectiveOperationException exception) {
            return "Bukkit 兼容运行时";
        }
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
        return Optional.of(currentMod);
    }

    @Override
    public List<PluginHandle> plugins() {
        Object pluginManager = pluginManager();
        if (pluginManager == null) {
            return List.of();
        }

        Object[] plugins = invokePlugins(pluginManager);
        if (plugins.length == 0) {
            return List.of();
        }

        List<PluginHandle> handles = new ArrayList<>(plugins.length);
        for (Object plugin : plugins) {
            if (plugin != null) {
                handles.add(new BukkitPluginHandle(plugin));
            }
        }
        return List.copyOf(handles);
    }

    @Override
    public Optional<PluginHandle> findPlugin(final String pluginName) {
        Object pluginManager = pluginManager();
        if (pluginManager == null) {
            return Optional.empty();
        }

        try {
            Method getPlugin = pluginManager.getClass().getMethod("getPlugin", String.class);
            Object plugin = getPlugin.invoke(pluginManager, pluginName);
            if (plugin == null) {
                return Optional.empty();
            }
            return Optional.of(new BukkitPluginHandle(plugin));
        } catch (ReflectiveOperationException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Object> findService(final Class<?> serviceType) {
        Object servicesManager = servicesManager();
        if (servicesManager == null) {
            return Optional.empty();
        }

        try {
            Method getRegistration = servicesManager.getClass().getMethod("getRegistration", Class.class);
            Object registration = getRegistration.invoke(servicesManager, serviceType);
            if (registration == null) {
                return Optional.empty();
            }

            Method getProvider = registration.getClass().getMethod("getProvider");
            Object provider = getProvider.invoke(registration);
            return Optional.ofNullable(provider);
        } catch (ReflectiveOperationException exception) {
            return Optional.empty();
        }
    }

    private Object pluginManager() {
        try {
            return getPluginManagerMethod.invoke(null);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private Object servicesManager() {
        try {
            return getServicesManagerMethod.invoke(null);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private Object[] invokePlugins(final Object pluginManager) {
        try {
            Method getPlugins = pluginManager.getClass().getMethod("getPlugins");
            Object result = getPlugins.invoke(pluginManager);
            if (result instanceof Object[] plugins) {
                return plugins;
            }
        } catch (ReflectiveOperationException ignored) {
            return new Object[0];
        }
        return new Object[0];
    }
}
