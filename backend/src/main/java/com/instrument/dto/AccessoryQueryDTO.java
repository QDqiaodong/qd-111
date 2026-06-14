package com.instrument.dto;

import com.instrument.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AccessoryQueryDTO extends PageQuery {

    private String keyword;

    private Long groupId;

    private String typeCode;

    private String wornStatus;

    private String instrument;
}
