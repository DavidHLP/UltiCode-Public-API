package com.david.open.dashboard.model;

import java.util.List;

public record DashboardSummaryResponse(
        List<DashboardSummaryCard> cards
) {
}
