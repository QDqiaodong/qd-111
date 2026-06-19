package com.instrument.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PreparationTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "配件类型编码不能为空")
    @Size(max = 50, message = "配件类型编码不能超过50个字符")
    private String typeCode;

    @Size(max = 50, message = "配件类型名称不能超过50个字符")
    private String typeName;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称不能超过100个字符")
    private String name;

    @Size(max = 500, message = "模板描述不能超过500个字符")
    private String description;

    private Integer enabled;

    @NotEmpty(message = "模板至少包含一个准备项")
    @Valid
    private List<PreparationTemplateItemDTO> items;
}
