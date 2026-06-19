package com.instrument.service;

import com.instrument.common.PageResult;
import com.instrument.dto.WornStatusDictDTO;
import com.instrument.entity.WornStatusDict;
import com.instrument.vo.WornStatusUsageVO;

import java.util.List;

public interface WornStatusDictService {

    PageResult<WornStatusDict> page(Integer pageNum, Integer pageSize, String keyword);

    List<WornStatusDict> listEnabled();

    List<WornStatusDict> listAll();

    WornStatusDict getById(Long id);

    WornStatusDict getByCode(String statusCode);

    boolean add(WornStatusDictDTO dto);

    boolean update(WornStatusDictDTO dto);

    boolean remove(List<Long> ids);

    WornStatusUsageVO getUsageInfo(Long id);

    boolean toggleStatus(Long id, Integer enabled);
}
