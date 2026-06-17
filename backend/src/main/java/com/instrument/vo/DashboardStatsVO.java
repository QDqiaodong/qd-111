package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class DashboardStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private long totalAccessories;
    private long wornCount;
    private long monthReplacements;
    private long groupCount;

    private long extremeRiskCount;
    private long criticalRiskCount;
    private long highRiskCount;
    private long mediumRiskCount;
    private long lowRiskCount;
    private List<RiskDistributionVO> riskDistribution;
}
