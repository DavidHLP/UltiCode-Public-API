package com.david.open.problem.controller;

import com.david.core.http.ApiResponse;
import com.david.open.problem.dto.ProblemListResponse;
import com.david.open.problem.service.ProblemQueryService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/public/problems")
public class ProblemPublicController {

    private final ProblemQueryService problemQueryService;

    public ProblemPublicController(ProblemQueryService problemQueryService) {
        this.problemQueryService = problemQueryService;
    }

    @GetMapping
    public ApiResponse<ProblemListResponse> listProblems(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") int page,
            @RequestParam(defaultValue = "12")
                    @Min(value = 1, message = "分页大小不能小于1")
                    @Max(value = 50, message = "分页大小不能超过50")
                    int size,
            @RequestParam(name = "lang", required = false) String langCode) {
        ProblemListResponse response = problemQueryService.listProblems(page, size, langCode);
        return ApiResponse.success(response);
    }
}
