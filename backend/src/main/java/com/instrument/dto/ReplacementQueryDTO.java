package com.instrument.dto;

import com.instrument.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReplacementQueryDTO extends PageQuery {

    private String keyword;

    private Long accessoryId;

    private LocalDate startDate;

    private LocalDate endDate;
}
