package com.instrument.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class PreparationChecklistGenerateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "配件ID不能为空")
    private Long accessoryId;

    private Long replacementRecordId;

    private String operator;

    private String remark;
}
