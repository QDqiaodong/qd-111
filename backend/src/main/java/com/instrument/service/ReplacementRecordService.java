package com.instrument.service;

import com.instrument.common.PageResult;
import com.instrument.dto.ReplacementDTO;
import com.instrument.dto.ReplacementQueryDTO;
import com.instrument.entity.ReplacementRecord;

import java.util.List;

public interface ReplacementRecordService {

    PageResult<ReplacementRecord> page(ReplacementQueryDTO query);

    List<ReplacementRecord> list(ReplacementQueryDTO query);

    List<ReplacementRecord> history(Long accessoryId);

    ReplacementRecord getById(Long id);

    boolean add(ReplacementDTO dto);

    boolean update(ReplacementDTO dto);

    boolean remove(List<Long> ids);
}
