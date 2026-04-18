package cn.ibax.loaderbridge.bridge;

import cn.ibax.loaderbridge.bridge.internal.BukkitBridgeEnvironment;
import cn.ibax.loaderbridge.bridge.internal.LocalModHandle;
import cn.ibax.loaderbridge.bridge.internal.ReflectionSupport;
import cn.ibax.loaderbridge.bridge.internal.UnavailableBridgeEnvironment;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 桥接层启动入口。
 */
public final class BridgeBootstrap {
    private BridgeBootstrap() {
    }

    public static BootstrapReport bootstrap(final Object modAnchor, final String modId) {
        Objects.requireNonNull(modAnchor, "模组锚点不能为空");
        Objects.requireNonNull(modId, "模组 ID 不能为空");

        LocalModHandle localMod = new LocalModHandle(modId, modAnchor);
        ClassLoader classLoader = Objects.requireNonNullElse(modAnchor.getClass().getClassLoader(), BridgeBootstrap.class.getClassLoader());

        try {
            Optional<Class<?>> bukkitClass = ReflectionSupport.tryLoad("org.bukkit.Bukkit", classLoader);
            if (bukkitClass.isEmpty()) {
                return publishUnavailable(localMod, modId, "未检测到 Bukkit/Spigot 运行时");
            }

            Method getPluginManager = ReflectionSupport.findStaticMethod(bukkitClass.get(), "getPluginManager");
            Method getServicesManager = ReflectionSupport.findStaticMethod(bukkitClass.get(), "getServicesManager");
            Method getServer = ReflectionSupport.findStaticMethod(bukkitClass.get(), "getServer");

            if (getPluginManager == null || getServicesManager == null || getServer == null) {
                return publishUnavailable(localMod, modId, "检测到 Bukkit，但关键入口方法不完整");
            }

            BukkitBridgeEnvironment environment = new BukkitBridgeEnvironment(
                    localMod,
                    bukkitClass.get(),
                    getPluginManager,
                    getServicesManager,
                    getServer
            );
            BridgeManager.install(environment);
            return BootstrapReport.available(
                    modId,
                    environment.runtimeName(),
                    environment.capabilities(),
                    environment.plugins().stream().map(PluginHandle::name).toList()
            );
        } catch (Throwable throwable) {
            BridgeEnvironment environment = new UnavailableBridgeEnvironment(localMod, "桥接初始化失败，已自动禁用");
            BridgeManager.install(environment);
            return BootstrapReport.failure(
                    modId,
                    environment.runtimeName(),
                    environment.capabilities(),
                    environment.plugins().stream().map(PluginHandle::name).toList(),
                    "桥接初始化失败，已自动禁用",
                    throwable
            );
        }
    }

    private static BootstrapReport publishUnavailable(final LocalModHandle localMod, final String modId, final String message) {
        BridgeEnvironment environment = new UnavailableBridgeEnvironment(localMod, message);
        BridgeManager.install(environment);
        return BootstrapReport.unavailable(
                modId,
                environment.runtimeName(),
                environment.capabilities(),
                environment.plugins().stream().map(PluginHandle::name).toList(),
                message
        );
    }
}
