package com.david.open.dashboard.service;

import com.david.open.dashboard.mapper.DashboardMapper;
import com.david.open.dashboard.model.DashboardOverviewResponse;
import com.david.open.dashboard.model.RecentSubmission;
import com.david.open.dashboard.model.SubmissionTrendPoint;
import com.david.open.dashboard.model.SummaryCard;
import com.david.open.dashboard.support.DateRange;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final int MAX_RANGE_DAYS = 365;
    private static final int MIN_RECENT_LIMIT = 1;
    private static final int MAX_RECENT_LIMIT = 20;
    private static final Locale LOCALE = Locale.CHINA;

    private final DashboardMapper dashboardMapper;

    public DashboardService(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    public DashboardOverviewResponse getOverview(
            LocalDate startDate,
            LocalDate endDate,
            String lang,
            int recentLimit) {
        DateRange range = resolveRange(startDate, endDate);
        int safeLimit = Math.max(MIN_RECENT_LIMIT, Math.min(MAX_RECENT_LIMIT, recentLimit));
        String resolvedLang = (lang == null || lang.isBlank()) ? "zh-CN" : lang;

        long totalProblems = dashboardMapper.countPublishedProblems();
        long newProblemsInRange =
                dashboardMapper.countPublishedProblemsInRange(
                        range.startDateTime(), range.endExclusiveDateTime());

        long totalSubmissions = dashboardMapper.countTotalSubmissions();
        long submissionsInRange =
                dashboardMapper.countSubmissionsInRange(
                        range.startDateTime(), range.endExclusiveDateTime());

        DashboardMapper.SubmissionCounts allTimeCounts =
                dashboardMapper.countSubmissionVerdictsAllTime();
        DashboardMapper.SubmissionCounts rangeCounts =
                dashboardMapper.countSubmissionVerdictsInRange(
                        range.startDateTime(), range.endExclusiveDateTime());

        long activeUsersInRange =
                dashboardMapper.countActiveUsersInRange(
                        range.startDateTime(), range.endExclusiveDateTime());
        long totalUsers = dashboardMapper.countTotalUsers();

        List<SubmissionTrendPoint> trendPoints = buildTrendPoints(range,
                dashboardMapper.selectSubmissionTrend(
                        range.startDateTime(), range.endExclusiveDateTime()));

        List<RecentSubmission> recentSubmissions = dashboardMapper
                .selectRecentSubmissions(
                        resolvedLang,
                        safeLimit,
                        range.startDateTime(),
                        range.endExclusiveDateTime())
                .stream()
                .map(this::mapRecentSubmission)
                .toList();

        List<SummaryCard> summaryCards = List.of(
                new SummaryCard(
                        "totalProblems",
                        "公开题目",
                        formatNumber(totalProblems),
                        "区间内新增 " + formatNumber(newProblemsInRange) + " 道"),
                new SummaryCard(
                        "submissionsInRange",
                        "区间提交数",
                        formatNumber(submissionsInRange),
                        "累计提交 " + formatNumber(totalSubmissions) + " 次"),
                new SummaryCard(
                        "acceptanceRate",
                        "全站通过率",
                        formatAcceptance(allTimeCounts),
                        "区间通过 " + formatNumber(rangeCounts.accepted()) + " 次"),
                new SummaryCard(
                        "activeUsers",
                        "活跃用户",
                        formatNumber(activeUsersInRange),
                        "累计注册 " + formatNumber(totalUsers) + " 人")
        );

        return new DashboardOverviewResponse(summaryCards, trendPoints, recentSubmissions);
    }

    private DateRange resolveRange(LocalDate startDate, LocalDate endDate) {
        LocalDate end = Objects.requireNonNullElse(endDate, LocalDate.now());
        LocalDate start = Objects.requireNonNullElse(startDate, end.minusDays(DEFAULT_RANGE_DAYS - 1));

        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        long span = ChronoUnit.DAYS.between(start, end) + 1;
        if (span > MAX_RANGE_DAYS) {
            start = end.minusDays(MAX_RANGE_DAYS - 1);
        }

        return new DateRange(start, end);
    }

    private List<SubmissionTrendPoint> buildTrendPoints(
            DateRange range, List<DashboardMapper.SubmissionTrendRow> rows) {

        Map<LocalDate, DashboardMapper.SubmissionTrendRow> mapped = rows.stream()
                .collect(Collectors.toMap(
                        DashboardMapper.SubmissionTrendRow::date,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<SubmissionTrendPoint> result = new ArrayList<>();
        for (LocalDate cursor = range.start(); !cursor.isAfter(range.end()); cursor = cursor.plusDays(1)) {
            DashboardMapper.SubmissionTrendRow row = mapped.get(cursor);
            long total = row != null ? row.total() : 0L;
            long accepted = row != null ? row.accepted() : 0L;
            result.add(new SubmissionTrendPoint(cursor, total, accepted));
        }
        return result;
    }

    private RecentSubmission mapRecentSubmission(DashboardMapper.RecentSubmissionRow row) {
        return new RecentSubmission(
                row.id(),
                row.username(),
                row.displayName(),
                row.email(),
                row.avatarUrl(),
                row.problemTitle(),
                row.verdict(),
                row.createdAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }

    private String formatNumber(long value) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(LOCALE);
        return formatter.format(value);
    }

    private String formatAcceptance(DashboardMapper.SubmissionCounts counts) {
        if (counts.total() <= 0) {
            return "--";
        }
        NumberFormat formatter = NumberFormat.getPercentInstance(LOCALE);
        formatter.setMinimumFractionDigits(1);
        formatter.setMaximumFractionDigits(1);
        return formatter.format(counts.acceptanceRate());
    }
}
