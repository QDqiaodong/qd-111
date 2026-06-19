package com.instrument.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

@Data
public class PreparationChecklistItemCompleteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "清单项ID不能为空")
    private Long itemId;

    @NotNull(message = "完成状态不能为空")
    private Integer completed;

    private String completedBy;

    private String completionNote;
}
