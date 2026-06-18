package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class DictItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private String label;
    private Integer sortOrder;

    public DictItemVO() {}

    public DictItemVO(String code, String label, Integer sortOrder) {
        this.code = code;
        this.label = label;
        this.sortOrder = sortOrder;
    }
}
