package com.instrument.vo;

import com.instrument.entity.PreparationTemplateItem;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PreparationTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String typeCode;

    private String typeName;

    private String name;

    private String description;

    private Integer enabled;

    private Integer itemCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<PreparationTemplateItem> items;
}
