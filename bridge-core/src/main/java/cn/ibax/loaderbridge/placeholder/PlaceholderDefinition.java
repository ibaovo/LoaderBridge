package cn.ibax.loaderbridge.placeholder;

import java.util.List;
import java.util.Objects;

/**
 * 占位符定义。
 */
public record PlaceholderDefinition(
        PlaceholderKey key,
        PlaceholderScope scope,
        PlaceholderSource source,
        String description,
        List<String> placeholders,
        PlaceholderResolver resolver
) {
    public PlaceholderDefinition {
        key = Objects.requireNonNull(key, "占位符键不能为空");
        scope = Objects.requireNonNull(scope, "占位符作用域不能为空");
        source = Objects.requireNonNull(source, "占位符来源不能为空");
        resolver = Objects.requireNonNull(resolver, "占位符解析器不能为空");
        placeholders = placeholders == null ? List.of() : List.copyOf(placeholders);
        description = description == null ? "" : description.trim();
    }

    public boolean isExact() {
        return scope == PlaceholderScope.EXACT;
    }

    public boolean isNamespace() {
        return scope == PlaceholderScope.NAMESPACE;
    }
}
