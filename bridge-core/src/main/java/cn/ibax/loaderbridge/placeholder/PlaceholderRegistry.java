package cn.ibax.loaderbridge.placeholder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 占位符注册表。
 */
public final class PlaceholderRegistry {
    private final Map<PlaceholderKey, PlaceholderDefinition> exactDefinitions = new LinkedHashMap<>();
    private final Map<String, PlaceholderDefinition> namespaceDefinitions = new LinkedHashMap<>();

    public synchronized PlaceholderRegistrationResult register(final PlaceholderDefinition definition) {
        Objects.requireNonNull(definition, "占位符定义不能为空");
        if (definition.isExact()) {
            return registerExact(definition);
        }
        return registerNamespace(definition);
    }

    public synchronized PlaceholderRegistrationResult registerExact(
            final String namespace,
            final String path,
            final PlaceholderResolver resolver,
            final String description,
            final List<String> placeholders,
            final PlaceholderSource source
    ) {
        return register(new PlaceholderDefinition(
                PlaceholderKey.of(namespace, path),
                PlaceholderScope.EXACT,
                source,
                description,
                placeholders,
                resolver
        ));
    }

    public synchronized PlaceholderRegistrationResult registerNamespace(
            final String namespace,
            final PlaceholderResolver resolver,
            final String description,
            final List<String> placeholders,
            final PlaceholderSource source
    ) {
        return register(new PlaceholderDefinition(
                PlaceholderKey.namespace(namespace),
                PlaceholderScope.NAMESPACE,
                source,
                description,
                placeholders,
                resolver
        ));
    }

    public synchronized List<PlaceholderDefinition> definitions() {
        List<PlaceholderDefinition> definitions = new ArrayList<>(exactDefinitions.values());
        definitions.addAll(namespaceDefinitions.values());
        return List.copyOf(definitions);
    }

    public synchronized Set<String> namespaces() {
        Set<String> namespaces = new LinkedHashSet<>();
        exactDefinitions.keySet().forEach(key -> namespaces.add(key.namespace()));
        namespaces.addAll(namespaceDefinitions.keySet());
        return Set.copyOf(namespaces);
    }

    public synchronized List<String> placeholdersForNamespace(final String namespace) {
        String normalized = PlaceholderKey.namespace(namespace).namespace();
        List<String> placeholders = new ArrayList<>();
        exactDefinitions.entrySet().stream()
                .filter(entry -> entry.getKey().namespace().equals(normalized))
                .map(entry -> entry.getKey().path())
                .filter(Objects::nonNull)
                .forEach(placeholders::add);
        PlaceholderDefinition namespaceDefinition = namespaceDefinitions.get(normalized);
        if (namespaceDefinition != null) {
            placeholders.addAll(namespaceDefinition.placeholders());
        }
        return List.copyOf(placeholders);
    }

    public synchronized Optional<PlaceholderDefinition> findExact(final String namespace, final String path) {
        return Optional.ofNullable(exactDefinitions.get(PlaceholderKey.of(namespace, path)));
    }

    public synchronized Optional<PlaceholderDefinition> findNamespace(final String namespace) {
        return Optional.ofNullable(namespaceDefinitions.get(PlaceholderKey.namespace(namespace).namespace()));
    }

    public synchronized Optional<String> resolve(
            final String namespace,
            final String rawParameters,
            final PlaceholderContext context
    ) {
        String normalizedNamespace = PlaceholderKey.namespace(namespace).namespace();
        String normalizedParameters = rawParameters == null ? "" : rawParameters;

        PlaceholderDefinition exact = exactDefinitions.get(PlaceholderKey.of(normalizedNamespace, normalizedParameters));
        if (exact != null) {
            return Optional.ofNullable(exact.resolver().resolve(
                    context.withResolution(exact, normalizedNamespace, normalizedParameters, "")
            ));
        }

        PlaceholderDefinition namespaceDefinition = namespaceDefinitions.get(normalizedNamespace);
        if (namespaceDefinition != null) {
            return Optional.ofNullable(namespaceDefinition.resolver().resolve(
                    context.withResolution(namespaceDefinition, normalizedNamespace, null, normalizedParameters)
            ));
        }

        return Optional.empty();
    }

    public synchronized String render(final String input, final PlaceholderContext context) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        PlaceholderContext baseContext = context == null ? PlaceholderContext.empty() : context;
        StringBuilder rendered = new StringBuilder(input.length());

        for (int index = 0; index < input.length(); ) {
            char current = input.charAt(index);
            if (current != '%') {
                rendered.append(current);
                index++;
                continue;
            }

            int end = input.indexOf('%', index + 1);
            if (end < 0) {
                rendered.append(input.substring(index));
                break;
            }

            String token = input.substring(index + 1, end);
            if (token.isEmpty()) {
                rendered.append("%%");
                index = end + 1;
                continue;
            }

            String replacement = resolveToken(token, baseContext).orElse(null);
            if (replacement == null) {
                rendered.append('%').append(token).append('%');
            } else {
                rendered.append(replacement);
            }
            index = end + 1;
        }

        return rendered.toString();
    }

    private PlaceholderRegistrationResult registerExact(final PlaceholderDefinition definition) {
        PlaceholderKey key = definition.key();
        if (exactDefinitions.containsKey(key)) {
            return PlaceholderRegistrationResult.duplicate(definition, "占位符已存在：" + key.displayName());
        }

        exactDefinitions.put(key, definition);
        return PlaceholderRegistrationResult.registered(definition, "已注册占位符：" + key.displayName());
    }

    private PlaceholderRegistrationResult registerNamespace(final PlaceholderDefinition definition) {
        PlaceholderKey key = definition.key();
        String namespace = key.namespace();
        if (namespaceDefinitions.containsKey(namespace)) {
            return PlaceholderRegistrationResult.duplicate(definition, "命名空间已存在：" + namespace);
        }

        namespaceDefinitions.put(namespace, definition);
        return PlaceholderRegistrationResult.registered(definition, "已注册命名空间：" + namespace);
    }

    private Optional<String> resolveToken(final String token, final PlaceholderContext baseContext) {
        int separator = token.indexOf('_');
        if (separator < 0) {
            return resolve(token, "", baseContext);
        }

        String namespace = token.substring(0, separator);
        String parameters = token.substring(separator + 1);
        return resolve(namespace, parameters, baseContext);
    }
}
