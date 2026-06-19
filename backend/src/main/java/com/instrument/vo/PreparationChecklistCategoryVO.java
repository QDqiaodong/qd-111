package com.instrument.vo;

import com.instrument.entity.PreparationChecklistItem;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PreparationChecklistCategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String category;

    private String categoryName;

    private Integer totalCount;

    private Integer completedCount;

    private List<PreparationChecklistItem> items;
}
