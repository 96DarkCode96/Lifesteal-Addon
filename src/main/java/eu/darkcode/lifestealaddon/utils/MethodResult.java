package eu.darkcode.lifestealaddon.utils;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public final class MethodResult {

    public static MethodResult success() {
        return success(null);
    }

    public static MethodResult success(@Nullable Object result) {
        return new MethodResult(true, result, null);
    }

    public static MethodResult error() {
        return error(null);
    }

    public static MethodResult error(@Nullable Throwable error) {
        return new MethodResult(false, null, error);
    }

    private final boolean success;
    private final @Nullable Object result;
    private final @Nullable Throwable error;

    private MethodResult(boolean success, @Nullable Object result, @Nullable Throwable error) {
        this.success = success;
        this.result = result;
        this.error = error;
    }

    public boolean hasError() {
        return error != null;
    }

}