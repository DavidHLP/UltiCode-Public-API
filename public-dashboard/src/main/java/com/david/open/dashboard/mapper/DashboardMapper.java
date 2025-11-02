package com.david.open.dashboard.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DashboardMapper {

    @Select("""
            SELECT COUNT(*)
            FROM problems
            WHERE is_public = 1
              AND lifecycle_status = 'published'
            """)
    long countPublishedProblems();

    @Select("""
            SELECT COUNT(*)
            FROM problems
            WHERE is_public = 1
              AND lifecycle_status = 'published'
              AND created_at >= #{start}
              AND created_at < #{end}
            """)
    long countPublishedProblemsInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT COUNT(*)
            FROM submissions
            """)
    long countTotalSubmissions();

    @Select("""
            SELECT COUNT(*)
            FROM submissions
            WHERE created_at >= #{start}
              AND created_at < #{end}
            """)
    long countSubmissionsInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT
                COUNT(*) AS total,
                COALESCE(SUM(CASE WHEN verdict = 'AC' THEN 1 ELSE 0 END), 0) AS accepted
            FROM submissions
            """)
    SubmissionCounts countSubmissionVerdictsAllTime();

    @Select("""
            SELECT
                COUNT(*) AS total,
                COALESCE(SUM(CASE WHEN verdict = 'AC' THEN 1 ELSE 0 END), 0) AS accepted
            FROM submissions
            WHERE created_at >= #{start}
              AND created_at < #{end}
            """)
    SubmissionCounts countSubmissionVerdictsInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Select("""
            SELECT COUNT(DISTINCT user_id)
            FROM submissions
            WHERE created_at >= #{start}
              AND created_at < #{end}
            """)
    long countActiveUsersInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT COUNT(*)
            FROM users
            """)
    long countTotalUsers();

    @Select("""
            SELECT
                DATE(created_at) AS submission_date,
                COUNT(*) AS total,
                COALESCE(SUM(CASE WHEN verdict = 'AC' THEN 1 ELSE 0 END), 0) AS accepted
            FROM submissions
            WHERE created_at >= #{start}
              AND created_at < #{end}
            GROUP BY submission_date
            ORDER BY submission_date
            """)
    List<SubmissionTrendRow> selectSubmissionTrend(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Select("""
            SELECT
                s.id,
                s.created_at,
                s.verdict,
                u.username,
                u.username AS display_name,
                u.email,
                u.avatar_url,
                COALESCE(ps_lang.title, ps_en.title, CONCAT('#', p.slug)) AS problem_title
            FROM submissions s
            JOIN users u ON u.id = s.user_id
            JOIN problems p ON p.id = s.problem_id
            LEFT JOIN problem_statements ps_lang
                ON ps_lang.problem_id = s.problem_id
               AND ps_lang.lang_code = #{lang}
            LEFT JOIN problem_statements ps_en
                ON ps_en.problem_id = s.problem_id
               AND ps_en.lang_code = 'en'
            WHERE p.is_public = 1
              AND s.created_at >= #{start}
              AND s.created_at < #{end}
            ORDER BY s.created_at DESC
            LIMIT #{limit}
            """)
    List<RecentSubmissionRow> selectRecentSubmissions(
            @Param("lang") String lang,
            @Param("limit") int limit,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    record SubmissionCounts(long total, long accepted) {
        public double acceptanceRate() {
            if (total <= 0) {
                return 0D;
            }
            return accepted / (double) total;
        }
    }

    record SubmissionTrendRow(LocalDate date, long total, long accepted) {
    }

    record RecentSubmissionRow(
            long id,
            LocalDateTime createdAt,
            String verdict,
            String username,
            String displayName,
            String email,
            String avatarUrl,
            String problemTitle) {
    }
}
