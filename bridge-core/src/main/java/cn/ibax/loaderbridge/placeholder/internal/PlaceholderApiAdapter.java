package cn.ibax.loaderbridge.placeholder.internal;

import cn.ibax.loaderbridge.placeholder.PlaceholderDefinition;
import cn.ibax.loaderbridge.placeholder.PlaceholderRegistrationResult;
import cn.ibax.loaderbridge.placeholder.PlaceholderResolver;

import java.util.List;
import java.util.function.Supplier;

/**
 * PlaceholderAPI 适配器。
 */
public interface PlaceholderApiAdapter {
    List<PlaceholderDefinition> snapshotDefinitions();

    PlaceholderRegistrationResult mirrorNamespace(
            String namespace,
            PlaceholderResolver resolver,
            Supplier<List<String>> placeholdersSupplier
    );
}
