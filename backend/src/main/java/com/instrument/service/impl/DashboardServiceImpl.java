package com.instrument.service.impl;

import com.instrument.entity.Accessory;
import com.instrument.entity.AccessoryGroup;
import com.instrument.entity.ReplacementRecord;
import com.instrument.mapper.AccessoryMapper;
import com.instrument.mapper.AccessoryGroupMapper;
import com.instrument.mapper.ReplacementRecordMapper;
import com.instrument.service.DashboardService;
import com.instrument.service.DictService;
import com.instrument.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AccessoryMapper accessoryMapper;
    private final AccessoryGroupMapper groupMapper;
    private final ReplacementRecordMapper recordMapper;
    private final DictService dictService;

    private static final Map<String, String> WORN_COLORS = Map.of(
            "good", "#67c23a",
            "slight", "#e6a23c",
            "severe", "#f56c6c",
            "broken", "#909399"
    );

    @Override
    public DashboardStatsVO stats() {
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setTotalAccessories(accessoryMapper.selectCount(null));
        vo.setWornCount(accessoryMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Accessory>()
                        .in(Accessory::getWornStatus, "severe", "broken")
        ));
        YearMonth currentMonth = YearMonth.now();
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();
        vo.setMonthReplacements(recordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReplacementRecord>()
                        .ge(ReplacementRecord::getReplaceDate, start)
                        .le(ReplacementRecord::getReplaceDate, end)
        ));
        vo.setGroupCount(groupMapper.selectCount(null));
        return vo;
    }

    @Override
    public List<UpcomingReplacementVO> upcomingReplacements() {
        List<Accessory> accessories = accessoryMapper.selectList(null);
        if (accessories.isEmpty()) return Collections.emptyList();

        Map<Long, List<ReplacementRecord>> historyMap = recordMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(ReplacementRecord::getAccessoryId));

        List<UpcomingReplacementVO> result = new ArrayList<>();
        for (Accessory acc : accessories) {
            if (acc.getStandardCycle() == null || acc.getStandardCycle() <= 0) continue;
            List<ReplacementRecord> history = historyMap.getOrDefault(acc.getId(), Collections.emptyList());
            LocalDate lastDate = history.stream()
                    .map(ReplacementRecord::getReplaceDate)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(acc.getPurchaseDate());
            if (lastDate == null) continue;
            int usageDays = (int) ChronoUnit.DAYS.between(lastDate, LocalDate.now());
            int daysLeft = acc.getStandardCycle() - usageDays;
            if (daysLeft <= 60) {
                UpcomingReplacementVO vo = new UpcomingReplacementVO();
                vo.setAccessoryId(acc.getId());
                vo.setName(acc.getName());
                vo.setSpecification(acc.getSpecification());
                vo.setInstrument(acc.getInstrument());
                vo.setInstrumentName(acc.getInstrumentName());
                vo.setLastReplaceDate(lastDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                vo.setUsageDays(Math.max(usageDays, 0));
                vo.setDaysLeft(daysLeft);
                result.add(vo);
            }
        }
        result.sort(Comparator.comparingInt(UpcomingReplacementVO::getDaysLeft));
        return result.stream().limit(10).collect(Collectors.toList());
    }

    @Override
    public List<WornDistributionVO> wornDistribution() {
        List<DictVO> wornStatuses = dictService.wornStatuses();
        List<Accessory> all = accessoryMapper.selectList(null);
        long total = all.size();
        Map<String, Long> countMap = all.stream()
                .collect(Collectors.groupingBy(Accessory::getWornStatus, Collectors.counting()));

        return wornStatuses.stream().map(status -> {
            WornDistributionVO vo = new WornDistributionVO();
            vo.setStatus(status.getCode());
            vo.setLabel(status.getLabel());
            long count = countMap.getOrDefault(status.getCode(), 0L);
            vo.setCount(count);
            vo.setPercent(total == 0 ? 0 : BigDecimal.valueOf(count * 100.0 / total)
                    .setScale(0, RoundingMode.HALF_UP).intValue());
            vo.setColor(WORN_COLORS.getOrDefault(status.getCode(), "#909399"));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<GroupDistributionVO> groupDistribution() {
        List<AccessoryGroup> groups = groupMapper.selectList(null);
        List<Accessory> all = accessoryMapper.selectList(null);
        long total = all.size();
        Map<Long, Long> countMap = all.stream()
                .filter(a -> a.getGroupId() != null)
                .collect(Collectors.groupingBy(Accessory::getGroupId, Collectors.counting()));

        return groups.stream().map(group -> {
            GroupDistributionVO vo = new GroupDistributionVO();
            vo.setId(group.getId());
            vo.setName(group.getName());
            long count = countMap.getOrDefault(group.getId(), 0L);
            vo.setCount(count);
            vo.setPercent(total == 0 ? 0 : BigDecimal.valueOf(count * 100.0 / total)
                    .setScale(0, RoundingMode.HALF_UP).intValue());
            return vo;
        }).collect(Collectors.toList());
    }
}
