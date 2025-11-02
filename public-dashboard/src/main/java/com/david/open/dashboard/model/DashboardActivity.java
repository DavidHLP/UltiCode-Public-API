package com.david.open.dashboard.model;

import java.time.LocalDateTime;

public record DashboardActivity(
        String message,
        String icon,
        String accent,
        LocalDateTime occurredAt
) {
}
