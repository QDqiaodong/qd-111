package com.instrument.service;

import com.instrument.common.PageResult;
import com.instrument.dto.AccessoryDTO;
import com.instrument.dto.AccessoryQueryDTO;
import com.instrument.entity.Accessory;
import com.instrument.vo.AccessoryLifecycleVO;

import java.util.List;

public interface AccessoryService {

    PageResult<Accessory> page(AccessoryQueryDTO query);

    List<Accessory> list(AccessoryQueryDTO query);

    Accessory getById(Long id);

    boolean add(AccessoryDTO dto);

    boolean update(AccessoryDTO dto);

    boolean remove(List<Long> ids);

    boolean updateStatus(Long id, String status);

    boolean batchUpdateStatus(List<Long> ids, String status);

    long countByGroup(Long groupId);

    long countByStatus(String status);

    AccessoryLifecycleVO getLifecycle(Long id);

    List<AccessoryLifecycleVO> listLifecycle(AccessoryQueryDTO query);

    com.instrument.vo.CycleReferenceVO getCycleReference(String typeCode, String instrument, Integer currentCycle);
}
