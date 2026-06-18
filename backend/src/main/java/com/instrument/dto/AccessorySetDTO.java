package com.instrument.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AccessorySetDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "套装名称不能为空")
    @Size(max = 100, message = "套装名称不能超过100个字符")
    private String name;

    @NotBlank(message = "适配乐器不能为空")
    private String instrument;

    @Size(max = 500, message = "套装说明不能超过500个字符")
    private String description;

    private String coverUrl;

    private String status;

    @NotEmpty(message = "套装至少包含一个配件")
    @Valid
    private List<AccessorySetItemDTO> items;
}
