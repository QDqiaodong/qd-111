package com.instrument.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("accessory_set_item")
public class AccessorySetItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long setId;

    private Long accessoryId;

    private String accessoryName;

    private String typeCode;

    private String typeName;

    private String specification;

    private String instrument;

    private String instrumentName;

    private Long groupId;

    private String groupName;

    private Integer quantity;

    private Integer sortOrder;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
