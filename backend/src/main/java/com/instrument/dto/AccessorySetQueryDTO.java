package com.instrument.dto;

import com.instrument.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AccessorySetQueryDTO extends PageQuery {

    private String keyword;

    private String instrument;

    private String status;
}
