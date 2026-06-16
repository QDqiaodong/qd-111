package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class CycleReferenceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer standardCycle;

    private String standardCycleLabel;

    private Integer lastInterval;

    private LocalDate lastReplaceDate;

    private Integer averageInterval;

    private Integer historyCount;

    private String suggestion;

    private String suggestionLevel;

    private Integer currentInputCycle;

    private Integer diffFromStandard;

    private Integer diffFromLast;

    private String diffFromStandardLabel;

    private String diffFromLastLabel;
}
