package com.instrument.dto;

import com.instrument.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PreparationTemplateQueryDTO extends PageQuery {

    private String typeCode;

    private String keyword;

    private Integer enabled;
}
