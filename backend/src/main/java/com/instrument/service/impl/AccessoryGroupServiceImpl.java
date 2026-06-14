package com.instrument.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.instrument.entity.AccessoryGroup;
import com.instrument.mapper.AccessoryGroupMapper;
import com.instrument.service.AccessoryGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccessoryGroupServiceImpl implements AccessoryGroupService {

    private final AccessoryGroupMapper groupMapper;

    @Override
    public List<AccessoryGroup> list() {
        LambdaQueryWrapper<AccessoryGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(AccessoryGroup::getSortOrder).orderByAsc(AccessoryGroup::getId);
        return groupMapper.selectList(wrapper);
    }

    @Override
    public AccessoryGroup getById(Long id) {
        return groupMapper.selectById(id);
    }

    @Override
    @Transactional
    public boolean add(AccessoryGroup group) {
        if (group.getSortOrder() == null) {
            group.setSortOrder(0);
        }
        return groupMapper.insert(group) > 0;
    }

    @Override
    @Transactional
    public boolean update(AccessoryGroup group) {
        return groupMapper.updateById(group) > 0;
    }

    @Override
    @Transactional
    public boolean remove(Long id) {
        return groupMapper.deleteById(id) > 0;
    }
}
