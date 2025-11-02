package com.david.open.dashboard.controller;

import com.david.open.dashboard.model.ApiResponse;
import com.david.open.dashboard.model.DashboardActivityGroup;
import com.david.open.dashboard.model.DashboardSummaryResponse;
import com.david.open.dashboard.model.DashboardTrendPoint;
import com.david.open.dashboard.model.RecentSubmission;
import com.david.open.dashboard.model.TrendingProblem;
import com.david.open.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary() {
        return ApiResponse.success(dashboardService.getSummary());
    }

    @GetMapping("/recent-submissions")
    public ApiResponse<List<RecentSubmission>> getRecentSubmissions(
            @RequestParam(name = "limit", defaultValue = "6") int limit
    ) {
        int safeLimit = clamp(limit, 3, 20);
        return ApiResponse.success(dashboardService.getRecentSubmissions(safeLimit));
    }

    @GetMapping("/trending-problems")
    public ApiResponse<List<TrendingProblem>> getTrendingProblems(
            @RequestParam(name = "limit", defaultValue = "6") int limit
    ) {
        int safeLimit = clamp(limit, 3, 12);
        return ApiResponse.success(dashboardService.getTrendingProblems(safeLimit));
    }

    @GetMapping("/submission-trends")
    public ApiResponse<List<DashboardTrendPoint>> getSubmissionTrends() {
        return ApiResponse.success(dashboardService.getSubmissionTrends());
    }

    @GetMapping("/activities")
    public ApiResponse<List<DashboardActivityGroup>> getActivities(
            @RequestParam(name = "size", defaultValue = "9") int size
    ) {
        int safeSize = clamp(size, 4, 24);
        return ApiResponse.success(dashboardService.getActivityTimeline(safeSize));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
