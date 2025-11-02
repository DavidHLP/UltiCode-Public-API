package com.david.open.dashboard.model;

public record TrendingProblem(
        long id,
        String slug,
        String title,
        String difficulty,
        long submissionCount,
        long solvedCount,
        Double acceptanceRate,
        String accent
) {
}
