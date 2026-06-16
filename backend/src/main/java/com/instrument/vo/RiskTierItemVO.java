package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class RiskTierItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long accessoryId;
    private String name;
    private String typeCode;
    private String typeName;
    private String specification;
    private String instrument;
    private String instrumentName;
    private String wornStatus;
    private Integer usageDays;
    private Integer daysLeft;
}
