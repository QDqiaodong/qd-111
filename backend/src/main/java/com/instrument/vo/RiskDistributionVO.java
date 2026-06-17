package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class RiskDistributionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String riskLevel;
    private String riskLabel;
    private String riskColor;
    private long count;
    private int percent;
}
