package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class GroupCapacityStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long groupId;
    private String groupName;
    private Integer sortOrder;
    private Integer totalCount;
    private Integer severeCount;
    private Integer brokenCount;
    private Integer maxUnreplacedDays;
    private LocalDate lastReplacementDate;
}
