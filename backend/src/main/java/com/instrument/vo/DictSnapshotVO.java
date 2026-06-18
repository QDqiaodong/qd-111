package com.instrument.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DictSnapshotVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<DictItemVO> accessoryTypes;
    private LocalDateTime accessoryTypesUpdateTime;

    private List<DictItemVO> instruments;
    private LocalDateTime instrumentsUpdateTime;

    private List<DictItemVO> wornStatuses;
    private LocalDateTime wornStatusesUpdateTime;

    private List<DictItemVO> replacementCycles;
    private LocalDateTime replacementCyclesUpdateTime;

    private LocalDateTime snapshotTime;
}
