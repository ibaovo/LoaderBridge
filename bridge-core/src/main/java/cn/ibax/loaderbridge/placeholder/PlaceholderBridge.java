package cn.ibax.loaderbridge.placeholder;

import cn.ibax.loaderbridge.bridge.BridgeManager;
import cn.ibax.loaderbridge.bridge.PluginHandle;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * 占位符桥静态入口。
 */
public final class PlaceholderBridge {
    private static final Logger LOGGER = Logger.getLogger(PlaceholderBridge.class.getName());
    private static final AtomicReference<Supplier<Optional<String>>> NAMESPACE_RESOLVER =
            new AtomicReference<>(PlaceholderBridge::resolveNamespaceFromForgeContext);
    private static final AtomicReference<PlaceholderBridgeRuntime> RUNTIME =
            new AtomicReference<>(PlaceholderBridgeRuntime.disabled(PlaceholderBootstrapReport.disabled("尚未启动")));

    private PlaceholderBridge() {
    }

    public static PlaceholderBootstrapReport bootstrap() {
        PlaceholderBridgeRuntime runtime = createRuntime();
        RUNTIME.set(runtime);
        return runtime.bootstrapReport();
    }

    public static boolean available() {
        return RUNTIME.get().available();
    }

    public static PlaceholderBootstrapReport bootstrapReport() {
        return RUNTIME.get().bootstrapReport();
    }

    public static PlaceholderRegistrationResult register(final String path, final PlaceholderResolver resolver) {
        if (!available()) {
            return PlaceholderRegistrationResult.disabled("占位符桥已禁用");
        }
        return resolveCurrentNamespace()
                .map(namespace -> register(namespace, path, resolver))
                .orElseGet(() -> PlaceholderRegistrationResult.invalid("无法推导默认命名空间，请显式传入 modid"));
    }

    public static PlaceholderRegistrationResult register(
            final String namespace,
            final String path,
            final PlaceholderResolver resolver
    ) {
        return RUNTIME.get().registerExact(namespace, path, resolver);
    }

    public static PlaceholderRegistrationResult registerNamespace(final PlaceholderResolver resolver) {
        if (!available()) {
            return PlaceholderRegistrationResult.disabled("占位符桥已禁用");
        }
        return resolveCurrentNamespace()
                .map(namespace -> registerNamespace(namespace, resolver))
                .orElseGet(() -> PlaceholderRegistrationResult.invalid("无法推导默认命名空间，请显式传入 modid"));
    }

    public static PlaceholderRegistrationResult registerNamespace(
            final String namespace,
            final PlaceholderResolver resolver
    ) {
        return RUNTIME.get().registerNamespace(namespace, resolver);
    }

    public static List<PlaceholderDefinition> definitions() {
        return RUNTIME.get().definitions();
    }

    public static Set<String> namespaces() {
        return RUNTIME.get().namespaces();
    }

    public static String render(final String input, final PlaceholderContext context) {
        return RUNTIME.get().render(input, context);
    }

    public static void installNamespaceResolverForTesting(final Supplier<Optional<String>> resolver) {
        NAMESPACE_RESOLVER.set(resolver == null ? PlaceholderBridge::resolveNamespaceFromForgeContext : resolver);
    }

    public static void resetForTesting() {
        NAMESPACE_RESOLVER.set(PlaceholderBridge::resolveNamespaceFromForgeContext);
        RUNTIME.set(PlaceholderBridgeRuntime.disabled(PlaceholderBootstrapReport.disabled("尚未启动")));
    }

    private static PlaceholderBridgeRuntime createRuntime() {
        Optional<PluginHandle> placeholderApi = findPlaceholderApiPlugin();
        if (placeholderApi.isEmpty()) {
            LOGGER.info("未检测到 PlaceholderAPI，占位符桥保持禁用");
            return PlaceholderBridgeRuntime.disabled(PlaceholderBootstrapReport.disabled("未检测到 PlaceholderAPI"));
        }

        try {
            return PlaceholderBridgeRuntime.enabled(placeholderApi.get());
        } catch (Throwable throwable) {
            LOGGER.warning("占位符桥启动失败，已自动禁用：" + throwable.getMessage());
            return PlaceholderBridgeRuntime.disabled(PlaceholderBootstrapReport.disabled("占位符桥启动失败", throwable));
        }
    }

    private static Optional<PluginHandle> findPlaceholderApiPlugin() {
        return BridgeManager.plugins().stream()
                .filter(plugin -> "placeholderapi".equals(plugin.name().toLowerCase(Locale.ROOT)))
                .findFirst();
    }

    private static Optional<String> resolveCurrentNamespace() {
        return NAMESPACE_RESOLVER.get().get();
    }

    private static Optional<String> resolveNamespaceFromForgeContext() {
        try {
            ClassLoader classLoader = PlaceholderBridge.class.getClassLoader();
            Class<?> contextType = Class.forName("net.minecraftforge.fml.ModLoadingContext", false, classLoader);
            Method get = contextType.getMethod("get");
            Object context = get.invoke(null);
            Method activeContainer = context.getClass().getMethod("getActiveContainer");
            Object container = activeContainer.invoke(context);
            if (container == null) {
                return Optional.empty();
            }

            Method getModId = container.getClass().getMethod("getModId");
            Object modId = getModId.invoke(container);
            if (modId instanceof String value && !value.isBlank()) {
                return Optional.of(value.trim());
            }
        } catch (ReflectiveOperationException ignored) {
            // 回退到显式传入 modid。
        }

        return Optional.empty();
    }
}
