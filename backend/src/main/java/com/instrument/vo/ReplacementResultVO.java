package com.instrument.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ReplacementResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long recordId;

    private Integer usageDays;

    private Integer standardCycle;

    private Integer deviationDays;

    private Double deviationRatio;

    private String deviationLabel;

    private Boolean statusUpdated;

    private String previousStatus;

    private String currentStatus;

    private String statusMessage;
}
