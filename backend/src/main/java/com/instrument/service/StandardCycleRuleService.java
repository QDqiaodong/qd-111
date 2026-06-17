package com.instrument.service;

import com.instrument.common.PageResult;
import com.instrument.dto.CycleRuleQueryDTO;
import com.instrument.dto.StandardCycleRuleDTO;
import com.instrument.entity.StandardCycleRule;
import com.instrument.vo.CycleRuleMatchVO;

import java.util.List;

public interface StandardCycleRuleService {

    PageResult<StandardCycleRule> page(CycleRuleQueryDTO query);

    List<StandardCycleRule> list(CycleRuleQueryDTO query);

    StandardCycleRule getById(Long id);

    boolean add(StandardCycleRuleDTO dto);

    boolean update(StandardCycleRuleDTO dto);

    boolean remove(List<Long> ids);

    CycleRuleMatchVO matchRule(String typeCode, String instrument, String specification);

    CycleRuleMatchVO matchRule(String typeCode, String instrument, String specification, Integer manualCycle);

    Integer getMatchedCycle(String typeCode, String instrument, String specification);

    List<StandardCycleRule> findCandidateRules(String typeCode, String instrument, String specification);
}
