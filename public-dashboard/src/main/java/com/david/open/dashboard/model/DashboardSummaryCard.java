package com.david.open.dashboard.model;

public record DashboardSummaryCard(
        String key,
        String title,
        long value,
        String highlightText,
        String helperText,
        String icon,
        String accent
) {
}
