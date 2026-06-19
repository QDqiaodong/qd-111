package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class AnnualAccessoryStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer year;
    private Integer totalReplacements;
    private Integer avgUsageDays;
    private List<TypeAnnualStat> typeStats;
    private List<InstrumentConsumptionStat> topInstruments;

    @Data
    public static class TypeAnnualStat implements Serializable {

        private static final long serialVersionUID = 1L;

        private String typeCode;
        private String typeName;
        private Integer replacementCount;
        private Integer avgUsageDays;
    }

    @Data
    public static class InstrumentConsumptionStat implements Serializable {

        private static final long serialVersionUID = 1L;

        private String instrument;
        private String instrumentName;
        private Integer replacementCount;
    }
}
