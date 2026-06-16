package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class RiskTiersVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<RiskTierItemVO> expired;
    private List<RiskTierItemVO> broken;
    private List<RiskTierItemVO> severe;
    private List<RiskTierItemVO> upcoming;
}
