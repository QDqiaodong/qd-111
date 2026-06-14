package com.instrument.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class ReplacementDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull(message = "请选择配件")
    private Long accessoryId;

    @NotNull(message = "更换日期不能为空")
    private LocalDate replaceDate;

    private String operator;

    private String remark;
}
