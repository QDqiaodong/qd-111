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
import com.instrument.service.AccessoryCompatibilityService;
import com.instrument.service.AccessoryService;
import com.instrument.service.DictService;
import com.instrument.service.StandardCycleRuleService;
import com.instrument.vo.AccessoryCompatibilityVO;
import com.instrument.vo.AccessoryLifecycleVO;
import com.instrument.vo.CycleReferenceVO;
import com.instrument.vo.CycleRuleMatchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessoryServiceImpl implements AccessoryService {

    private final AccessoryMapper accessoryMapper;
    private final ReplacementRecordMapper replacementRecordMapper;
    private final DictService dictService;
    private final StandardCycleRuleService cycleRuleService;
    private final AccessoryCompatibilityService compatibilityService;

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
        validateWornStatus(dto.getWornStatus());
        validateStandardCycle(dto.getStandardCycle());
        Accessory entity = new Accessory();
        BeanUtils.copyProperties(dto, entity);
        fillDictFields(entity);
        if (entity.getStandardCycle() == null) {
            Integer matchedCycle = cycleRuleService.getMatchedCycle(dto.getTypeCode(), dto.getInstrument(), dto.getSpecification());
            entity.setStandardCycle(matchedCycle);
        }
        return accessoryMapper.insert(entity) > 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = "accessory", allEntries = true)
    public boolean update(AccessoryDTO dto) {
        validateWornStatus(dto.getWornStatus());
        validateStandardCycle(dto.getStandardCycle());
        Accessory entity = new Accessory();
        BeanUtils.copyProperties(dto, entity);
        fillDictFields(entity);
        if (entity.getStandardCycle() == null) {
            Integer matchedCycle = cycleRuleService.getMatchedCycle(dto.getTypeCode(), dto.getInstrument(), dto.getSpecification());
            entity.setStandardCycle(matchedCycle);
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
        validateWornStatus(status);
        Accessory entity = new Accessory();
        entity.setId(id);
        entity.setWornStatus(status);
        return accessoryMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = "accessory", allEntries = true)
    public boolean batchUpdateStatus(List<Long> ids, String status) {
        validateWornStatus(status);
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

    private void validateWornStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("损耗状态不能为空");
        }
        Set<String> validStatuses = dictService.wornStatuses().stream()
                .map(d -> d.getCode())
                .collect(Collectors.toSet());
        if (!validStatuses.contains(status)) {
            throw new IllegalArgumentException("无效的损耗状态: " + status + "，合法值为: " + validStatuses);
        }
    }

    private void validateStandardCycle(Integer cycle) {
        if (cycle != null && cycle <= 0) {
            throw new IllegalArgumentException("标准更换周期必须大于0，当前值: " + cycle);
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

    @Override
    public CycleReferenceVO getCycleReference(String typeCode, String instrument, Integer currentCycle) {
        CycleReferenceVO vo = new CycleReferenceVO();

        Integer standardCycle = dictService.getStandardCycle(typeCode);
        vo.setStandardCycle(standardCycle);
        vo.setStandardCycleLabel(buildCycleLabel(standardCycle));

        if (org.springframework.util.StringUtils.hasText(typeCode) && org.springframework.util.StringUtils.hasText(instrument)) {
            LambdaQueryWrapper<Accessory> accessoryWrapper = new LambdaQueryWrapper<>();
            accessoryWrapper.eq(Accessory::getTypeCode, typeCode);
            accessoryWrapper.eq(Accessory::getInstrument, instrument);
            List<Accessory> sameTypeAccessories = accessoryMapper.selectList(accessoryWrapper);

            if (!sameTypeAccessories.isEmpty()) {
                List<Long> accessoryIds = sameTypeAccessories.stream()
                        .map(Accessory::getId)
                        .collect(java.util.stream.Collectors.toList());

                LambdaQueryWrapper<ReplacementRecord> recordWrapper = new LambdaQueryWrapper<>();
                recordWrapper.in(ReplacementRecord::getAccessoryId, accessoryIds);
                recordWrapper.isNotNull(ReplacementRecord::getUsageDays);
                recordWrapper.gt(ReplacementRecord::getUsageDays, 0);
                recordWrapper.orderByDesc(ReplacementRecord::getReplaceDate);
                List<ReplacementRecord> records = replacementRecordMapper.selectList(recordWrapper);

                vo.setHistoryCount(records.size());

                if (!records.isEmpty()) {
                    ReplacementRecord lastRecord = records.get(0);
                    vo.setLastInterval(lastRecord.getUsageDays());
                    vo.setLastReplaceDate(lastRecord.getReplaceDate());

                    double avg = records.stream()
                            .mapToInt(ReplacementRecord::getUsageDays)
                            .average()
                            .orElse(0);
                    vo.setAverageInterval((int) Math.round(avg));
                }
            }
        }

        if (currentCycle != null && currentCycle > 0) {
            vo.setCurrentInputCycle(currentCycle);

            if (standardCycle != null && standardCycle > 0) {
                int diff = currentCycle - standardCycle;
                vo.setDiffFromStandard(diff);
                vo.setDiffFromStandardLabel(buildDiffLabel(diff));
            }

            if (vo.getLastInterval() != null && vo.getLastInterval() > 0) {
                int diff = currentCycle - vo.getLastInterval();
                vo.setDiffFromLast(diff);
                vo.setDiffFromLastLabel(buildDiffLabel(diff));
            }
        }

        buildSuggestion(vo);

        return vo;
    }

    private String buildCycleLabel(Integer days) {
        if (days == null || days <= 0) return "未设置";
        if (days >= 365) {
            double years = days / 365.0;
            return String.format("约%.1f年", years);
        } else if (days >= 30) {
            double months = days / 30.0;
            return String.format("约%.1f个月", months);
        } else {
            return days + "天";
        }
    }

    private String buildDiffLabel(int diff) {
        if (diff == 0) return "一致";
        if (diff > 0) return "多" + diff + "天";
        return "少" + Math.abs(diff) + "天";
    }

    private void buildSuggestion(CycleReferenceVO vo) {
        Integer standard = vo.getStandardCycle();
        Integer current = vo.getCurrentInputCycle();
        Integer last = vo.getLastInterval();
        Integer avg = vo.getAverageInterval();

        if (current == null || current <= 0) {
            if (standard != null && standard > 0) {
                vo.setSuggestion("建议使用标准周期 " + standard + " 天");
                vo.setSuggestionLevel("info");
            } else {
                vo.setSuggestion("请输入合理的更换周期");
                vo.setSuggestionLevel("warning");
            }
            return;
        }

        if (standard != null && standard > 0) {
            double ratio = (double) current / standard;

            if (ratio < 0.5) {
                vo.setSuggestion("周期设置过短，会增加使用成本，请确认");
                vo.setSuggestionLevel("warning");
            } else if (ratio > 1.5) {
                vo.setSuggestion("周期设置过长，可能影响使用效果，建议缩短");
                vo.setSuggestionLevel("danger");
            } else if (last != null && last > 0) {
                double lastRatio = (double) current / last;
                if (lastRatio < 0.7) {
                    vo.setSuggestion("比上次同类更换间隔短很多，是使用习惯变了吗？");
                    vo.setSuggestionLevel("warning");
                } else if (lastRatio > 1.3) {
                    vo.setSuggestion("比上次同类更换间隔长很多，请注意配件损耗情况");
                    vo.setSuggestionLevel("warning");
                } else {
                    vo.setSuggestion("周期设置合理，与标准和历史数据相符");
                    vo.setSuggestionLevel("success");
                }
            } else if (avg != null && avg > 0) {
                double avgRatio = (double) current / avg;
                if (avgRatio < 0.7 || avgRatio > 1.3) {
                    vo.setSuggestion("与历史平均更换间隔差异较大，请确认");
                    vo.setSuggestionLevel("warning");
                } else {
                    vo.setSuggestion("周期设置合理");
                    vo.setSuggestionLevel("success");
                }
            } else {
                vo.setSuggestion("周期在标准范围内，暂无历史数据对比");
                vo.setSuggestionLevel("info");
            }
        } else {
            vo.setSuggestion("无标准周期参考，请根据实际情况设置");
            vo.setSuggestionLevel("info");
        }
    }

    @Override
    public com.instrument.vo.CalendarMonthVO getCalendarMonth(Integer year, Integer month) {
        if (year == null || month == null) {
            YearMonth now = YearMonth.now();
            year = now.getYear();
            month = now.getMonthValue();
        }

        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        LocalDate queryStart = monthStart.minusDays(60);
        LocalDate queryEnd = monthEnd.plusDays(30);

        LambdaQueryWrapper<Accessory> accessoryWrapper = new LambdaQueryWrapper<>();
        List<Accessory> accessories = accessoryMapper.selectList(accessoryWrapper);

        LambdaQueryWrapper<ReplacementRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.between(ReplacementRecord::getReplaceDate, queryStart, queryEnd);
        List<ReplacementRecord> records = replacementRecordMapper.selectList(recordWrapper);

        Map<Long, List<ReplacementRecord>> recordMap = records.stream()
                .collect(Collectors.groupingBy(ReplacementRecord::getAccessoryId));

        Map<String, com.instrument.vo.CalendarDayVO> dayMap = new LinkedHashMap<>();
        int expectedCount = 0;
        int replacedCount = 0;
        int severeCount = 0;

        for (Accessory accessory : accessories) {
            List<ReplacementRecord> accRecords = recordMap.getOrDefault(accessory.getId(), Collections.emptyList());
            accRecords.sort(Comparator.comparing(ReplacementRecord::getReplaceDate));

            LocalDate referenceDate = accessory.getPurchaseDate();
            if (!accRecords.isEmpty()) {
                referenceDate = accRecords.get(accRecords.size() - 1).getReplaceDate();
            }

            for (ReplacementRecord record : accRecords) {
                LocalDate replaceDate = record.getReplaceDate();
                if (!replaceDate.isBefore(monthStart) && !replaceDate.isAfter(monthEnd)) {
                    String dateKey = replaceDate.toString();
                    com.instrument.vo.CalendarDayVO dayVO = dayMap.computeIfAbsent(dateKey, k -> createDayVO(replaceDate));
                    dayVO.setHasReplaced(true);
                    replacedCount++;

                    com.instrument.vo.CalendarAccessoryVO accVO = createAccessoryVO(accessory, "replaced", "已更换");
                    dayVO.getAccessories().add(accVO);
                }
            }

            if (referenceDate != null && accessory.getStandardCycle() != null && accessory.getStandardCycle() > 0) {
                LocalDate expectedDate = referenceDate.plusDays(accessory.getStandardCycle());

                LocalDate windowStart = monthStart.minusDays(1);
                LocalDate windowEnd = monthEnd.plusDays(1);
                while (!expectedDate.isAfter(windowEnd)) {
                    if (!expectedDate.isBefore(windowStart) && !expectedDate.isAfter(monthEnd)) {
                        LocalDate dayDate = expectedDate;
                        String dateKey = expectedDate.toString();
                        com.instrument.vo.CalendarDayVO dayVO = dayMap.computeIfAbsent(dateKey, k -> createDayVO(dayDate));
                        dayVO.setHasExpected(true);
                        expectedCount++;

                        com.instrument.vo.CalendarAccessoryVO accVO = createAccessoryVO(accessory, "expected", "预计到期");
                        dayVO.getAccessories().add(accVO);
                    }
                    expectedDate = expectedDate.plusDays(accessory.getStandardCycle());
                }
            }

            if ("severe".equals(accessory.getWornStatus()) || "broken".equals(accessory.getWornStatus())) {
                LocalDate today = LocalDate.now();
                if (!today.isBefore(monthStart) && !today.isAfter(monthEnd)) {
                    String dateKey = today.toString();
                    com.instrument.vo.CalendarDayVO dayVO = dayMap.computeIfAbsent(dateKey, k -> createDayVO(today));
                    dayVO.setHasSevere(true);
                    severeCount++;

                    String status = "severe".equals(accessory.getWornStatus()) ? "severe" : "broken";
                    String label = "severe".equals(accessory.getWornStatus()) ? "严重损耗" : "已损坏";
                    com.instrument.vo.CalendarAccessoryVO accVO = createAccessoryVO(accessory, status, label);
                    dayVO.getAccessories().add(accVO);
                }
            }
        }

        List<com.instrument.vo.CalendarDayVO> days = new ArrayList<>(dayMap.values());
        days.sort(Comparator.comparing(com.instrument.vo.CalendarDayVO::getDate));

        com.instrument.vo.CalendarMonthVO vo = new com.instrument.vo.CalendarMonthVO();
        vo.setYear(year);
        vo.setMonth(month);
        vo.setDayMap(dayMap);
        vo.setDays(days);
        vo.setExpectedCount(expectedCount);
        vo.setReplacedCount(replacedCount);
        vo.setSevereCount(severeCount);

        return vo;
    }

    @Override
    public com.instrument.vo.CalendarDayVO getCalendarDay(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        LocalDate queryStart = date.minusDays(90);
        LocalDate queryEnd = date.plusDays(30);

        LambdaQueryWrapper<Accessory> accessoryWrapper = new LambdaQueryWrapper<>();
        List<Accessory> accessories = accessoryMapper.selectList(accessoryWrapper);

        LambdaQueryWrapper<ReplacementRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.between(ReplacementRecord::getReplaceDate, queryStart, queryEnd);
        List<ReplacementRecord> records = replacementRecordMapper.selectList(recordWrapper);

        Map<Long, List<ReplacementRecord>> recordMap = records.stream()
                .collect(Collectors.groupingBy(ReplacementRecord::getAccessoryId));

        com.instrument.vo.CalendarDayVO dayVO = createDayVO(date);

        for (Accessory accessory : accessories) {
            List<ReplacementRecord> accRecords = recordMap.getOrDefault(accessory.getId(), Collections.emptyList());
            accRecords.sort(Comparator.comparing(ReplacementRecord::getReplaceDate));

            LocalDate referenceDate = accessory.getPurchaseDate();
            if (!accRecords.isEmpty()) {
                referenceDate = accRecords.get(accRecords.size() - 1).getReplaceDate();
            }

            for (ReplacementRecord record : accRecords) {
                if (record.getReplaceDate().isEqual(date)) {
                    dayVO.setHasReplaced(true);
                    com.instrument.vo.CalendarAccessoryVO accVO = createAccessoryVO(accessory, "replaced", "已更换");
                    dayVO.getAccessories().add(accVO);
                }
            }

            if (referenceDate != null && accessory.getStandardCycle() != null && accessory.getStandardCycle() > 0) {
                LocalDate expectedDate = referenceDate.plusDays(accessory.getStandardCycle());
                while (!expectedDate.isAfter(date)) {
                    if (expectedDate.isEqual(date)) {
                        dayVO.setHasExpected(true);
                        com.instrument.vo.CalendarAccessoryVO accVO = createAccessoryVO(accessory, "expected", "预计到期");
                        dayVO.getAccessories().add(accVO);
                        break;
                    }
                    expectedDate = expectedDate.plusDays(accessory.getStandardCycle());
                }
            }

            if ("severe".equals(accessory.getWornStatus()) || "broken".equals(accessory.getWornStatus())) {
                LocalDate today = LocalDate.now();
                if (date.isEqual(today)) {
                    dayVO.setHasSevere(true);
                    String status = "severe".equals(accessory.getWornStatus()) ? "severe" : "broken";
                    String label = "severe".equals(accessory.getWornStatus()) ? "严重损耗" : "已损坏";
                    com.instrument.vo.CalendarAccessoryVO accVO = createAccessoryVO(accessory, status, label);
                    dayVO.getAccessories().add(accVO);
                }
            }
        }

        return dayVO;
    }

    private com.instrument.vo.CalendarDayVO createDayVO(LocalDate date) {
        com.instrument.vo.CalendarDayVO vo = new com.instrument.vo.CalendarDayVO();
        vo.setDate(date);
        vo.setAccessories(new ArrayList<>());
        vo.setHasExpected(false);
        vo.setHasReplaced(false);
        vo.setHasSevere(false);
        return vo;
    }

    private com.instrument.vo.CalendarAccessoryVO createAccessoryVO(Accessory accessory, String status, String statusLabel) {
        com.instrument.vo.CalendarAccessoryVO vo = new com.instrument.vo.CalendarAccessoryVO();
        vo.setAccessoryId(accessory.getId());
        vo.setName(accessory.getName());
        vo.setSpecification(accessory.getSpecification());
        vo.setInstrumentName(accessory.getInstrumentName());
        vo.setStatus(status);
        vo.setStatusLabel(statusLabel);
        return vo;
    }

    @Override
    public com.instrument.vo.CycleRuleMatchVO getMatchedCycleRule(String typeCode, String instrument, String specification, Integer manualCycle) {
        return cycleRuleService.matchRule(typeCode, instrument, specification, manualCycle);
    }

    @Override
    public com.instrument.vo.AccessoryCompatibilityVO checkCompatibility(String typeCode, String instrument, String specification) {
        return compatibilityService.checkCompatibility(typeCode, instrument, specification);
    }
}
