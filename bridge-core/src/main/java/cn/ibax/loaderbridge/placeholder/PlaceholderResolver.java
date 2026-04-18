package cn.ibax.loaderbridge.placeholder;

/**
 * 占位符解析器。
 */
@FunctionalInterface
public interface PlaceholderResolver {
    String resolve(PlaceholderContext context);
}
