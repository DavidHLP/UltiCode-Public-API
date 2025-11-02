package com.david.open.dashboard.model;

import java.time.Instant;
import java.util.Map;

public record ApiResponse<T>(
        boolean isSuccess,
        T data,
        ApiError error,
        Instant timestamp,
        Map<String, Object> metadata
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return new ApiResponse<>(false, null, error, Instant.now(), null);
    }
}
