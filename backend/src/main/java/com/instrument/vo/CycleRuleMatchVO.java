package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class CycleRuleMatchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer matchedCycle;

    private String matchedCycleLabel;

    private String typeName;

    private String instrumentName;

    private String specDescription;

    private String remark;

    private boolean fromManualOverride;

    private List<StandardCycleRuleVO> candidateRules;

    private String suggestion;
}
