package com.david.open.dashboard.model;

import java.util.List;

public record DashboardActivityGroup(
        String label,
        List<DashboardActivity> items
) {
}
