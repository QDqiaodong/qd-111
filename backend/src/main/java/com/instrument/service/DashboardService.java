package com.instrument.service;

import com.instrument.vo.*;

import java.util.List;

public interface DashboardService {

    DashboardStatsVO stats();

    List<UpcomingReplacementVO> upcomingReplacements();

    List<WornDistributionVO> wornDistribution();

    List<GroupDistributionVO> groupDistribution();

    RiskTiersVO riskTiers();

    List<RiskTierItemVO> riskTier(String tier);

    WornHeatmapVO wornHeatmap();
}
