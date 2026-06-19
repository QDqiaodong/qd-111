package com.instrument.controller;

import com.instrument.common.Result;
import com.instrument.service.DashboardService;
import com.instrument.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.success(dashboardService.stats());
    }

    @GetMapping("/upcoming-replacements")
    public Result<List<UpcomingReplacementVO>> upcomingReplacements() {
        return Result.success(dashboardService.upcomingReplacements());
    }

    @GetMapping("/worn-distribution")
    public Result<List<WornDistributionVO>> wornDistribution() {
        return Result.success(dashboardService.wornDistribution());
    }

    @GetMapping("/group-distribution")
    public Result<List<GroupDistributionVO>> groupDistribution() {
        return Result.success(dashboardService.groupDistribution());
    }

    @GetMapping("/risk-tiers")
    public Result<RiskTiersVO> riskTiers() {
        return Result.success(dashboardService.riskTiers());
    }

    @GetMapping("/risk-tier/{tier}")
    public Result<List<RiskTierItemVO>> riskTier(@PathVariable String tier) {
        return Result.success(dashboardService.riskTier(tier));
    }

    @GetMapping("/worn-heatmap")
    public Result<WornHeatmapVO> wornHeatmap() {
        return Result.success(dashboardService.wornHeatmap());
    }

    @GetMapping("/risk-distribution")
    public Result<List<RiskDistributionVO>> riskDistribution() {
        return Result.success(dashboardService.riskDistribution());
    }

    @GetMapping("/annual-stats")
    public Result<List<AnnualAccessoryStatsVO>> annualStats() {
        return Result.success(dashboardService.annualStats());
    }
}
