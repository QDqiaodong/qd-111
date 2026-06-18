package com.instrument.service;

import com.instrument.common.PageResult;
import com.instrument.dto.AccessorySetDTO;
import com.instrument.dto.AccessorySetQueryDTO;
import com.instrument.entity.AccessorySet;
import com.instrument.vo.AccessorySetVO;

import java.util.List;

public interface AccessorySetService {

    PageResult<AccessorySet> page(AccessorySetQueryDTO query);

    List<AccessorySet> list(AccessorySetQueryDTO query);

    AccessorySetVO getById(Long id);

    boolean add(AccessorySetDTO dto);

    boolean update(AccessorySetDTO dto);

    boolean remove(List<Long> ids);

    boolean updateStatus(Long id, String status);
}
