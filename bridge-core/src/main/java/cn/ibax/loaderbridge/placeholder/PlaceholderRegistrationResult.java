package cn.ibax.loaderbridge.placeholder;

/**
 * 占位符注册结果。
 */
public record PlaceholderRegistrationResult(
        PlaceholderRegistrationStatus status,
        PlaceholderDefinition definition,
        String message,
        Throwable error
) {
    public PlaceholderRegistrationResult {
        status = status == null ? PlaceholderRegistrationStatus.INVALID : status;
        message = message == null ? "" : message;
    }

    public boolean registered() {
        return status == PlaceholderRegistrationStatus.REGISTERED;
    }

    public PlaceholderKey key() {
        return definition == null ? null : definition.key();
    }

    public static PlaceholderRegistrationResult registered(final PlaceholderDefinition definition, final String message) {
        return new PlaceholderRegistrationResult(PlaceholderRegistrationStatus.REGISTERED, definition, message, null);
    }

    public static PlaceholderRegistrationResult duplicate(final PlaceholderDefinition definition, final String message) {
        return new PlaceholderRegistrationResult(PlaceholderRegistrationStatus.DUPLICATE, definition, message, null);
    }

    public static PlaceholderRegistrationResult disabled(final String message) {
        return new PlaceholderRegistrationResult(PlaceholderRegistrationStatus.DISABLED, null, message, null);
    }

    public static PlaceholderRegistrationResult invalid(final String message) {
        return new PlaceholderRegistrationResult(PlaceholderRegistrationStatus.INVALID, null, message, null);
    }

    public static PlaceholderRegistrationResult unavailable(final String message, final Throwable error) {
        return new PlaceholderRegistrationResult(PlaceholderRegistrationStatus.UNAVAILABLE, null, message, error);
    }
}
