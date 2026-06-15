package com.instrument.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.common.PageResult;
import com.instrument.dto.AccessoryDTO;
import com.instrument.dto.AccessoryQueryDTO;
import com.instrument.entity.Accessory;
import com.instrument.entity.ReplacementRecord;
import com.instrument.mapper.AccessoryMapper;
import com.instrument.mapper.ReplacementRecordMapper;
import com.instrument.service.AccessoryService;
import com.instrument.service.DictService;
import com.instrument.vo.AccessoryLifecycleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessoryServiceImpl implements AccessoryService {

    private final AccessoryMapper accessoryMapper;
    private final ReplacementRecordMapper replacementRecordMapper;
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
        if (entity.getStandardCycle() == null || entity.getStandardCycle() <= 0) {
            entity.setStandardCycle(dictService.getStandardCycle(dto.getTypeCode()));
        }
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

    @Override
    public AccessoryLifecycleVO getLifecycle(Long id) {
        Accessory accessory = accessoryMapper.selectById(id);
        if (accessory == null) return null;
        return buildLifecycleVO(accessory);
    }

    @Override
    public List<AccessoryLifecycleVO> listLifecycle(AccessoryQueryDTO query) {
        LambdaQueryWrapper<Accessory> wrapper = buildWrapper(query);
        wrapper.orderByDesc(Accessory::getCreateTime);
        List<Accessory> list = accessoryMapper.selectList(wrapper);
        return list.stream().map(this::buildLifecycleVO).collect(Collectors.toList());
    }

    private AccessoryLifecycleVO buildLifecycleVO(Accessory accessory) {
        AccessoryLifecycleVO vo = new AccessoryLifecycleVO();
        vo.setAccessoryId(accessory.getId());
        vo.setName(accessory.getName());
        vo.setPurchaseDate(accessory.getPurchaseDate());
        vo.setStandardCycle(accessory.getStandardCycle());
        vo.setWornStatus(accessory.getWornStatus());

        LocalDate referenceDate = getLastReplaceDate(accessory.getId());
        vo.setLastReplaceDate(referenceDate);

        if (referenceDate == null && accessory.getPurchaseDate() != null) {
            referenceDate = accessory.getPurchaseDate();
        }

        if (referenceDate != null) {
            int usedDays = (int) ChronoUnit.DAYS.between(referenceDate, LocalDate.now());
            vo.setUsedDays(Math.max(usedDays, 0));
        } else {
            vo.setUsedDays(0);
        }

        if (accessory.getStandardCycle() != null && accessory.getStandardCycle() > 0) {
            int pct = Math.round((float) vo.getUsedDays() / accessory.getStandardCycle() * 100);
            vo.setCyclePercent(Math.min(pct, 100));
            int daysLeft = accessory.getStandardCycle() - vo.getUsedDays();
            vo.setDaysLeft(Math.max(daysLeft, 0));
        } else {
            vo.setCyclePercent(0);
            vo.setDaysLeft(0);
        }

        String stage = determineStage(vo.getCyclePercent(), accessory.getWornStatus());
        vo.setStage(stage);
        vo.setStageLabel(getStageLabel(stage));
        return vo;
    }

    private LocalDate getLastReplaceDate(Long accessoryId) {
        LambdaQueryWrapper<ReplacementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReplacementRecord::getAccessoryId, accessoryId);
        wrapper.orderByDesc(ReplacementRecord::getReplaceDate);
        wrapper.last("LIMIT 1");
        ReplacementRecord last = replacementRecordMapper.selectOne(wrapper);
        return last != null ? last.getReplaceDate() : null;
    }

    private String determineStage(int cyclePercent, String wornStatus) {
        if ("broken".equals(wornStatus)) return "broken";
        if (cyclePercent >= 100 || "severe".equals(wornStatus)) return "expired";
        if (cyclePercent >= 80) return "warning";
        if (cyclePercent >= 50) return "aging";
        return "fresh";
    }

    private String getStageLabel(String stage) {
        switch (stage) {
            case "fresh": return "初期";
            case "aging": return "中期";
            case "warning": return "临近更换";
            case "expired": return "已超期";
            case "broken": return "已损坏";
            default: return "未知";
        }
    }
}
