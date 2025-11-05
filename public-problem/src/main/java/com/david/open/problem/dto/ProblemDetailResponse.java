package com.david.open.problem.dto;

import com.david.open.problem.dto.ProblemCardView.DifficultyInfo;
import com.david.open.problem.dto.ProblemCardView.ProblemMetadata;
import com.david.open.problem.dto.ProblemCardView.ProblemStats;
import com.david.open.problem.dto.ProblemCardView.TagInfo;
import java.time.LocalDateTime;
import java.util.List;

public record ProblemDetailResponse(
        Long id,
        String slug,
        String title,
        String descriptionMd,
        String constraintsMd,
        String examplesMd,
        DifficultyInfo difficulty,
        ProblemStats stats,
        ProblemMetadata metadata,
        List<TagInfo> tags,
        List<LanguageConfig> languages,
        LocalDateTime updatedAt) {

    public record LanguageConfig(
            Integer languageId,
            String languageCode,
            String languageName,
            String functionName,
            String starterCode) {}
}
