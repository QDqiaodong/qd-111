package com.instrument.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("accessory")
public class Accessory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String typeCode;

    private String typeName;

    private String specification;

    private String instrument;

    private String instrumentName;

    private Long groupId;

    private String groupName;

    private String brandModel;

    private Integer standardCycle;

    private LocalDate purchaseDate;

    private String wornStatus;

    private String imageUrl;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(exist = false)
    private Integer usageDays;
}
