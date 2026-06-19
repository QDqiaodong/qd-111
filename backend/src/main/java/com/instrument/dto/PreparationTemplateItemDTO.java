package com.instrument.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;

@Data
public class PreparationTemplateItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "准备项分类不能为空")
    @Size(max = 50, message = "准备项分类不能超过50个字符")
    private String category;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过50个字符")
    private String categoryName;

    @NotBlank(message = "准备项名称不能为空")
    @Size(max = 100, message = "准备项名称不能超过100个字符")
    private String name;

    @Size(max = 500, message = "准备项说明不能超过500个字符")
    private String description;

    private Integer required;

    private Integer sortOrder;
}
