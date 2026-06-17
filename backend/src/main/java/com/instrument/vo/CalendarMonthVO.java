package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class CalendarMonthVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer year;
    private Integer month;
    private Map<String, CalendarDayVO> dayMap;
    private List<CalendarDayVO> days;
    private Integer expectedCount;
    private Integer replacedCount;
    private Integer severeCount;
}
