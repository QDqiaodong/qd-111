package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ReplacementTimelineVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long accessoryId;

    private String accessoryName;

    private String specification;

    private String instrumentName;

    private String imageUrl;

    private Integer standardCycle;

    private Integer recordCount;

    private Integer avgUsageDays;

    private List<ReplacementTimelineItemVO> items;
}
