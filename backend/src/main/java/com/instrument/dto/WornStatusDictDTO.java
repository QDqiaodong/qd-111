package com.instrument.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

@Data
public class WornStatusDictDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "状态编码不能为空")
    private String statusCode;

    @NotBlank(message = "状态名称不能为空")
    private String statusLabel;

    private String color;

    private Integer sortOrder;

    private Integer enabled;

    private String remark;
}
