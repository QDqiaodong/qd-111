package com.instrument.service;

import com.instrument.common.PageResult;
import com.instrument.dto.ReplacementDTO;
import com.instrument.dto.ReplacementQueryDTO;
import com.instrument.entity.ReplacementRecord;
import com.instrument.vo.ReplacementResultVO;
import com.instrument.vo.ReplacementTimelineVO;

import java.util.List;

public interface ReplacementRecordService {

    PageResult<ReplacementRecord> page(ReplacementQueryDTO query);

    List<ReplacementRecord> list(ReplacementQueryDTO query);

    List<ReplacementRecord> history(Long accessoryId);

    ReplacementRecord getById(Long id);

    ReplacementResultVO add(ReplacementDTO dto);

    ReplacementResultVO update(ReplacementDTO dto);

    boolean remove(List<Long> ids);

    List<ReplacementTimelineVO> timeline(ReplacementQueryDTO query);

    void recalculateByAccessory(Long accessoryId);

    void recalculateByAccessoryIds(List<Long> accessoryIds);

    void recalculateByAccessoryWithStandardCycle(Long accessoryId);

    void recalculateByCondition(String typeCode, String instrument);
}
