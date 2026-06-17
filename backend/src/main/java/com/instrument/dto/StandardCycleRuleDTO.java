package com.instrument.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class StandardCycleRuleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "配件类型不能为空")
    private String typeCode;

    private String instrument;

    private String specPattern;

    private String specDescription;

    @NotNull(message = "标准更换周期不能为空")
    @Min(value = 1, message = "标准更换周期最小为1天")
    private Integer standardCycle;

    private Integer priority;

    private Integer enabled;

    private String remark;
}
