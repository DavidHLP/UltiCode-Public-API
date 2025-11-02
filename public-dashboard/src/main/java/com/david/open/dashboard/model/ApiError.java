package com.david.open.dashboard.model;

import java.util.Map;

public record ApiError(
        int status,
        String code,
        String message,
        Map<String, Object> details
) {
}
