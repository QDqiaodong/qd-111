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
import com.instrument.vo.ReplacementTimelineItemVO;
import com.instrument.vo.ReplacementTimelineVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        boolean result = recordMapper.insert(entity) > 0;
        if (result) {
            recalculateAllUsageDays(dto.getAccessoryId());
        }
        return result;
    }

    @Override
    @Transactional
    public boolean update(ReplacementDTO dto) {
        ReplacementRecord entity = new ReplacementRecord();
        BeanUtils.copyProperties(dto, entity);
        fillAccessoryInfo(entity, dto.getAccessoryId());
        calculateUsageDays(entity, dto.getAccessoryId(), dto.getId());
        boolean result = recordMapper.updateById(entity) > 0;
        if (result) {
            recalculateAllUsageDays(dto.getAccessoryId());
        }
        return result;
    }

    @Override
    @Transactional
    public boolean remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        List<Long> accessoryIds = new ArrayList<>();
        for (Long id : ids) {
            ReplacementRecord record = recordMapper.selectById(id);
            if (record != null && record.getAccessoryId() != null) {
                if (!accessoryIds.contains(record.getAccessoryId())) {
                    accessoryIds.add(record.getAccessoryId());
                }
            }
        }
        boolean result = recordMapper.deleteBatchIds(ids) > 0;
        if (result) {
            for (Long accessoryId : accessoryIds) {
                recalculateAllUsageDays(accessoryId);
            }
        }
        return result;
    }

    private void recalculateAllUsageDays(Long accessoryId) {
        if (accessoryId == null) return;
        List<ReplacementRecord> allRecords = history(accessoryId);
        if (allRecords.isEmpty()) return;

        allRecords.sort(Comparator.comparing(ReplacementRecord::getReplaceDate));

        Accessory accessory = accessoryMapper.selectById(accessoryId);

        for (int i = 0; i < allRecords.size(); i++) {
            ReplacementRecord current = allRecords.get(i);
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
            if (!usageDays.equals(current.getUsageDays())) {
                current.setUsageDays(usageDays);
                recordMapper.updateById(current);
            }
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
