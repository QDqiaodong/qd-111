package com.instrument.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class CycleRuleQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String typeCode;

    private String instrument;

    private String specification;

    private Integer enabled;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
