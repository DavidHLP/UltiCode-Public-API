package com.david.open.dashboard.model;

import java.util.List;

/**
 * 仪表盘概览接口返回结构。
 *
 * @param summaryCards       统计卡片数据
 * @param submissionTrend    提交趋势序列
 * @param recentSubmissions  最近提交列表
 */
public record DashboardOverviewResponse(
        List<SummaryCard> summaryCards,
        List<SubmissionTrendPoint> submissionTrend,
        List<RecentSubmission> recentSubmissions) {
}
