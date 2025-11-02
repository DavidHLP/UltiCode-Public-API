package com.david.open.dashboard.model;

import java.time.LocalDateTime;

public record RecentSubmission(
        long id,
        String problemSlug,
        String problemTitle,
        String difficulty,
        String language,
        String verdict,
        Integer score,
        String username,
        LocalDateTime submittedAt
) {
}
