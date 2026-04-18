package cn.ibax.loaderbridge.placeholder;

import java.util.List;

/**
 * 占位符桥启动报告。
 */
public record PlaceholderBootstrapReport(
        boolean enabled,
        boolean placeholderApiAvailable,
        String runtimeName,
        String message,
        Throwable error,
        List<String> mirroredNamespaces,
        int exactDefinitionCount,
        int namespaceDefinitionCount,
        int mirroredExpansionCount
) {
    public PlaceholderBootstrapReport {
        runtimeName = runtimeName == null ? "未知运行时" : runtimeName;
        message = message == null ? "" : message;
        mirroredNamespaces = mirroredNamespaces == null ? List.of() : List.copyOf(mirroredNamespaces);
    }

    public static PlaceholderBootstrapReport disabled(final String message) {
        return new PlaceholderBootstrapReport(false, false, "占位符桥已禁用", message, null, List.of(), 0, 0, 0);
    }

    public static PlaceholderBootstrapReport disabled(final String message, final Throwable error) {
        return new PlaceholderBootstrapReport(false, false, "占位符桥已禁用", message, error, List.of(), 0, 0, 0);
    }

    public static PlaceholderBootstrapReport enabled(
            final String runtimeName,
            final boolean placeholderApiAvailable,
            final String message,
            final List<String> mirroredNamespaces,
            final int exactDefinitionCount,
            final int namespaceDefinitionCount,
            final int mirroredExpansionCount
    ) {
        return new PlaceholderBootstrapReport(true, placeholderApiAvailable, runtimeName, message, null, mirroredNamespaces, exactDefinitionCount, namespaceDefinitionCount, mirroredExpansionCount);
    }
}
