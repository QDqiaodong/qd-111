package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class MaintenancePlanItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long accessoryId;
    private String accessoryName;
    private String typeCode;
    private String typeName;
    private String specification;
    private String imageUrl;

    private Integer standardCycle;
    private Integer usageDays;
    private Integer cyclePercent;

    private LocalDate lastActionDate;
    private LocalDate expectedNextDate;
    private Integer daysLeft;

    private String wornStatus;
    private String wornStatusLabel;

    private String riskLevel;
    private String riskLabel;
    private String riskColor;
    private Integer riskScore;

    private String planStatus;
    private String planStatusLabel;
    private String planStatusColor;

    private String actionType;
    private String actionSuggestion;
}
