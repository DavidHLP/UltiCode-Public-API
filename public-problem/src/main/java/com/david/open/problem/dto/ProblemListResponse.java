package com.david.open.problem.dto;

import java.util.List;

public record ProblemListResponse(
        long total, int page, int size, boolean hasMore, List<ProblemCardView> items) {

    public ProblemListResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
