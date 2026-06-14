package com.instrument.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.common.PageResult;
import com.instrument.dto.AccessoryDTO;
import com.instrument.dto.AccessoryQueryDTO;
import com.instrument.entity.Accessory;
import com.instrument.mapper.AccessoryMapper;
import com.instrument.service.AccessoryService;
import com.instrument.service.DictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessoryServiceImpl implements AccessoryService {

    private final AccessoryMapper accessoryMapper;
    private final DictService dictService;

    @Override
    public PageResult<Accessory> page(AccessoryQueryDTO query) {
        LambdaQueryWrapper<Accessory> wrapper = buildWrapper(query);
        wrapper.orderByDesc(Accessory::getCreateTime);
        IPage<Accessory> page = accessoryMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    @Cacheable(value = "accessory", key = "'list' + #query.hashCode()", unless = "#result == null")
    public List<Accessory> list(AccessoryQueryDTO query) {
        LambdaQueryWrapper<Accessory> wrapper = buildWrapper(query);
        wrapper.orderByDesc(Accessory::getCreateTime);
        return accessoryMapper.selectList(wrapper);
    }

    @Override
    @Cacheable(value = "accessory", key = "#id", unless = "#result == null")
    public Accessory getById(Long id) {
        return accessoryMapper.selectById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "accessory", allEntries = true)
    public boolean add(AccessoryDTO dto) {
        Accessory entity = new Accessory();
        BeanUtils.copyProperties(dto, entity);
        fillDictFields(entity);
        if (entity.getStandardCycle() == null) {
            entity.setStandardCycle(dictService.getStandardCycle(dto.getTypeCode()));
        }
        return accessoryMapper.insert(entity) > 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = "accessory", allEntries = true)
    public boolean update(AccessoryDTO dto) {
        Accessory entity = new Accessory();
        BeanUtils.copyProperties(dto, entity);
        fillDictFields(entity);
        return accessoryMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = "accessory", allEntries = true)
    public boolean remove(List<Long> ids) {
        return accessoryMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = "accessory", allEntries = true)
    public boolean updateStatus(Long id, String status) {
        Accessory entity = new Accessory();
        entity.setId(id);
        entity.setWornStatus(status);
        return accessoryMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = "accessory", allEntries = true)
    public boolean batchUpdateStatus(List<Long> ids, String status) {
        for (Long id : ids) {
            Accessory entity = new Accessory();
            entity.setId(id);
            entity.setWornStatus(status);
            accessoryMapper.updateById(entity);
        }
        return true;
    }

    @Override
    public long countByGroup(Long groupId) {
        LambdaQueryWrapper<Accessory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Accessory::getGroupId, groupId);
        return accessoryMapper.selectCount(wrapper);
    }

    @Override
    public long countByStatus(String status) {
        LambdaQueryWrapper<Accessory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Accessory::getWornStatus, status);
        return accessoryMapper.selectCount(wrapper);
    }

    private void fillDictFields(Accessory entity) {
        if (StringUtils.hasText(entity.getTypeCode())) {
            entity.setTypeName(dictService.getAccessoryTypeLabel(entity.getTypeCode()));
        }
        if (StringUtils.hasText(entity.getInstrument())) {
            entity.setInstrumentName(dictService.getInstrumentLabel(entity.getInstrument()));
        }
        if (StringUtils.hasText(entity.getWornStatus())) {
        }
    }

    private LambdaQueryWrapper<Accessory> buildWrapper(AccessoryQueryDTO query) {
        LambdaQueryWrapper<Accessory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Accessory::getName, query.getKeyword())
                    .or().like(Accessory::getSpecification, query.getKeyword()));
        }
        if (query.getGroupId() != null) {
            wrapper.eq(Accessory::getGroupId, query.getGroupId());
        }
        if (StringUtils.hasText(query.getTypeCode())) {
            wrapper.eq(Accessory::getTypeCode, query.getTypeCode());
        }
        if (StringUtils.hasText(query.getWornStatus())) {
            wrapper.eq(Accessory::getWornStatus, query.getWornStatus());
        }
        if (StringUtils.hasText(query.getInstrument())) {
            wrapper.eq(Accessory::getInstrument, query.getInstrument());
        }
        return wrapper;
    }
}
