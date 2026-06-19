package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class WornStatusUsageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String statusCode;

    private String statusLabel;

    private Boolean canDelete;

    private Long usedCount;

    private List<String> typicalAccessoryNames;

    public WornStatusUsageVO() {}

    public WornStatusUsageVO(String statusCode, String statusLabel, boolean canDelete, Long usedCount, List<String> typicalAccessoryNames) {
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.canDelete = canDelete;
        this.usedCount = usedCount;
        this.typicalAccessoryNames = typicalAccessoryNames;
    }
}
