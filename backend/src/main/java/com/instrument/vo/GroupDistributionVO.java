package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class GroupDistributionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private long count;
    private int percent;
}
