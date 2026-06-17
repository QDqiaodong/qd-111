package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
public class CalendarDayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate date;
    private List<CalendarAccessoryVO> accessories;
    private boolean hasExpected;
    private boolean hasReplaced;
    private boolean hasSevere;
}
