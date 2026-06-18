package com.instrument.vo;

import com.instrument.entity.AccessorySetItem;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AccessorySetVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String instrument;

    private String instrumentName;

    private String description;

    private String coverUrl;

    private String status;

    private Integer itemCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<AccessorySetItem> items;
}
