package com.david.open.problem.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProblemCardView(
        Long id,
        String slug,
        String title,
        DifficultyInfo difficulty,
        List<TagInfo> tags,
        ProblemStats stats,
        ProblemMetadata metadata,
        LocalDateTime updatedAt) {

    public ProblemCardView {
        tags = tags == null ? List.of() : List.copyOf(tags);
        metadata = metadata == null ? ProblemMetadata.empty() : metadata;
    }

    public record DifficultyInfo(Integer id, String code, String label) {}

    public record TagInfo(Long id, String name, String slug) {}

    public record ProblemStats(Integer timeLimitMs, Integer memoryLimitKb) {}

    public record ProblemMetadata(
            List<String> companies,
            Double frequency,
            Boolean paidOnly,
            Integer frontendId,
            Boolean leetcodeStyle) {

        public ProblemMetadata {
            companies = companies == null ? List.of() : List.copyOf(companies);
        }

        public static ProblemMetadata empty() {
            return new ProblemMetadata(List.of(), null, null, null, null);
        }
    }
}
