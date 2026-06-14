package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class WornDistributionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status;
    private String label;
    private long count;
    private int percent;
    private String color;
}
