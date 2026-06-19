package com.instrument.vo;

import com.instrument.entity.PreparationChecklistItem;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PreparationChecklistVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long templateId;

    private String templateName;

    private String typeCode;

    private String typeName;

    private Long accessoryId;

    private String accessoryName;

    private Long replacementRecordId;

    private String operator;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private String status;

    private String statusName;

    private Integer totalCount;

    private Integer completedCount;

    private Integer requiredCompletedCount;

    private Integer requiredTotalCount;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<PreparationChecklistItem> items;
}
