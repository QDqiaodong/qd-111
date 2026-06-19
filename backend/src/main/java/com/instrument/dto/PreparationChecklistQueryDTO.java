package com.instrument.dto;

import com.instrument.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PreparationChecklistQueryDTO extends PageQuery {

    private String typeCode;

    private Long accessoryId;

    private Long replacementRecordId;

    private String status;

    private String keyword;
}
