package com.instrument.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class AccessoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "配件名称不能为空")
    private String name;

    @NotBlank(message = "配件类型不能为空")
    private String typeCode;

    @NotBlank(message = "规格描述不能为空")
    private String specification;

    @NotBlank(message = "适配乐器不能为空")
    private String instrument;

    @NotNull(message = "所属分组不能为空")
    private Long groupId;

    private String brandModel;

    @NotNull(message = "标准更换周期不能为空")
    @Min(value = 1, message = "标准更换周期最小为1天")
    private Integer standardCycle;

    @NotNull(message = "购入时间不能为空")
    private LocalDate purchaseDate;

    @NotBlank(message = "损耗状态不能为空")
    private String wornStatus;

    private String imageUrl;

    private String remark;
}
