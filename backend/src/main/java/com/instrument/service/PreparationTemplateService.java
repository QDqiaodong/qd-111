package com.instrument.service;

import com.instrument.common.PageResult;
import com.instrument.dto.PreparationTemplateDTO;
import com.instrument.dto.PreparationTemplateQueryDTO;
import com.instrument.entity.PreparationTemplate;
import com.instrument.vo.PreparationTemplateVO;

import java.util.List;

public interface PreparationTemplateService {

    PageResult<PreparationTemplate> page(PreparationTemplateQueryDTO query);

    List<PreparationTemplate> list(PreparationTemplateQueryDTO query);

    PreparationTemplateVO getById(Long id);

    PreparationTemplateVO getByTypeCode(String typeCode);

    PreparationTemplateVO add(PreparationTemplateDTO dto);

    PreparationTemplateVO update(PreparationTemplateDTO dto);

    boolean remove(List<Long> ids);

    boolean updateStatus(Long id, Integer enabled);
}
