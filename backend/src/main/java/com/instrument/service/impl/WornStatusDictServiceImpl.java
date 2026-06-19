package com.instrument.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.common.PageResult;
import com.instrument.dto.WornStatusDictDTO;
import com.instrument.entity.Accessory;
import com.instrument.entity.WornStatusDict;
import com.instrument.mapper.AccessoryMapper;
import com.instrument.mapper.WornStatusDictMapper;
import com.instrument.service.WornStatusDictService;
import com.instrument.vo.WornStatusUsageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WornStatusDictServiceImpl implements WornStatusDictService {

    private final WornStatusDictMapper wornStatusDictMapper;
    private final AccessoryMapper accessoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY = "instrument:dict:wornStatuses";

    @Override
    public PageResult<WornStatusDict> page(Integer pageNum, Integer pageSize, String keyword) {
        LambdaQueryWrapper<WornStatusDict> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(WornStatusDict::getStatusCode, keyword)
                    .or().like(WornStatusDict::getStatusLabel, keyword));
        }
        wrapper.orderByAsc(WornStatusDict::getSortOrder)
                .orderByDesc(WornStatusDict::getCreateTime);
        IPage<WornStatusDict> page = wornStatusDictMapper.selectPage(
                new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10), wrapper);
        return PageResult.of(page);
    }

    @Override
    @Cacheable(value = "wornStatusDict", key = "'enabledList'", unless = "#result == null || #result.isEmpty()")
    public List<WornStatusDict> listEnabled() {
        LambdaQueryWrapper<WornStatusDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WornStatusDict::getEnabled, 1);
        wrapper.orderByAsc(WornStatusDict::getSortOrder);
        return wornStatusDictMapper.selectList(wrapper);
    }

    @Override
    public List<WornStatusDict> listAll() {
        LambdaQueryWrapper<WornStatusDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(WornStatusDict::getSortOrder);
        return wornStatusDictMapper.selectList(wrapper);
    }

    @Override
    public WornStatusDict getById(Long id) {
        return wornStatusDictMapper.selectById(id);
    }

    @Override
    public WornStatusDict getByCode(String statusCode) {
        LambdaQueryWrapper<WornStatusDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WornStatusDict::getStatusCode, statusCode);
        return wornStatusDictMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    @CacheEvict(value = "wornStatusDict", allEntries = true)
    public boolean add(WornStatusDictDTO dto) {
        validateCodeUnique(dto.getStatusCode(), null);
        WornStatusDict entity = new WornStatusDict();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        if (!StringUtils.hasText(entity.getColor())) {
            entity.setColor("#909399");
        }
        clearDictCache();
        return wornStatusDictMapper.insert(entity) > 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = "wornStatusDict", allEntries = true)
    public boolean update(WornStatusDictDTO dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("ID不能为空");
        }
        validateCodeUnique(dto.getStatusCode(), dto.getId());
        WornStatusDict entity = new WornStatusDict();
        BeanUtils.copyProperties(dto, entity);
        clearDictCache();
        return wornStatusDictMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = "wornStatusDict", allEntries = true)
    public boolean remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            WornStatusDict dict = wornStatusDictMapper.selectById(id);
            if (dict == null) {
                continue;
            }
            WornStatusUsageVO usage = getUsageInfo(id);
            if (!usage.getCanDelete()) {
                throw new IllegalStateException(
                    "状态「" + dict.getStatusLabel() + "」仍被 " + usage.getUsedCount() + " 个配件使用，无法删除。" +
                    "典型配件：" + String.join("、", usage.getTypicalAccessoryNames())
                );
            }
        }
        clearDictCache();
        return wornStatusDictMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public WornStatusUsageVO getUsageInfo(Long id) {
        WornStatusDict dict = wornStatusDictMapper.selectById(id);
        if (dict == null) {
            return new WornStatusUsageVO(null, null, true, 0L, List.of());
        }

        LambdaQueryWrapper<Accessory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Accessory::getWornStatus, dict.getStatusCode());
        Long count = accessoryMapper.selectCount(wrapper);

        List<String> typicalNames = List.of();
        if (count > 0) {
            wrapper.orderByDesc(Accessory::getCreateTime);
            wrapper.last("LIMIT 5");
            List<Accessory> accessories = accessoryMapper.selectList(wrapper);
            typicalNames = accessories.stream()
                    .map(Accessory::getName)
                    .collect(Collectors.toList());
        }

        return new WornStatusUsageVO(
                dict.getStatusCode(),
                dict.getStatusLabel(),
                count == 0,
                count,
                typicalNames
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = "wornStatusDict", allEntries = true)
    public boolean toggleStatus(Long id, Integer enabled) {
        if (id == null) {
            throw new IllegalArgumentException("ID不能为空");
        }
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new IllegalArgumentException("状态值不正确");
        }
        if (enabled == 0) {
            WornStatusUsageVO usage = getUsageInfo(id);
            if (!usage.getCanDelete()) {
                WornStatusDict dict = wornStatusDictMapper.selectById(id);
                throw new IllegalStateException(
                    "状态「" + dict.getStatusLabel() + "」仍被 " + usage.getUsedCount() + " 个配件使用，无法禁用。" +
                    "典型配件：" + String.join("、", usage.getTypicalAccessoryNames())
                );
            }
        }
        WornStatusDict entity = new WornStatusDict();
        entity.setId(id);
        entity.setEnabled(enabled);
        clearDictCache();
        return wornStatusDictMapper.updateById(entity) > 0;
    }

    private void validateCodeUnique(String statusCode, Long excludeId) {
        if (!StringUtils.hasText(statusCode)) {
            return;
        }
        LambdaQueryWrapper<WornStatusDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WornStatusDict::getStatusCode, statusCode);
        if (excludeId != null) {
            wrapper.ne(WornStatusDict::getId, excludeId);
        }
        Long count = wornStatusDictMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("状态编码已存在: " + statusCode);
        }
    }

    private void clearDictCache() {
        try {
            redisTemplate.delete(CACHE_KEY);
            log.debug("损耗状态字典Redis缓存已清除");
        } catch (Exception e) {
            log.warn("清除损耗状态字典Redis缓存失败: {}", e.getMessage());
        }
    }
}
