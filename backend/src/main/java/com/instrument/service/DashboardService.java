package com.instrument.service;

import com.instrument.vo.*;

import java.util.List;

public interface DashboardService {

    DashboardStatsVO stats();

    List<UpcomingReplacementVO> upcomingReplacements();

    List<WornDistributionVO> wornDistribution();

    List<GroupDistributionVO> groupDistribution();

    WornHeatmapVO wornHeatmap();
}
