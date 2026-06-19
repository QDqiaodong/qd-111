package com.instrument.service;

import com.instrument.common.PageResult;
import com.instrument.dto.PreparationChecklistGenerateDTO;
import com.instrument.dto.PreparationChecklistItemCompleteDTO;
import com.instrument.dto.PreparationChecklistQueryDTO;
import com.instrument.entity.PreparationChecklist;
import com.instrument.vo.PreparationChecklistCategoryVO;
import com.instrument.vo.PreparationChecklistVO;

import java.util.List;

public interface PreparationChecklistService {

    PageResult<PreparationChecklist> page(PreparationChecklistQueryDTO query);

    List<PreparationChecklist> list(PreparationChecklistQueryDTO query);

    PreparationChecklistVO getById(Long id);

    List<PreparationChecklistCategoryVO> getChecklistWithCategories(Long id);

    PreparationChecklistVO generate(PreparationChecklistGenerateDTO dto);

    PreparationChecklistVO startChecklist(Long id, String operator);

    PreparationChecklistVO completeItem(PreparationChecklistItemCompleteDTO dto);

    PreparationChecklistVO completeChecklist(Long id, String operator);

    boolean remove(List<Long> ids);

    boolean linkReplacementRecord(Long checklistId, Long replacementRecordId);

    List<PreparationChecklistVO> getByReplacementRecordId(Long replacementRecordId);

    List<PreparationChecklistVO> getByAccessoryId(Long accessoryId);
}
