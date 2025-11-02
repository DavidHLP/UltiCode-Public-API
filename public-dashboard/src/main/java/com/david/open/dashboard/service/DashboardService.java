package com.david.open.dashboard.service;

import com.david.open.dashboard.model.DashboardActivity;
import com.david.open.dashboard.model.DashboardActivityGroup;
import com.david.open.dashboard.model.DashboardSummaryCard;
import com.david.open.dashboard.model.DashboardSummaryResponse;
import com.david.open.dashboard.model.DashboardTrendPoint;
import com.david.open.dashboard.model.RecentSubmission;
import com.david.open.dashboard.model.TrendingProblem;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Map<String, String> DIFFICULTY_ACCENT;
    private static final DateTimeFormatter TREND_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    static {
        Map<String, String> accentMap = new HashMap<>();
        accentMap.put("easy", "orange");
        accentMap.put("medium", "cyan");
        accentMap.put("hard", "purple");
        DIFFICULTY_ACCENT = Collections.unmodifiableMap(accentMap);
    }

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DashboardService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummaryResponse getSummary() {
        long publicProblems = queryForLong(
                "SELECT COUNT(*) FROM problems WHERE is_public = 1",
                MapSqlParameterSourceUtils.empty());

        LocalDateTime problemAnchor = resolveLatestTimestamp("problems", "created_at");
        LocalDateTime submissionAnchor = resolveLatestTimestamp("submissions", "created_at");
        LocalDateTime userAnchor = resolveLatestTimestamp("users", "created_at");
        LocalDateTime commentAnchor = resolveLatestTimestamp("comments", "created_at");

        LocalDateTime problemThreshold = problemAnchor.minusDays(30);
        LocalDateTime submissionThreshold = submissionAnchor.minusDays(30);
        LocalDateTime userThreshold = userAnchor.minusDays(30);
        LocalDateTime commentThreshold = commentAnchor.minusDays(30);

        long newPublicProblems = queryForLong(
                """
                        SELECT COUNT(*)
                        FROM problems
                        WHERE is_public = 1
                          AND created_at >= :threshold
                        """,
                new MapSqlParameterSource("threshold", problemThreshold));

        long submissionCount = queryForLong(
                "SELECT COUNT(*) FROM submissions",
                MapSqlParameterSourceUtils.empty());

        long acceptedSubmissionCount = queryForLong(
                "SELECT COUNT(*) FROM submissions WHERE verdict = 'AC'",
                MapSqlParameterSourceUtils.empty());

        long userCount = queryForLong(
                "SELECT COUNT(*) FROM users",
                MapSqlParameterSourceUtils.empty());

        long activeUsers = queryForLong(
                """
                        SELECT COUNT(DISTINCT user_id)
                        FROM submissions
                        WHERE created_at >= :threshold
                        """,
                new MapSqlParameterSource("threshold", submissionThreshold));

        long commentCount = queryForLong(
                """
                        SELECT COUNT(*)
                        FROM comments
                        WHERE status <> 'hidden'
                        """,
                MapSqlParameterSourceUtils.empty());

        long pendingComments = queryForLong(
                """
                        SELECT COUNT(*)
                        FROM comments
                        WHERE status = 'pending'
                        """,
                MapSqlParameterSourceUtils.empty());

        long newUsers = queryForLong(
                """
                        SELECT COUNT(*)
                        FROM users
                        WHERE created_at >= :threshold
                        """,
                new MapSqlParameterSource("threshold", userThreshold));

        long recentComments = queryForLong(
                """
                        SELECT COUNT(*)
                        FROM comments
                        WHERE created_at >= :threshold
                          AND status <> 'hidden'
                        """,
                new MapSqlParameterSource("threshold", commentThreshold));

        List<DashboardSummaryCard> cards = List.of(
                new DashboardSummaryCard(
                        "problems",
                        "公开题目",
                        publicProblems,
                        formatHighlight(newPublicProblems, "新增"),
                        "最近30天新增",
                        "pi pi-book",
                        "blue"),
                new DashboardSummaryCard(
                        "submissions",
                        "总提交",
                        submissionCount,
                        acceptedSubmissionCount + " 次 AC",
                        "历史通过",
                        "pi pi-send",
                        "orange"),
                new DashboardSummaryCard(
                        "users",
                        "注册用户",
                        userCount,
                        activeUsers + " 人活跃",
                        "最近30天有提交",
                        "pi pi-users",
                        "cyan"),
                new DashboardSummaryCard(
                        "comments",
                        "讨论评论",
                        commentCount,
                        formatHighlight(pendingComments, "待审核"),
                        "最近30天新增 " + recentComments,
                        "pi pi-comments",
                        "purple")
        );

        return new DashboardSummaryResponse(cards);
    }

    public List<RecentSubmission> getRecentSubmissions(int limit) {
        String sql = """
                SELECT s.id,
                       p.slug,
                       COALESCE(ps_zh.title, ps_en.title, CONCAT('Problem #', p.id)) AS title,
                       d.code                           AS difficulty_code,
                       l.display_name                   AS language,
                       s.verdict,
                       s.score,
                       u.username,
                       s.created_at
                FROM submissions s
                JOIN problems p ON p.id = s.problem_id
                LEFT JOIN problem_statements ps_zh
                          ON ps_zh.problem_id = p.id AND ps_zh.locale = 'zh-CN'
                LEFT JOIN problem_statements ps_en
                          ON ps_en.problem_id = p.id AND ps_en.locale = 'en'
                LEFT JOIN difficulties d ON d.id = p.difficulty_id
                JOIN languages l ON l.id = s.language_id
                JOIN users u ON u.id = s.user_id
                ORDER BY s.created_at DESC
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new RecentSubmission(
                rs.getLong("id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("difficulty_code"),
                rs.getString("language"),
                rs.getString("verdict"),
                getScore(rs),
                rs.getString("username"),
                rs.getObject("created_at", LocalDateTime.class)
        ));
    }

    public List<TrendingProblem> getTrendingProblems(int limit) {
        String sql = """
                SELECT p.id,
                       p.slug,
                       COALESCE(ps_zh.title, ps_en.title, CONCAT('Problem #', p.id)) AS title,
                       d.code                                                               AS difficulty_code,
                       COALESCE(vps.submission_count, 0)                                    AS submission_count,
                       COALESCE(vps.solved_count, 0)                                        AS solved_count,
                       vps.acceptance_rate                                                  AS acceptance_rate
                FROM problems p
                LEFT JOIN vw_problem_stats vps ON vps.problem_id = p.id
                LEFT JOIN problem_statements ps_zh
                          ON ps_zh.problem_id = p.id AND ps_zh.locale = 'zh-CN'
                LEFT JOIN problem_statements ps_en
                          ON ps_en.problem_id = p.id AND ps_en.locale = 'en'
                LEFT JOIN difficulties d ON d.id = p.difficulty_id
                WHERE p.is_public = 1
                ORDER BY COALESCE(vps.submission_count, 0) DESC, p.id ASC
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new TrendingProblem(
                rs.getLong("id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("difficulty_code"),
                rs.getLong("submission_count"),
                rs.getLong("solved_count"),
                getNullableDouble(rs, "acceptance_rate"),
                accentForDifficulty(rs.getString("difficulty_code"))
        ));
    }

    public List<DashboardTrendPoint> getSubmissionTrends() {
        String sql = """
                SELECT DATE(s.created_at) AS period,
                       SUM(CASE WHEN s.verdict = 'AC' THEN 1 ELSE 0 END) AS accepted_count,
                       SUM(CASE WHEN s.verdict = 'WA' THEN 1 ELSE 0 END) AS wrong_count,
                       SUM(CASE WHEN s.verdict NOT IN ('AC', 'WA') THEN 1 ELSE 0 END) AS pending_count
                FROM submissions s
                GROUP BY DATE(s.created_at)
                ORDER BY period DESC
                LIMIT 8
                """;

        List<TrendRow> rows = jdbcTemplate.query(sql, MapSqlParameterSourceUtils.empty(), this::mapTrendRow);
        LocalDate referenceDate = rows.stream()
                .map(TrendRow::period)
                .max(Comparator.naturalOrder())
                .orElseGet(() -> resolveLatestTimestamp("submissions", "created_at").toLocalDate());

        if (rows.isEmpty()) {
            referenceDate = resolveLatestTimestamp("submissions", "created_at").toLocalDate();
        }

        LinkedHashMap<LocalDate, TrendRow> rowMap = new LinkedHashMap<>();
        for (TrendRow row : rows) {
            rowMap.putIfAbsent(row.period(), row);
        }

        // Ensure at least four periods for the chart.
        for (int i = 0; i < 4; i++) {
            LocalDate day = referenceDate.minusDays(3 - i);
            rowMap.putIfAbsent(day, new TrendRow(day, 0, 0, 0));
        }

        return rowMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DashboardTrendPoint(
                        TREND_LABEL_FORMATTER.format(entry.getKey()),
                        entry.getValue().accepted(),
                        entry.getValue().wrong(),
                        entry.getValue().pending()))
                .collect(Collectors.toList());
    }

    public List<DashboardActivityGroup> getActivityTimeline(int size) {
        List<DashboardActivity> events = new ArrayList<>();
        events.addAll(fetchRecentSubmissionsForActivity(Math.max(2, size / 3)));
        events.addAll(fetchRecentCommentsForActivity(Math.max(2, size / 3)));
        events.addAll(fetchUpcomingContestsForActivity(2));
        events.addAll(fetchSecurityAuditsForActivity(2));

        events.sort(Comparator.comparing(DashboardActivity::occurredAt).reversed());

        if (events.isEmpty()) {
            return List.of();
        }

        LocalDate referenceDate = events.stream()
                .map(activity -> activity.occurredAt().toLocalDate())
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        Map<String, List<DashboardActivity>> grouped = new LinkedHashMap<>();
        for (DashboardActivity activity : events) {
            LocalDate eventDate = activity.occurredAt().toLocalDate();
            long daysDiff = ChronoUnit.DAYS.between(eventDate, referenceDate);
            String bucket;
            if (daysDiff == 0) {
                bucket = "今天";
            } else if (daysDiff == 1) {
                bucket = "昨天";
            } else if (daysDiff <= 7) {
                bucket = "最近一周";
            } else {
                bucket = "更早";
            }
            grouped.computeIfAbsent(bucket, key -> new ArrayList<>()).add(activity);
        }

        return grouped.entrySet()
                .stream()
                .map(entry -> new DashboardActivityGroup(
                        entry.getKey(),
                        entry.getValue()
                                .stream()
                                .sorted(Comparator.comparing(DashboardActivity::occurredAt).reversed())
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    private List<DashboardActivity> fetchRecentSubmissionsForActivity(int limit) {
        String sql = """
                SELECT s.verdict,
                       s.created_at,
                       u.username,
                       p.slug,
                       COALESCE(ps_zh.title, ps_en.title, CONCAT('Problem #', p.id)) AS title
                FROM submissions s
                JOIN users u ON u.id = s.user_id
                JOIN problems p ON p.id = s.problem_id
                LEFT JOIN problem_statements ps_zh
                          ON ps_zh.problem_id = p.id AND ps_zh.locale = 'zh-CN'
                LEFT JOIN problem_statements ps_en
                          ON ps_en.problem_id = p.id AND ps_en.locale = 'en'
                ORDER BY s.created_at DESC
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String verdict = rs.getString("verdict");
            String username = rs.getString("username");
            String title = rs.getString("title");
            LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
            boolean accepted = Objects.equals("AC", verdict);
            String icon = accepted ? "pi pi-check-circle" : "pi pi-exclamation-triangle";
            String accent = accepted ? "green" : "pink";
            String message = accepted
                    ? String.format(Locale.CHINA, "%s 通过了「%s」", username, title)
                    : String.format(Locale.CHINA, "%s 提交「%s」判定为 %s", username, title, verdict);
            return new DashboardActivity(message, icon, accent, createdAt);
        });
    }

    private List<DashboardActivity> fetchRecentCommentsForActivity(int limit) {
        String sql = """
                SELECT c.id,
                       c.status,
                       c.created_at,
                       c.entity_type,
                       c.entity_id,
                       u.username,
                       COALESCE(ps_zh.title, ps_en.title, CONCAT('Problem #', c.entity_id)) AS title
                FROM comments c
                JOIN users u ON u.id = c.user_id
                LEFT JOIN problems p ON c.entity_type = 'problem' AND c.entity_id = p.id
                LEFT JOIN problem_statements ps_zh
                          ON ps_zh.problem_id = p.id AND ps_zh.locale = 'zh-CN'
                LEFT JOIN problem_statements ps_en
                          ON ps_en.problem_id = p.id AND ps_en.locale = 'en'
                ORDER BY c.created_at DESC
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            long id = rs.getLong("id");
            String status = rs.getString("status");
            LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
            String username = rs.getString("username");
            String title = rs.getString("title");
            boolean pending = Objects.equals("pending", status);
            String icon = pending ? "pi pi-inbox" : "pi pi-comments";
            String accent = pending ? "orange" : "blue";
            String message = pending
                    ? String.format(Locale.CHINA, "评论 #%d 待审核：%s 在「%s」的回复", id, username, title)
                    : String.format(Locale.CHINA, "%s 在「%s」发布了评论", username, title);
            return new DashboardActivity(message, icon, accent, createdAt);
        });
    }

    private List<DashboardActivity> fetchUpcomingContestsForActivity(int limit) {
        String sql = """
                SELECT title,
                       start_time
                FROM contests
                WHERE start_time >= :threshold
                ORDER BY start_time ASC
                LIMIT :limit
                """;
        LocalDateTime nowAnchor = resolveLatestTimestamp("contests", "start_time");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("threshold", nowAnchor.minusDays(1));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String title = rs.getString("title");
            LocalDateTime startTime = rs.getObject("start_time", LocalDateTime.class);
            String message = String.format(
                    Locale.CHINA,
                    "比赛「%s」即将在 %s 开赛",
                    title,
                    formatter.format(startTime));
            return new DashboardActivity(message, "pi pi-calendar", "teal", startTime);
        });
    }

    private List<DashboardActivity> fetchSecurityAuditsForActivity(int limit) {
        String sql = """
                SELECT actor_username,
                       action,
                       object_type,
                       object_id,
                       created_at
                FROM security_audit_logs
                ORDER BY created_at DESC
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String actor = rs.getString("actor_username");
            String action = rs.getString("action");
            String objectType = rs.getString("object_type");
            String objectId = rs.getString("object_id");
            LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
            String message = String.format(Locale.CHINA, "%s 调整了 %s(%s)：%s",
                    actor, objectType, objectId, action);
            return new DashboardActivity(message, "pi pi-shield", "cyan", createdAt);
        });
    }

    private long queryForLong(String sql, MapSqlParameterSource params) {
        Long value;
        try {
            value = jdbcTemplate.queryForObject(sql, params, Long.class);
        } catch (EmptyResultDataAccessException ex) {
            value = null;
        }
        return value == null ? 0L : value;
    }

    private LocalDateTime resolveLatestTimestamp(String table, String column) {
        String sql = "SELECT MAX(" + column + ") FROM " + table;
        LocalDateTime value;
        try {
            value = jdbcTemplate.queryForObject(sql, MapSqlParameterSourceUtils.empty(), LocalDateTime.class);
        } catch (EmptyResultDataAccessException ex) {
            value = null;
        }
        return value != null ? value : LocalDateTime.now();
    }

    private Integer getScore(ResultSet rs) throws SQLException {
        int score = rs.getInt("score");
        if (rs.wasNull()) {
            return null;
        }
        return score;
    }

    private Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }

    private String accentForDifficulty(String difficulty) {
        if (difficulty == null) {
            return "blue";
        }
        return DIFFICULTY_ACCENT.getOrDefault(difficulty.toLowerCase(Locale.ROOT), "blue");
    }

    private TrendRow mapTrendRow(ResultSet rs, int rowNum) throws SQLException {
        LocalDate period = rs.getObject("period", LocalDate.class);
        return new TrendRow(
                period,
                rs.getLong("accepted_count"),
                rs.getLong("wrong_count"),
                rs.getLong("pending_count"));
    }

    private String formatHighlight(long value, String unit) {
        return value + " " + unit;
    }

    private record TrendRow(LocalDate period, long accepted, long wrong, long pending) {
    }

    private static class MapSqlParameterSourceUtils {
        private static MapSqlParameterSource empty() {
            return new MapSqlParameterSource();
        }
    }
}
