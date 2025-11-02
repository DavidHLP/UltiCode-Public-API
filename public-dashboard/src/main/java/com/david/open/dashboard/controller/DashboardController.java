package com.david.open.dashboard.controller;

import com.david.core.http.ApiResponse;
import com.david.open.dashboard.model.DashboardOverviewResponse;
import com.david.open.dashboard.service.DashboardService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> getOverview(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(value = "lang", required = false, defaultValue = "zh-CN")
            String lang,
            @RequestParam(value = "recentLimit", defaultValue = "6")
            int recentLimit) {

        DashboardOverviewResponse response =
                dashboardService.getOverview(startDate, endDate, lang, recentLimit);
        return ApiResponse.success(response);
    }
}
