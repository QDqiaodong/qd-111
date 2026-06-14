package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class UpcomingReplacementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long accessoryId;
    private String name;
    private String specification;
    private String instrument;
    private String instrumentName;
    private String lastReplaceDate;
    private Integer usageDays;
    private Integer daysLeft;
}
