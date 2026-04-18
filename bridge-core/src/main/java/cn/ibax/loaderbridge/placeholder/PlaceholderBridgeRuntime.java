package cn.ibax.loaderbridge.placeholder;

import cn.ibax.loaderbridge.bridge.PluginHandle;
import cn.ibax.loaderbridge.placeholder.internal.PlaceholderApiAdapter;
import cn.ibax.loaderbridge.placeholder.internal.ReflectionPlaceholderApiAdapter;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 占位符桥运行时实现。
 */
public class PlaceholderBridgeRuntime {
    private final boolean available;
    private final PlaceholderBootstrapReport bootstrapReport;
    private final PlaceholderRegistry registry;
    private final PlaceholderApiAdapter placeholderApiAdapter;
    private final Set<String> externalNamespaces;
    private final Set<String> mirroredLocalNamespaces;

    protected PlaceholderBridgeRuntime(
            final boolean available,
            final PlaceholderBootstrapReport bootstrapReport,
            final PlaceholderRegistry registry,
            final PlaceholderApiAdapter placeholderApiAdapter,
            final Set<String> externalNamespaces
    ) {
        this.available = available;
        this.bootstrapReport = bootstrapReport;
        this.registry = registry;
        this.placeholderApiAdapter = placeholderApiAdapter;
        this.externalNamespaces = externalNamespaces;
        this.mirroredLocalNamespaces = ConcurrentHashMap.newKeySet();
    }

    public static PlaceholderBridgeRuntime disabled(final PlaceholderBootstrapReport bootstrapReport) {
        return new PlaceholderBridgeRuntime(false, bootstrapReport, new PlaceholderRegistry(), null, Set.of());
    }

    public static PlaceholderBridgeRuntime enabled(final PluginHandle placeholderApi) {
        PlaceholderApiAdapter adapter = ReflectionPlaceholderApiAdapter.tryCreate(placeholderApi)
                .orElseThrow(() -> new IllegalStateException("无法创建 PlaceholderAPI 适配器"));

        PlaceholderRegistry registry = new PlaceholderRegistry();
        List<PlaceholderDefinition> mirroredDefinitions = adapter.snapshotDefinitions();
        for (PlaceholderDefinition definition : mirroredDefinitions) {
            registry.register(definition);
        }

        Set<String> externalNamespaces = mirroredDefinitions.stream()
                .map(definition -> definition.key().namespace())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        PlaceholderBootstrapReport report = PlaceholderBootstrapReport.enabled(
                placeholderApi.name(),
                true,
                "占位符桥已启用",
                List.copyOf(externalNamespaces),
                registry.definitions().stream().mapToInt(definition -> definition.isExact() ? 1 : 0).sum(),
                registry.definitions().stream().mapToInt(definition -> definition.isNamespace() ? 1 : 0).sum(),
                externalNamespaces.size()
        );

        return new PlaceholderBridgeRuntime(true, report, registry, adapter, externalNamespaces);
    }

    public boolean available() {
        return available;
    }

    public PlaceholderBootstrapReport bootstrapReport() {
        return bootstrapReport;
    }

    public PlaceholderRegistrationResult registerExact(
            final String namespace,
            final String path,
            final PlaceholderResolver resolver
    ) {
        if (!available) {
            return PlaceholderRegistrationResult.disabled("占位符桥已禁用");
        }
        if (resolver == null) {
            return PlaceholderRegistrationResult.invalid("占位符解析器不能为空");
        }
        if (path == null || path.trim().isEmpty()) {
            return PlaceholderRegistrationResult.invalid("占位符路径不能为空");
        }
        String normalizedNamespace = PlaceholderKey.namespace(namespace).namespace();
        if (externalNamespaces.contains(normalizedNamespace)) {
            return PlaceholderRegistrationResult.duplicate(
                    new PlaceholderDefinition(
                            PlaceholderKey.of(normalizedNamespace, path),
                            PlaceholderScope.EXACT,
                            PlaceholderSource.LOCAL,
                            "",
                            List.of(path.trim()),
                            resolver
                    ),
                    "命名空间已被 PlaceholderAPI 占用：" + normalizedNamespace
            );
        }

        PlaceholderRegistrationResult result = registry.registerExact(
                normalizedNamespace,
                path.trim(),
                resolver,
                "",
                List.of(path.trim()),
                PlaceholderSource.LOCAL
        );
        mirrorLocalNamespaceIfNeeded(normalizedNamespace, resolver);
        return result;
    }

    public PlaceholderRegistrationResult registerNamespace(
            final String namespace,
            final PlaceholderResolver resolver
    ) {
        if (!available) {
            return PlaceholderRegistrationResult.disabled("占位符桥已禁用");
        }
        if (resolver == null) {
            return PlaceholderRegistrationResult.invalid("占位符解析器不能为空");
        }
        String normalizedNamespace = PlaceholderKey.namespace(namespace).namespace();
        if (externalNamespaces.contains(normalizedNamespace)) {
            return PlaceholderRegistrationResult.duplicate(
                    new PlaceholderDefinition(
                            PlaceholderKey.namespace(normalizedNamespace),
                            PlaceholderScope.NAMESPACE,
                            PlaceholderSource.LOCAL,
                            "",
                            List.of(),
                            resolver
                    ),
                    "命名空间已被 PlaceholderAPI 占用：" + normalizedNamespace
            );
        }

        PlaceholderRegistrationResult result = registry.registerNamespace(
                normalizedNamespace,
                resolver,
                "",
                List.of(),
                PlaceholderSource.LOCAL
        );
        mirrorLocalNamespaceIfNeeded(normalizedNamespace, resolver);
        return result;
    }

    public List<PlaceholderDefinition> definitions() {
        return registry.definitions();
    }

    public Set<String> namespaces() {
        return registry.namespaces();
    }

    public String render(final String input, final PlaceholderContext context) {
        if (!available) {
            return input;
        }
        return registry.render(input, context);
    }

    private void mirrorLocalNamespaceIfNeeded(final String namespace, final PlaceholderResolver resolver) {
        if (placeholderApiAdapter == null || mirroredLocalNamespaces.contains(namespace)) {
            return;
        }

        PlaceholderRegistrationResult mirrorResult = placeholderApiAdapter.mirrorNamespace(
                namespace,
                resolver,
                () -> registry.placeholdersForNamespace(namespace)
        );
        if (mirrorResult.registered()) {
            mirroredLocalNamespaces.add(namespace);
        }
    }
}
