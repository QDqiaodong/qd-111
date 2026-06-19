package com.instrument.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("preparation_checklist")
public class PreparationChecklist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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

    private Integer totalCount;

    private Integer completedCount;

    private Integer requiredCompletedCount;

    private Integer requiredTotalCount;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
