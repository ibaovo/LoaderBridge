package cn.ibax.loaderbridge.placeholder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 占位符解析上下文。
 */
public record PlaceholderContext(
        Object subject,
        Map<String, Object> attributes,
        String namespace,
        String matchedPath,
        String parameters,
        PlaceholderDefinition definition
) {
    public PlaceholderContext {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        namespace = normalize(namespace);
        matchedPath = normalize(matchedPath);
        parameters = parameters == null ? "" : parameters;
    }

    public static PlaceholderContext empty() {
        return new PlaceholderContext(null, Map.of(), null, null, "", null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public PlaceholderContext withResolution(
            final PlaceholderDefinition definition,
            final String namespace,
            final String matchedPath,
            final String parameters
    ) {
        return new PlaceholderContext(subject, attributes, namespace, matchedPath, parameters, definition);
    }

    public PlaceholderContext withSubject(final Object subject) {
        return new PlaceholderContext(subject, attributes, namespace, matchedPath, parameters, definition);
    }

    public PlaceholderContext withAttribute(final String key, final Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(attributes);
        copy.put(Objects.requireNonNull(key, "属性键不能为空"), value);
        return new PlaceholderContext(subject, copy, namespace, matchedPath, parameters, definition);
    }

    private static String normalize(final String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class Builder {
        private Object subject;
        private final Map<String, Object> attributes = new LinkedHashMap<>();
        private String namespace;
        private String matchedPath;
        private String parameters = "";
        private PlaceholderDefinition definition;

        private Builder() {
        }

        public Builder subject(final Object subject) {
            this.subject = subject;
            return this;
        }

        public Builder attribute(final String key, final Object value) {
            attributes.put(Objects.requireNonNull(key, "属性键不能为空"), value);
            return this;
        }

        public Builder namespace(final String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder matchedPath(final String matchedPath) {
            this.matchedPath = matchedPath;
            return this;
        }

        public Builder parameters(final String parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder definition(final PlaceholderDefinition definition) {
            this.definition = definition;
            return this;
        }

        public PlaceholderContext build() {
            return new PlaceholderContext(subject, attributes, namespace, matchedPath, parameters, definition);
        }
    }
}
