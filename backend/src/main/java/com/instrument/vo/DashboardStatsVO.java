package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class DashboardStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private long totalAccessories;
    private long wornCount;
    private long monthReplacements;
    private long groupCount;
}
