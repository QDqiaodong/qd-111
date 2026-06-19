package com.instrument.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("preparation_checklist_item")
public class PreparationChecklistItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long checklistId;

    private Long templateItemId;

    private String category;

    private String categoryName;

    private String name;

    private String description;

    private Integer required;

    private Integer sortOrder;

    private Integer completed;

    private LocalDateTime completedTime;

    private String completedBy;

    private String completionNote;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
