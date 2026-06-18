package com.instrument.service;

import com.instrument.entity.AccessoryGroup;
import com.instrument.vo.GroupCapacityStatsVO;
import com.instrument.vo.GroupHealthScoreVO;

import java.util.List;

public interface AccessoryGroupService {

    List<AccessoryGroup> list();

    AccessoryGroup getById(Long id);

    AccessoryGroup add(AccessoryGroup group);

    boolean update(AccessoryGroup group);

    boolean remove(Long id);

    GroupHealthScoreVO healthScore(Long groupId);

    List<GroupHealthScoreVO> healthScores();

    List<GroupCapacityStatsVO> capacityStats();
}
