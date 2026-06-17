package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class StandardCycleRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String typeCode;

    private String typeName;

    private String instrument;

    private String instrumentName;

    private String specPattern;

    private String specDescription;

    private Integer standardCycle;

    private String standardCycleLabel;

    private Integer priority;

    private Integer enabled;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
