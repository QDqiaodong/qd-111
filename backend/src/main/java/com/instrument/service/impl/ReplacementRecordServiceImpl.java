package com.instrument.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.common.PageResult;
import com.instrument.dto.ReplacementDTO;
import com.instrument.dto.ReplacementQueryDTO;
import com.instrument.entity.Accessory;
import com.instrument.entity.ReplacementRecord;
import com.instrument.mapper.AccessoryMapper;
import com.instrument.mapper.ReplacementRecordMapper;
import com.instrument.service.ReplacementRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplacementRecordServiceImpl implements ReplacementRecordService {

    private final ReplacementRecordMapper recordMapper;
    private final AccessoryMapper accessoryMapper;

    @Override
    public PageResult<ReplacementRecord> page(ReplacementQueryDTO query) {
        LambdaQueryWrapper<ReplacementRecord> wrapper = buildWrapper(query);
        wrapper.orderByDesc(ReplacementRecord::getReplaceDate);
        IPage<ReplacementRecord> page = recordMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public List<ReplacementRecord> list(ReplacementQueryDTO query) {
        LambdaQueryWrapper<ReplacementRecord> wrapper = buildWrapper(query);
        wrapper.orderByDesc(ReplacementRecord::getReplaceDate);
        return recordMapper.selectList(wrapper);
    }

    @Override
    public List<ReplacementRecord> history(Long accessoryId) {
        LambdaQueryWrapper<ReplacementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReplacementRecord::getAccessoryId, accessoryId);
        wrapper.orderByDesc(ReplacementRecord::getReplaceDate);
        return recordMapper.selectList(wrapper);
    }

    @Override
    public ReplacementRecord getById(Long id) {
        return recordMapper.selectById(id);
    }

    @Override
    @Transactional
    public boolean add(ReplacementDTO dto) {
        ReplacementRecord entity = new ReplacementRecord();
        BeanUtils.copyProperties(dto, entity);
        fillAccessoryInfo(entity, dto.getAccessoryId());
        calculateUsageDays(entity, dto.getAccessoryId(), null);
        return recordMapper.insert(entity) > 0;
    }

    @Override
    @Transactional
    public boolean update(ReplacementDTO dto) {
        ReplacementRecord entity = new ReplacementRecord();
        BeanUtils.copyProperties(dto, entity);
        fillAccessoryInfo(entity, dto.getAccessoryId());
        calculateUsageDays(entity, dto.getAccessoryId(), dto.getId());
        return recordMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    public boolean remove(List<Long> ids) {
        return recordMapper.deleteBatchIds(ids) > 0;
    }

    private void fillAccessoryInfo(ReplacementRecord entity, Long accessoryId) {
        if (accessoryId == null) return;
        Accessory accessory = accessoryMapper.selectById(accessoryId);
        if (accessory != null) {
            entity.setAccessoryName(accessory.getName());
            entity.setSpecification(accessory.getSpecification());
            entity.setInstrumentName(accessory.getInstrumentName());
            entity.setImageUrl(accessory.getImageUrl());
            entity.setStandardCycle(accessory.getStandardCycle());
        }
    }

    private void calculateUsageDays(ReplacementRecord entity, Long accessoryId, Long excludeId) {
        if (entity.getReplaceDate() == null || accessoryId == null) return;
        List<ReplacementRecord> history = history(accessoryId);
        if (excludeId != null) {
            history = history.stream()
                    .filter(r -> !r.getId().equals(excludeId))
                    .toList();
        }
        List<ReplacementRecord> beforeCurrent = history.stream()
                .filter(r -> r.getReplaceDate() != null && !r.getReplaceDate().isAfter(entity.getReplaceDate()))
                .toList();
        if (beforeCurrent.isEmpty()) {
            Accessory accessory = accessoryMapper.selectById(accessoryId);
            if (accessory != null && accessory.getPurchaseDate() != null) {
                long days = ChronoUnit.DAYS.between(accessory.getPurchaseDate(), entity.getReplaceDate());
                entity.setUsageDays((int) Math.max(days, 0));
            } else {
                entity.setUsageDays(0);
            }
            return;
        }
        beforeCurrent.sort(Comparator.comparing(ReplacementRecord::getReplaceDate).reversed());
        ReplacementRecord last = beforeCurrent.get(0);
        if (last != null && last.getReplaceDate() != null) {
            long days = ChronoUnit.DAYS.between(last.getReplaceDate(), entity.getReplaceDate());
            entity.setUsageDays((int) Math.max(days, 0));
        }
    }

    private LambdaQueryWrapper<ReplacementRecord> buildWrapper(ReplacementQueryDTO query) {
        LambdaQueryWrapper<ReplacementRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(ReplacementRecord::getAccessoryName, query.getKeyword())
                    .or().like(ReplacementRecord::getSpecification, query.getKeyword()));
        }
        if (query.getAccessoryId() != null) {
            wrapper.eq(ReplacementRecord::getAccessoryId, query.getAccessoryId());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(ReplacementRecord::getReplaceDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(ReplacementRecord::getReplaceDate, query.getEndDate());
        }
        return wrapper;
    }
}
