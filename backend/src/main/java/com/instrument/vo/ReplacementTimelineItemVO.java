package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class ReplacementTimelineItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private LocalDate replaceDate;

    private Integer usageDays;

    private Integer standardCycle;

    private String operator;

    private String remark;

    private Integer intervalDays;

    private String intervalLabel;

    private Boolean isFirst;
}
