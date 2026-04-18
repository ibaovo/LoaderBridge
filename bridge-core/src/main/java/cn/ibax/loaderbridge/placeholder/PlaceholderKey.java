package cn.ibax.loaderbridge.placeholder;

import java.util.Locale;
import java.util.Objects;

/**
 * 占位符键。
 */
public record PlaceholderKey(String namespace, String path) {
    public PlaceholderKey {
        namespace = normalizeNamespace(namespace);
        path = normalizePath(path);
    }

    public static PlaceholderKey of(final String namespace, final String path) {
        return new PlaceholderKey(namespace, path);
    }

    public static PlaceholderKey namespace(final String namespace) {
        return new PlaceholderKey(namespace, null);
    }

    public boolean hasPath() {
        return path != null && !path.isBlank();
    }

    public String displayName() {
        return hasPath() ? namespace + ':' + path : namespace;
    }

    private static String normalizeNamespace(final String namespace) {
        String value = Objects.requireNonNull(namespace, "命名空间不能为空").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("命名空间不能为空");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String normalizePath(final String path) {
        if (path == null) {
            return null;
        }
        String value = path.trim();
        return value.isEmpty() ? null : value;
    }
}
