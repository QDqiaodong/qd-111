package com.instrument.service.impl;

import com.instrument.entity.Accessory;
import com.instrument.entity.AccessoryGroup;
import com.instrument.entity.ReplacementRecord;
import com.instrument.mapper.AccessoryMapper;
import com.instrument.mapper.AccessoryGroupMapper;
import com.instrument.mapper.ReplacementRecordMapper;
import com.instrument.service.DashboardService;
import com.instrument.service.DictService;
import com.instrument.util.RiskLevelCalculator;
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

        List<RiskDistributionVO> dist = riskDistribution();
        vo.setRiskDistribution(dist);
        for (RiskDistributionVO d : dist) {
            switch (d.getRiskLevel()) {
                case RiskLevelCalculator.RISK_EXTREME: vo.setExtremeRiskCount(d.getCount()); break;
                case RiskLevelCalculator.RISK_CRITICAL: vo.setCriticalRiskCount(d.getCount()); break;
                case RiskLevelCalculator.RISK_HIGH: vo.setHighRiskCount(d.getCount()); break;
                case RiskLevelCalculator.RISK_MEDIUM: vo.setMediumRiskCount(d.getCount()); break;
                case RiskLevelCalculator.RISK_LOW: vo.setLowRiskCount(d.getCount()); break;
            }
        }
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

    @Override
    public RiskTiersVO riskTiers() {
        List<RiskTierItemVO> items = buildRiskItems();

        Set<Long> classifiedIds = new HashSet<>();

        RiskTiersVO vo = new RiskTiersVO();

        List<RiskTierItemVO> broken = items.stream()
                .filter(item -> "broken".equals(item.getWornStatus()))
                .sorted(riskItemComparator())
                .collect(Collectors.toList());
        broken.forEach(item -> classifiedIds.add(item.getAccessoryId()));
        vo.setBroken(broken);

        List<RiskTierItemVO> severe = items.stream()
                .filter(item -> "severe".equals(item.getWornStatus())
                        && !classifiedIds.contains(item.getAccessoryId()))
                .sorted(riskItemComparator())
                .collect(Collectors.toList());
        severe.forEach(item -> classifiedIds.add(item.getAccessoryId()));
        vo.setSevere(severe);

        List<RiskTierItemVO> expired = items.stream()
                .filter(item -> item.getDaysLeft() != null && item.getDaysLeft() <= 0
                        && !classifiedIds.contains(item.getAccessoryId()))
                .sorted(Comparator.comparingInt(RiskTierItemVO::getDaysLeft))
                .collect(Collectors.toList());
        expired.forEach(item -> classifiedIds.add(item.getAccessoryId()));
        vo.setExpired(expired);

        List<RiskTierItemVO> upcoming = items.stream()
                .filter(item -> item.getDaysLeft() != null
                        && item.getDaysLeft() > 0
                        && item.getDaysLeft() <= 30
                        && !classifiedIds.contains(item.getAccessoryId()))
                .sorted(Comparator.comparingInt(RiskTierItemVO::getDaysLeft))
                .collect(Collectors.toList());
        vo.setUpcoming(upcoming);

        return vo;
    }

    @Override
    public List<RiskTierItemVO> riskTier(String tier) {
        RiskTiersVO tiers = riskTiers();
        if ("expired".equals(tier)) {
            return tiers.getExpired();
        }
        if ("broken".equals(tier)) {
            return tiers.getBroken();
        }
        if ("severe".equals(tier)) {
            return tiers.getSevere();
        }
        if ("upcoming".equals(tier)) {
            return tiers.getUpcoming();
        }
        return Collections.emptyList();
    }

    @Override
    public WornHeatmapVO wornHeatmap() {
        List<DictVO> instruments = dictService.instruments();
        List<DictVO> accessoryTypes = dictService.accessoryTypes();
        List<DictVO> wornStatuses = dictService.wornStatuses();
        List<Accessory> all = accessoryMapper.selectList(null);

        WornHeatmapVO vo = new WornHeatmapVO();

        vo.setInstruments(instruments.stream()
                .map(d -> new WornHeatmapVO.InstrumentHeatmapVO(d.getCode(), d.getLabel()))
                .collect(Collectors.toList()));

        vo.setAccessoryTypes(accessoryTypes.stream()
                .map(d -> new WornHeatmapVO.AccessoryTypeHeatmapVO(d.getCode(), d.getLabel()))
                .collect(Collectors.toList()));

        vo.setLegends(wornStatuses.stream()
                .map(d -> new WornHeatmapVO.WornStatusLegendVO(
                        d.getCode(), d.getLabel(), WORN_COLORS.getOrDefault(d.getCode(), "#909399")))
                .collect(Collectors.toList()));

        Map<String, Map<String, List<Accessory>>> grouped = all.stream()
                .filter(a -> a.getInstrument() != null && a.getTypeCode() != null)
                .collect(Collectors.groupingBy(
                        Accessory::getInstrument,
                        Collectors.groupingBy(Accessory::getTypeCode)
                ));

        List<WornHeatmapVO.HeatmapCellVO> cells = new ArrayList<>();
        for (DictVO inst : instruments) {
            for (DictVO type : accessoryTypes) {
                WornHeatmapVO.HeatmapCellVO cell = new WornHeatmapVO.HeatmapCellVO();
                cell.setInstrumentCode(inst.getCode());
                cell.setInstrumentName(inst.getLabel());
                cell.setTypeCode(type.getCode());
                cell.setTypeName(type.getLabel());

                List<Accessory> list = grouped.containsKey(inst.getCode())
                        ? grouped.get(inst.getCode()).getOrDefault(type.getCode(), Collections.emptyList())
                        : Collections.emptyList();

                long goodCount = list.stream().filter(a -> "good".equals(a.getWornStatus())).count();
                long slightCount = list.stream().filter(a -> "slight".equals(a.getWornStatus())).count();
                long severeCount = list.stream().filter(a -> "severe".equals(a.getWornStatus())).count();
                long brokenCount = list.stream().filter(a -> "broken".equals(a.getWornStatus())).count();

                cell.setGoodCount(goodCount);
                cell.setSlightCount(slightCount);
                cell.setSevereCount(severeCount);
                cell.setBrokenCount(brokenCount);
                cell.setTotal(list.size());

                cells.add(cell);
            }
        }
        vo.setCells(cells);

        return vo;
    }

    private List<RiskTierItemVO> buildRiskItems() {
        List<Accessory> accessories = accessoryMapper.selectList(null);
        if (accessories.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<ReplacementRecord>> historyMap = recordMapper.selectList(null).stream()
                .filter(record -> record.getAccessoryId() != null)
                .collect(Collectors.groupingBy(ReplacementRecord::getAccessoryId));

        List<RiskTierItemVO> items = new ArrayList<>();
        for (Accessory accessory : accessories) {
            RiskTierItemVO item = new RiskTierItemVO();
            item.setId(accessory.getId());
            item.setAccessoryId(accessory.getId());
            item.setName(accessory.getName());
            item.setTypeCode(accessory.getTypeCode());
            item.setTypeName(accessory.getTypeName());
            item.setSpecification(accessory.getSpecification());
            item.setInstrument(accessory.getInstrument());
            item.setInstrumentName(accessory.getInstrumentName());
            item.setWornStatus(accessory.getWornStatus());

            List<ReplacementRecord> history = historyMap.getOrDefault(accessory.getId(), Collections.emptyList());
            RiskLevelCalculator.RiskResult riskResult = RiskLevelCalculator.calculate(accessory, history);

            item.setUsageDays(riskResult.getUsageDays());
            item.setDaysLeft(riskResult.getDaysLeft());
            item.setCyclePercent(riskResult.getCyclePercent());
            item.setRiskLevel(riskResult.getRiskLevel());
            item.setRiskLabel(riskResult.getRiskLabel());
            item.setRiskColor(riskResult.getRiskColor());
            item.setRiskScore(riskResult.getRiskScore());

            items.add(item);
        }
        return items;
    }

    @Override
    public List<RiskDistributionVO> riskDistribution() {
        List<RiskTierItemVO> items = buildRiskItems();
        long total = items.size();

        Map<String, Long> countMap = items.stream()
                .collect(Collectors.groupingBy(
                        RiskTierItemVO::getRiskLevel,
                        Collectors.counting()
                ));

        List<String> orderedLevels = RiskLevelCalculator.getAllRiskLevels();
        Map<String, String> labelMap = RiskLevelCalculator.getAllRiskLabels();
        Map<String, String> colorMap = RiskLevelCalculator.getAllRiskColors();

        List<RiskDistributionVO> result = new ArrayList<>();
        for (String level : orderedLevels) {
            RiskDistributionVO vo = new RiskDistributionVO();
            vo.setRiskLevel(level);
            vo.setRiskLabel(labelMap.get(level));
            vo.setRiskColor(colorMap.get(level));
            long count = countMap.getOrDefault(level, 0L);
            vo.setCount(count);
            vo.setPercent(total == 0 ? 0 : BigDecimal.valueOf(count * 100.0 / total)
                    .setScale(0, RoundingMode.HALF_UP).intValue());
            result.add(vo);
        }
        return result;
    }

    private Comparator<RiskTierItemVO> riskItemComparator() {
        return Comparator
                .comparing(RiskTierItemVO::getRiskScore, Comparator.reverseOrder())
                .thenComparing((RiskTierItemVO item) -> item.getDaysLeft() == null ? Integer.MAX_VALUE : item.getDaysLeft())
                .thenComparing(RiskTierItemVO::getUsageDays, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
