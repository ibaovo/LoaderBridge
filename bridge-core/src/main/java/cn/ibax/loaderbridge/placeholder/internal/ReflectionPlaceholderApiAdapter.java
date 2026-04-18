package cn.ibax.loaderbridge.placeholder.internal;

import cn.ibax.loaderbridge.bridge.PluginHandle;
import cn.ibax.loaderbridge.placeholder.PlaceholderBootstrapReport;
import cn.ibax.loaderbridge.placeholder.PlaceholderContext;
import cn.ibax.loaderbridge.placeholder.PlaceholderDefinition;
import cn.ibax.loaderbridge.placeholder.PlaceholderKey;
import cn.ibax.loaderbridge.placeholder.PlaceholderRegistrationResult;
import cn.ibax.loaderbridge.placeholder.PlaceholderRegistrationStatus;
import cn.ibax.loaderbridge.placeholder.PlaceholderResolver;
import cn.ibax.loaderbridge.placeholder.PlaceholderScope;
import cn.ibax.loaderbridge.placeholder.PlaceholderSource;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 通过反射接入 PlaceholderAPI。
 */
public final class ReflectionPlaceholderApiAdapter implements PlaceholderApiAdapter {
    private static final ConcurrentHashMap<ClassLoader, Class<?>> GENERATED_EXPANSION_CACHE = new ConcurrentHashMap<>();

    private final PluginHandle pluginHandle;
    private final Object pluginInstance;
    private final Object localExpansionManager;
    private final Class<?> placeholderExpansionBaseClass;
    private final Class<?> generatedExpansionClass;
    private final Method registerMethod;
    private final Method getIdentifiersMethod;
    private final Method findExpansionByIdentifierMethod;

    private ReflectionPlaceholderApiAdapter(
            final PluginHandle pluginHandle,
            final Object pluginInstance,
            final Object localExpansionManager,
            final Class<?> placeholderExpansionBaseClass,
            final Class<?> generatedExpansionClass,
            final Method registerMethod,
            final Method getIdentifiersMethod,
            final Method findExpansionByIdentifierMethod
    ) {
        this.pluginHandle = pluginHandle;
        this.pluginInstance = pluginInstance;
        this.localExpansionManager = localExpansionManager;
        this.placeholderExpansionBaseClass = placeholderExpansionBaseClass;
        this.generatedExpansionClass = generatedExpansionClass;
        this.registerMethod = registerMethod;
        this.getIdentifiersMethod = getIdentifiersMethod;
        this.findExpansionByIdentifierMethod = findExpansionByIdentifierMethod;
    }

    public static Optional<PlaceholderApiAdapter> tryCreate(final PluginHandle pluginHandle) {
        try {
            Optional<Object> pluginInstance = pluginHandle.instance();
            if (pluginInstance.isEmpty()) {
                return Optional.empty();
            }

            ClassLoader classLoader = pluginHandle.classLoader();
            Class<?> baseExpansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion", false, classLoader);
            Method getLocalExpansionManager = pluginInstance.get().getClass().getMethod("getLocalExpansionManager");
            Object localExpansionManager = getLocalExpansionManager.invoke(pluginInstance.get());
            if (localExpansionManager == null) {
                return Optional.empty();
            }

            Method registerMethod = findMethod(localExpansionManager.getClass(), "register", baseExpansionClass);
            Method getIdentifiersMethod = findMethod(localExpansionManager.getClass(), "getIdentifiers");
            Method findExpansionByIdentifierMethod = findMethod(localExpansionManager.getClass(), "findExpansionByIdentifier", String.class);
            if (registerMethod == null || getIdentifiersMethod == null || findExpansionByIdentifierMethod == null) {
                return Optional.empty();
            }

            Class<?> generatedExpansionClass = GENERATED_EXPANSION_CACHE.computeIfAbsent(
                    classLoader,
                    ignoredLoader -> PlaceholderExpansionGenerator.defineExpansionClass(baseExpansionClass)
            );
            return Optional.of(new ReflectionPlaceholderApiAdapter(
                    pluginHandle,
                    pluginInstance.get(),
                    localExpansionManager,
                    baseExpansionClass,
                    generatedExpansionClass,
                    registerMethod,
                    getIdentifiersMethod,
                    findExpansionByIdentifierMethod
            ));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    @Override
    public List<PlaceholderDefinition> snapshotDefinitions() {
        List<PlaceholderDefinition> definitions = new ArrayList<>();
        for (String identifier : identifiers()) {
            findExpansion(identifier).ifPresent(expansion -> definitions.add(snapshotExpansion(identifier, expansion)));
        }
        return List.copyOf(definitions);
    }

    @Override
    public PlaceholderRegistrationResult mirrorNamespace(
            final String namespace,
            final PlaceholderResolver resolver,
            final Supplier<List<String>> placeholdersSupplier
    ) {
        String normalizedNamespace = PlaceholderKey.namespace(namespace).namespace();
        if (findExpansion(normalizedNamespace).isPresent()) {
            return PlaceholderRegistrationResult.duplicate(
                    new PlaceholderDefinition(
                            PlaceholderKey.namespace(normalizedNamespace),
                            PlaceholderScope.NAMESPACE,
                            PlaceholderSource.LOCAL,
                            "",
                            List.of(),
                            resolver
                    ),
                    "PlaceholderAPI 中已存在相同标识：" + normalizedNamespace
            );
        }

        try {
            Constructor<?> constructor = generatedExpansionClass.getConstructor(
                    String.class,
                    String.class,
                    String.class,
                    BiFunction.class,
                    Supplier.class
            );
            Object expansion = constructor.newInstance(
                    normalizedNamespace,
                    "LoaderBridge",
                    runtimeVersion(),
                    (BiFunction<Object, String, String>) (subject, params) -> resolver.resolve(
                            PlaceholderContext.builder()
                                    .subject(subject)
                                    .parameters(params)
                                    .namespace(normalizedNamespace)
                                    .build()
                    ),
                    placeholdersSupplier
            );

            Object registered = registerMethod.invoke(localExpansionManager, expansion);
            if (registered instanceof Boolean result && !result) {
                return PlaceholderRegistrationResult.duplicate(
                        new PlaceholderDefinition(
                                PlaceholderKey.namespace(normalizedNamespace),
                                PlaceholderScope.NAMESPACE,
                                PlaceholderSource.LOCAL,
                                "",
                                placeholdersSupplier.get(),
                                resolver
                        ),
                        "PlaceholderAPI 拒绝注册相同标识：" + normalizedNamespace
                );
            }

            return PlaceholderRegistrationResult.registered(
                    new PlaceholderDefinition(
                            PlaceholderKey.namespace(normalizedNamespace),
                            PlaceholderScope.NAMESPACE,
                            PlaceholderSource.LOCAL,
                            "",
                            placeholdersSupplier.get(),
                            resolver
                    ),
                    "已回挂到 PlaceholderAPI：" + normalizedNamespace
            );
        } catch (Throwable throwable) {
            return PlaceholderRegistrationResult.unavailable(
                    "无法将占位符回挂到 PlaceholderAPI：" + normalizedNamespace,
                    throwable
            );
        }
    }

    private PlaceholderDefinition snapshotExpansion(final String identifier, final Object expansion) {
        List<String> placeholders = readPlaceholders(expansion);
        String author = readString(expansion, "getAuthor").orElse("未知作者");
        String version = readString(expansion, "getVersion").orElse("未知版本");
        PlaceholderResolver resolver = context -> resolveExpansion(expansion, context.subject(), context.parameters());

        return new PlaceholderDefinition(
                PlaceholderKey.namespace(identifier),
                PlaceholderScope.NAMESPACE,
                PlaceholderSource.MIRRORED_PAPI,
                author + " / " + version,
                placeholders,
                resolver
        );
    }

    private String resolveExpansion(final Object expansion, final Object subject, final String parameters) {
        try {
            Method onRequest = findMethod(expansion.getClass(), "onRequest", Class.forName("org.bukkit.OfflinePlayer"), String.class);
            if (onRequest != null) {
                Object result = onRequest.invoke(expansion, subject, parameters);
                if (result instanceof String value) {
                    return value;
                }
            }
        } catch (Throwable ignored) {
            // 继续尝试 Player 入口。
        }

        try {
            Method onPlaceholderRequest = findMethod(expansion.getClass(), "onPlaceholderRequest", Class.forName("org.bukkit.entity.Player"), String.class);
            if (onPlaceholderRequest != null) {
                Object result = onPlaceholderRequest.invoke(expansion, subject, parameters);
                if (result instanceof String value) {
                    return value;
                }
            }
        } catch (Throwable ignored) {
            // 放弃。
        }

        return null;
    }

    private List<String> readPlaceholders(final Object expansion) {
        try {
            Method method = findMethod(expansion.getClass(), "getPlaceholders");
            if (method == null) {
                return List.of();
            }

            Object result = method.invoke(expansion);
            if (result instanceof Collection<?> collection) {
                List<String> placeholders = new ArrayList<>();
                for (Object element : collection) {
                    if (element != null) {
                        placeholders.add(String.valueOf(element));
                    }
                }
                return List.copyOf(placeholders);
            }
        } catch (Throwable ignored) {
            // 没有额外提示也没关系。
        }
        return List.of();
    }

    private Optional<String> readString(final Object target, final String methodName) {
        try {
            Method method = findMethod(target.getClass(), methodName);
            if (method == null) {
                return Optional.empty();
            }
            Object value = method.invoke(target);
            return Optional.ofNullable(value == null ? null : String.valueOf(value));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    private List<String> identifiers() {
        try {
            Object result = getIdentifiersMethod.invoke(localExpansionManager);
            if (result instanceof Collection<?> collection) {
                List<String> identifiers = new ArrayList<>();
                for (Object element : collection) {
                    if (element != null) {
                        identifiers.add(String.valueOf(element));
                    }
                }
                return identifiers;
            }
        } catch (Throwable ignored) {
            // 忽略。
        }
        return List.of();
    }

    private Optional<Object> findExpansion(final String identifier) {
        try {
            Object result = findExpansionByIdentifierMethod.invoke(localExpansionManager, identifier);
            if (result instanceof Optional<?> optional) {
                return optional.map(value -> value);
            }
            return Optional.ofNullable(result);
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    private static Method findMethod(final Class<?> type, final String methodName, final Class<?>... parameterTypes) {
        try {
            Method method = type.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static String runtimeVersion() {
        Package currentPackage = ReflectionPlaceholderApiAdapter.class.getPackage();
        if (currentPackage != null && currentPackage.getImplementationVersion() != null) {
            return currentPackage.getImplementationVersion();
        }
        return "dev";
    }
}
