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
import com.instrument.service.StandardCycleRuleService;
import com.instrument.vo.ReplacementResultVO;
import com.instrument.vo.ReplacementTimelineItemVO;
import com.instrument.vo.ReplacementTimelineVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplacementRecordServiceImpl implements ReplacementRecordService {

    private final ReplacementRecordMapper recordMapper;
    private final AccessoryMapper accessoryMapper;
    private final StandardCycleRuleService cycleRuleService;

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
    public ReplacementResultVO add(ReplacementDTO dto) {
        Accessory accessory = validateAndGetAccessory(dto.getAccessoryId());
        validateReplaceDate(dto.getReplaceDate(), accessory);

        ReplacementRecord entity = new ReplacementRecord();
        BeanUtils.copyProperties(dto, entity);
        fillAccessoryInfo(entity, dto.getAccessoryId());
        calculateUsageDays(entity, dto.getAccessoryId(), null);

        boolean result = recordMapper.insert(entity) > 0;
        if (!result) {
            return null;
        }

        recalculateByAccessory(dto.getAccessoryId());

        ReplacementRecord savedRecord = recordMapper.selectById(entity.getId());
        ReplacementResultVO resultVO = buildResultVO(savedRecord, accessory);
        updateAccessoryStatusToGood(accessory, resultVO);

        return resultVO;
    }

    @Override
    @Transactional
    public ReplacementResultVO update(ReplacementDTO dto) {
        Accessory accessory = validateAndGetAccessory(dto.getAccessoryId());
        validateReplaceDate(dto.getReplaceDate(), accessory);

        ReplacementRecord entity = new ReplacementRecord();
        BeanUtils.copyProperties(dto, entity);
        fillAccessoryInfo(entity, dto.getAccessoryId());
        calculateUsageDays(entity, dto.getAccessoryId(), dto.getId());

        boolean result = recordMapper.updateById(entity) > 0;
        if (!result) {
            return null;
        }

        recalculateByAccessory(dto.getAccessoryId());

        ReplacementRecord updatedRecord = recordMapper.selectById(dto.getId());
        ReplacementResultVO resultVO = buildResultVO(updatedRecord, accessory);
        updateAccessoryStatusToGood(accessory, resultVO);

        return resultVO;
    }

    @Override
    @Transactional
    public boolean remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        Set<Long> accessoryIds = new HashSet<>();
        for (Long id : ids) {
            ReplacementRecord record = recordMapper.selectById(id);
            if (record != null && record.getAccessoryId() != null) {
                accessoryIds.add(record.getAccessoryId());
            }
        }
        boolean result = recordMapper.deleteBatchIds(ids) > 0;
        if (result) {
            recalculateByAccessoryIds(new ArrayList<>(accessoryIds));
        }
        return result;
    }

    @Override
    public void recalculateByAccessory(Long accessoryId) {
        recalculateAllUsageDaysAndCycle(accessoryId, false);
    }

    @Override
    public void recalculateByAccessoryIds(List<Long> accessoryIds) {
        if (accessoryIds == null || accessoryIds.isEmpty()) return;
        for (Long accessoryId : accessoryIds) {
            recalculateAllUsageDaysAndCycle(accessoryId, false);
        }
    }

    @Override
    public void recalculateByAccessoryWithStandardCycle(Long accessoryId) {
        recalculateAllUsageDaysAndCycle(accessoryId, true);
    }

    @Override
    public void recalculateByCondition(String typeCode, String instrument) {
        LambdaQueryWrapper<Accessory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(typeCode)) {
            wrapper.eq(Accessory::getTypeCode, typeCode);
        }
        if (StringUtils.hasText(instrument)) {
            wrapper.and(w -> w
                    .eq(Accessory::getInstrument, instrument)
                    .or().isNull(Accessory::getInstrument));
        }
        List<Accessory> accessories = accessoryMapper.selectList(wrapper);
        if (accessories == null || accessories.isEmpty()) return;
        log.info("按条件重算更换记录，typeCode={}, instrument={}, 影响配件数={}", typeCode, instrument, accessories.size());
        for (Accessory acc : accessories) {
            recalculateAllUsageDaysAndCycle(acc.getId(), true);
        }
    }

    @Transactional
    public void recalculateAllUsageDaysAndCycle(Long accessoryId, boolean refreshStandardCycle) {
        if (accessoryId == null) return;
        List<ReplacementRecord> allRecords = history(accessoryId);
        if (allRecords.isEmpty()) return;

        allRecords.sort(Comparator.comparing(ReplacementRecord::getReplaceDate));

        Accessory accessory = accessoryMapper.selectById(accessoryId);
        Integer accessoryStandardCycle = null;
        if (accessory != null && refreshStandardCycle) {
            accessoryStandardCycle = accessory.getStandardCycle();
            if (accessoryStandardCycle == null || accessoryStandardCycle <= 0) {
                accessoryStandardCycle = cycleRuleService.getMatchedCycle(
                        accessory.getTypeCode(),
                        accessory.getInstrument(),
                        accessory.getSpecification()
                );
            }
        }

        int recalculatedCount = 0;
        for (int i = 0; i < allRecords.size(); i++) {
            ReplacementRecord current = allRecords.get(i);
            boolean needUpdate = false;

            int usageDays;
            if (i == 0) {
                if (accessory != null && accessory.getPurchaseDate() != null) {
                    usageDays = (int) Math.max(ChronoUnit.DAYS.between(accessory.getPurchaseDate(), current.getReplaceDate()), 0);
                } else {
                    usageDays = 0;
                }
            } else {
                ReplacementRecord previous = allRecords.get(i - 1);
                usageDays = (int) Math.max(ChronoUnit.DAYS.between(previous.getReplaceDate(), current.getReplaceDate()), 0);
            }
            if (current.getUsageDays() == null || usageDays != current.getUsageDays()) {
                current.setUsageDays(usageDays);
                needUpdate = true;
            }

            if (refreshStandardCycle && accessoryStandardCycle != null) {
                if (current.getStandardCycle() == null || !accessoryStandardCycle.equals(current.getStandardCycle())) {
                    current.setStandardCycle(accessoryStandardCycle);
                    needUpdate = true;
                }
            }

            if (needUpdate) {
                recordMapper.updateById(current);
                recalculatedCount++;
            }
        }
        if (recalculatedCount > 0) {
            log.info("配件[{}]更换记录重算完成，更新记录数={}, 刷新标准周期={}", accessoryId, recalculatedCount, refreshStandardCycle);
        }
    }

    @Override
    public List<ReplacementTimelineVO> timeline(ReplacementQueryDTO query) {
        List<ReplacementRecord> filteredRecords = list(query);
        if (filteredRecords.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, List<ReplacementRecord>> grouped = filteredRecords.stream()
                .collect(Collectors.groupingBy(ReplacementRecord::getAccessoryId));

        List<ReplacementTimelineVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<ReplacementRecord>> entry : grouped.entrySet()) {
            Long accessoryId = entry.getKey();
            List<ReplacementRecord> records = entry.getValue();

            records.sort(Comparator.comparing(ReplacementRecord::getReplaceDate).reversed());

            List<ReplacementRecord> allHistory = history(accessoryId);
            allHistory.sort(Comparator.comparing(ReplacementRecord::getReplaceDate).reversed());

            Map<Long, Integer> idToHistoryIndex = new java.util.HashMap<>();
            for (int j = 0; j < allHistory.size(); j++) {
                idToHistoryIndex.put(allHistory.get(j).getId(), j);
            }

            ReplacementTimelineVO vo = new ReplacementTimelineVO();
            vo.setAccessoryId(accessoryId);
            vo.setAccessoryName(records.get(0).getAccessoryName());
            vo.setSpecification(records.get(0).getSpecification());
            vo.setInstrumentName(records.get(0).getInstrumentName());
            vo.setImageUrl(records.get(0).getImageUrl());
            vo.setStandardCycle(records.get(0).getStandardCycle());
            vo.setRecordCount(allHistory.size());

            List<ReplacementTimelineItemVO> items = new ArrayList<>();
            int totalUsageDays = 0;

            for (int i = 0; i < records.size(); i++) {
                ReplacementRecord record = records.get(i);
                ReplacementTimelineItemVO item = new ReplacementTimelineItemVO();
                BeanUtils.copyProperties(record, item);

                Integer historyIndex = idToHistoryIndex.get(record.getId());

                if (historyIndex != null && historyIndex < allHistory.size() - 1) {
                    ReplacementRecord prevRecord = allHistory.get(historyIndex + 1);
                    long interval = ChronoUnit.DAYS.between(prevRecord.getReplaceDate(), record.getReplaceDate());
                    item.setIntervalDays((int) Math.abs(interval));
                    item.setIntervalLabel("距上次 " + Math.abs(interval) + " 天");
                    item.setIsFirst(false);
                } else if (historyIndex != null && historyIndex == allHistory.size() - 1) {
                    item.setIsFirst(true);
                    item.setIntervalLabel("首次记录");
                } else {
                    item.setIsFirst(true);
                    item.setIntervalLabel("首次记录");
                }

                if (record.getUsageDays() != null) {
                    totalUsageDays += record.getUsageDays();
                }

                items.add(item);
            }

            if (!records.isEmpty() && records.size() > 1) {
                vo.setAvgUsageDays(totalUsageDays / records.size());
            } else if (!records.isEmpty()) {
                vo.setAvgUsageDays(records.get(0).getUsageDays());
            }

            vo.setItems(items);
            result.add(vo);
        }

        result.sort((a, b) -> {
            if (a.getItems() == null || a.getItems().isEmpty()) return 1;
            if (b.getItems() == null || b.getItems().isEmpty()) return -1;
            return b.getItems().get(0).getReplaceDate().compareTo(a.getItems().get(0).getReplaceDate());
        });

        return result;
    }

    private void fillAccessoryInfo(ReplacementRecord entity, Long accessoryId) {
        if (accessoryId == null) return;
        Accessory accessory = accessoryMapper.selectById(accessoryId);
        if (accessory != null) {
            entity.setAccessoryName(accessory.getName());
            entity.setSpecification(accessory.getSpecification());
            entity.setInstrumentName(accessory.getInstrumentName());
            entity.setImageUrl(accessory.getImageUrl());

            Integer standardCycle = accessory.getStandardCycle();
            if (standardCycle == null || standardCycle <= 0) {
                standardCycle = cycleRuleService.getMatchedCycle(
                        accessory.getTypeCode(),
                        accessory.getInstrument(),
                        accessory.getSpecification()
                );
            }
            entity.setStandardCycle(standardCycle);
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

    private Accessory validateAndGetAccessory(Long accessoryId) {
        if (accessoryId == null) {
            throw new IllegalArgumentException("请选择配件");
        }
        Accessory accessory = accessoryMapper.selectById(accessoryId);
        if (accessory == null || (accessory.getDeleted() != null && accessory.getDeleted() == 1)) {
            throw new IllegalArgumentException("关联配件不存在或已删除");
        }
        return accessory;
    }

    private void validateReplaceDate(LocalDate replaceDate, Accessory accessory) {
        if (replaceDate == null) {
            throw new IllegalArgumentException("更换日期不能为空");
        }
        if (accessory.getPurchaseDate() != null && replaceDate.isBefore(accessory.getPurchaseDate())) {
            throw new IllegalArgumentException("更换日期不得早于采购日期（" + accessory.getPurchaseDate() + "）");
        }
    }

    private ReplacementResultVO buildResultVO(ReplacementRecord record, Accessory accessory) {
        ReplacementResultVO vo = new ReplacementResultVO();
        vo.setRecordId(record.getId());
        vo.setUsageDays(record.getUsageDays());
        vo.setStandardCycle(record.getStandardCycle());

        if (record.getUsageDays() != null && record.getStandardCycle() != null && record.getStandardCycle() > 0) {
            int deviation = record.getUsageDays() - record.getStandardCycle();
            vo.setDeviationDays(deviation);
            double ratio = (double) record.getUsageDays() / record.getStandardCycle();
            vo.setDeviationRatio(Math.round(ratio * 100.0) / 100.0);

            String label;
            if (ratio >= 1.2) {
                label = "超期使用（+" + (deviation) + "天）";
            } else if (ratio >= 1.0) {
                label = "已达标准周期（+" + deviation + "天）";
            } else if (ratio >= 0.8) {
                label = "接近标准周期（" + deviation + "天）";
            } else {
                label = "提前更换（" + deviation + "天）";
            }
            vo.setDeviationLabel(label);
        }

        vo.setPreviousStatus(accessory.getWornStatus());
        return vo;
    }

    private void updateAccessoryStatusToGood(Accessory accessory, ReplacementResultVO resultVO) {
        String currentStatus = accessory.getWornStatus();
        String targetStatus = "good";

        if (targetStatus.equals(currentStatus)) {
            resultVO.setStatusUpdated(false);
            resultVO.setCurrentStatus(currentStatus);
            resultVO.setStatusMessage("配件状态已是完好，无需变更");
            return;
        }

        Accessory updateEntity = new Accessory();
        updateEntity.setId(accessory.getId());
        updateEntity.setWornStatus(targetStatus);
        int updated = accessoryMapper.updateById(updateEntity);

        if (updated > 0) {
            resultVO.setStatusUpdated(true);
            resultVO.setCurrentStatus(targetStatus);
            resultVO.setStatusMessage("配件状态已联动更新为完好");
            log.info("更换记录提交成功，配件[{}]状态由[{}]更新为[{}]", accessory.getId(), currentStatus, targetStatus);
        } else {
            resultVO.setStatusUpdated(false);
            resultVO.setCurrentStatus(currentStatus);
            resultVO.setStatusMessage("配件状态更新失败");
            log.warn("更换记录提交后配件状态更新失败，accessoryId={}", accessory.getId());
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
