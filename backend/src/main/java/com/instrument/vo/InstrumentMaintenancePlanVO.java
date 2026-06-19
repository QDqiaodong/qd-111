package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class InstrumentMaintenancePlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String instrument;
    private String instrumentName;

    private Integer totalCount;
    private Integer urgentCount;
    private Integer attentionCount;
    private Integer normalCount;

    private Integer minDaysLeft;
    private Integer maxRiskScore;

    private String overallStatus;
    private String overallStatusLabel;
    private String overallStatusColor;

    private List<MaintenancePlanItemVO> items;
}
