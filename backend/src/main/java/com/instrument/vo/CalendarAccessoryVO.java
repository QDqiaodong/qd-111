package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class CalendarAccessoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long accessoryId;
    private String name;
    private String specification;
    private String instrumentName;
    private String status;
    private String statusLabel;
}
