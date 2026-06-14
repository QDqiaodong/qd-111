package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class AccessoryLifecycleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long accessoryId;
    private String name;
    private LocalDate purchaseDate;
    private Integer standardCycle;
    private Integer usedDays;
    private Integer daysLeft;
    private Integer cyclePercent;
    private LocalDate lastReplaceDate;
    private String wornStatus;
    private String stage;
    private String stageLabel;
}
