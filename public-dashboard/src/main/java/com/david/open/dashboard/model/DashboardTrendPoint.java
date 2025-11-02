package com.david.open.dashboard.model;

public record DashboardTrendPoint(
        String label,
        long acceptedCount,
        long wrongCount,
        long pendingCount
) {
}
