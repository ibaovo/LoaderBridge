package cn.ibax.loaderbridge.bridge;

import java.util.Optional;
import java.util.function.Function;

/**
 * 桥接调用结果。
 */
public record BridgeResult<T>(boolean success, T value, String message, Throwable error) {
    public static <T> BridgeResult<T> success(final T value) {
        return new BridgeResult<>(true, value, "成功", null);
    }

    public static <T> BridgeResult<T> success(final T value, final String message) {
        return new BridgeResult<>(true, value, message, null);
    }

    public static <T> BridgeResult<T> failure(final String message) {
        return new BridgeResult<>(false, null, message, null);
    }

    public static <T> BridgeResult<T> failure(final String message, final Throwable error) {
        return new BridgeResult<>(false, null, message, error);
    }

    public boolean failed() {
        return !success;
    }

    public Optional<T> asOptional() {
        return success ? Optional.ofNullable(value) : Optional.empty();
    }

    public <U> BridgeResult<U> map(final Function<? super T, ? extends U> mapper) {
        if (!success) {
            return BridgeResult.failure(message, error);
        }
        return BridgeResult.success(mapper.apply(value), message);
    }
}
