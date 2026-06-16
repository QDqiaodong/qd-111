package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class WornHeatmapVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<InstrumentHeatmapVO> instruments;

    private List<AccessoryTypeHeatmapVO> accessoryTypes;

    private List<WornStatusLegendVO> legends;

    private List<HeatmapCellVO> cells;

    @Data
    public static class InstrumentHeatmapVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String code;
        private String label;

        public InstrumentHeatmapVO(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    @Data
    public static class AccessoryTypeHeatmapVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String code;
        private String label;

        public AccessoryTypeHeatmapVO(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    @Data
    public static class WornStatusLegendVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String code;
        private String label;
        private String color;

        public WornStatusLegendVO(String code, String label, String color) {
            this.code = code;
            this.label = label;
            this.color = color;
        }
    }

    @Data
    public static class HeatmapCellVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String instrumentCode;
        private String instrumentName;
        private String typeCode;
        private String typeName;
        private long total;
        private long goodCount;
        private long slightCount;
        private long severeCount;
        private long brokenCount;
    }
}
