package com.instrument.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.instrument.entity.Accessory;
import com.instrument.entity.ReplacementRecord;
import com.instrument.mapper.AccessoryMapper;
import com.instrument.mapper.ReplacementRecordMapper;
import com.instrument.service.DictService;
import com.instrument.service.MaintenancePlanService;
import com.instrument.util.RiskLevelCalculator;
import com.instrument.vo.InstrumentMaintenancePlanVO;
import com.instrument.vo.MaintenancePlanItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenancePlanServiceImpl implements MaintenancePlanService {

    private final AccessoryMapper accessoryMapper;
    private final ReplacementRecordMapper replacementRecordMapper;
    private final DictService dictService;

    private static final String PLAN_STATUS_URGENT = "urgent";
    private static final String PLAN_STATUS_ATTENTION = "attention";
    private static final String PLAN_STATUS_NORMAL = "normal";

    private static final Map<String, String> PLAN_STATUS_LABELS = Map.of(
            PLAN_STATUS_URGENT, "需立即处理",
            PLAN_STATUS_ATTENTION, "需关注",
            PLAN_STATUS_NORMAL, "正常"
    );

    private static final Map<String, String> PLAN_STATUS_COLORS = Map.of(
            PLAN_STATUS_URGENT, "#f56c6c",
            PLAN_STATUS_ATTENTION, "#e6a23c",
            PLAN_STATUS_NORMAL, "#67c23a"
    );

    private static final String ACTION_REPLACE = "replace";
    private static final String ACTION_CHECK = "check";
    private static final String ACTION_CLEAN = "clean";
    private static final String ACTION_NONE = "none";

    @Override
    public List<InstrumentMaintenancePlanVO> generatePlans() {
        List<Accessory> allAccessories = accessoryMapper.selectList(null);
        if (allAccessories.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<ReplacementRecord>> historyMap = replacementRecordMapper.selectList(null).stream()
                .filter(r -> r.getAccessoryId() != null)
                .collect(Collectors.groupingBy(ReplacementRecord::getAccessoryId));

        Map<String, List<Accessory>> groupedByInstrument = allAccessories.stream()
                .filter(a -> a.getInstrument() != null)
                .collect(Collectors.groupingBy(Accessory::getInstrument));

        List<InstrumentMaintenancePlanVO> result = new ArrayList<>();
        for (Map.Entry<String, List<Accessory>> entry : groupedByInstrument.entrySet()) {
            String instrument = entry.getKey();
            List<Accessory> instrumentAccessories = entry.getValue();
            InstrumentMaintenancePlanVO planVO = buildInstrumentPlan(instrument, instrumentAccessories, historyMap);
            result.add(planVO);
        }

        result.sort(Comparator
                .comparing((InstrumentMaintenancePlanVO p) -> {
                    if (PLAN_STATUS_URGENT.equals(p.getOverallStatus())) return 0;
                    if (PLAN_STATUS_ATTENTION.equals(p.getOverallStatus())) return 1;
                    return 2;
                })
                .thenComparing(InstrumentMaintenancePlanVO::getUrgentCount, Comparator.reverseOrder())
                .thenComparing(InstrumentMaintenancePlanVO::getMaxRiskScore, Comparator.reverseOrder()));

        return result;
    }

    @Override
    public InstrumentMaintenancePlanVO generatePlanByInstrument(String instrument) {
        if (instrument == null || instrument.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<Accessory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Accessory::getInstrument, instrument);
        List<Accessory> accessories = accessoryMapper.selectList(wrapper);
        if (accessories.isEmpty()) {
            return null;
        }

        Map<Long, List<ReplacementRecord>> historyMap = replacementRecordMapper.selectList(
                new LambdaQueryWrapper<ReplacementRecord>().in(
                        ReplacementRecord::getAccessoryId,
                        accessories.stream().map(Accessory::getId).collect(Collectors.toList())
                )
        ).stream().filter(r -> r.getAccessoryId() != null)
                .collect(Collectors.groupingBy(ReplacementRecord::getAccessoryId));

        return buildInstrumentPlan(instrument, accessories, historyMap);
    }

    @Override
    public List<MaintenancePlanItemVO> listPlanItems(String instrument) {
        InstrumentMaintenancePlanVO plan = generatePlanByInstrument(instrument);
        return plan != null ? plan.getItems() : Collections.emptyList();
    }

    private InstrumentMaintenancePlanVO buildInstrumentPlan(
            String instrument,
            List<Accessory> accessories,
            Map<Long, List<ReplacementRecord>> historyMap) {

        InstrumentMaintenancePlanVO vo = new InstrumentMaintenancePlanVO();
        vo.setInstrument(instrument);
        vo.setInstrumentName(dictService.getInstrumentLabel(instrument));

        List<MaintenancePlanItemVO> items = new ArrayList<>();
        int urgentCount = 0;
        int attentionCount = 0;
        int normalCount = 0;
        int minDaysLeft = Integer.MAX_VALUE;
        int maxRiskScore = 0;

        for (Accessory acc : accessories) {
            MaintenancePlanItemVO item = buildPlanItem(acc, historyMap.getOrDefault(acc.getId(), Collections.emptyList()));
            items.add(item);

            if (PLAN_STATUS_URGENT.equals(item.getPlanStatus())) {
                urgentCount++;
            } else if (PLAN_STATUS_ATTENTION.equals(item.getPlanStatus())) {
                attentionCount++;
            } else {
                normalCount++;
            }

            if (item.getDaysLeft() != null && item.getDaysLeft() < minDaysLeft) {
                minDaysLeft = item.getDaysLeft();
            }
            if (item.getRiskScore() != null && item.getRiskScore() > maxRiskScore) {
                maxRiskScore = item.getRiskScore();
            }
        }

        items.sort(Comparator
                .comparing((MaintenancePlanItemVO item) -> {
                    if (PLAN_STATUS_URGENT.equals(item.getPlanStatus())) return 0;
                    if (PLAN_STATUS_ATTENTION.equals(item.getPlanStatus())) return 1;
                    return 2;
                })
                .thenComparing(item -> item.getDaysLeft() == null ? Integer.MAX_VALUE : item.getDaysLeft())
                .thenComparing(MaintenancePlanItemVO::getRiskScore, Comparator.reverseOrder()));

        vo.setTotalCount(items.size());
        vo.setUrgentCount(urgentCount);
        vo.setAttentionCount(attentionCount);
        vo.setNormalCount(normalCount);
        vo.setMinDaysLeft(minDaysLeft == Integer.MAX_VALUE ? null : minDaysLeft);
        vo.setMaxRiskScore(maxRiskScore);
        vo.setItems(items);

        String overallStatus;
        if (urgentCount > 0) {
            overallStatus = PLAN_STATUS_URGENT;
        } else if (attentionCount > 0) {
            overallStatus = PLAN_STATUS_ATTENTION;
        } else {
            overallStatus = PLAN_STATUS_NORMAL;
        }
        vo.setOverallStatus(overallStatus);
        vo.setOverallStatusLabel(PLAN_STATUS_LABELS.get(overallStatus));
        vo.setOverallStatusColor(PLAN_STATUS_COLORS.get(overallStatus));

        return vo;
    }

    private MaintenancePlanItemVO buildPlanItem(Accessory accessory, List<ReplacementRecord> history) {
        MaintenancePlanItemVO vo = new MaintenancePlanItemVO();
        vo.setAccessoryId(accessory.getId());
        vo.setAccessoryName(accessory.getName());
        vo.setTypeCode(accessory.getTypeCode());
        vo.setTypeName(accessory.getTypeName());
        vo.setSpecification(accessory.getSpecification());
        vo.setImageUrl(accessory.getImageUrl());
        vo.setStandardCycle(accessory.getStandardCycle());

        LocalDate baselineDate = null;
        if (history != null && !history.isEmpty()) {
            baselineDate = history.stream()
                    .map(ReplacementRecord::getReplaceDate)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);
        }
        if (baselineDate == null) {
            baselineDate = accessory.getPurchaseDate();
        }
        vo.setLastActionDate(baselineDate);

        LocalDate today = LocalDate.now();
        int usageDays = 0;
        if (baselineDate != null) {
            usageDays = (int) Math.max(ChronoUnit.DAYS.between(baselineDate, today), 0);
        }
        vo.setUsageDays(usageDays);

        Integer standardCycle = accessory.getStandardCycle();
        Integer daysLeft = null;
        Integer cyclePercent = 0;
        LocalDate expectedNextDate = null;

        if (standardCycle != null && standardCycle > 0) {
            daysLeft = standardCycle - usageDays;
            cyclePercent = Math.min(Math.round((float) usageDays / standardCycle * 100), 100);
            if (baselineDate != null) {
                expectedNextDate = baselineDate.plusDays(standardCycle);
            }
        }
        vo.setDaysLeft(daysLeft);
        vo.setCyclePercent(cyclePercent);
        vo.setExpectedNextDate(expectedNextDate);

        vo.setWornStatus(accessory.getWornStatus());
        vo.setWornStatusLabel(dictService.getWornStatusLabel(accessory.getWornStatus()));

        RiskLevelCalculator.RiskResult riskResult = RiskLevelCalculator.calculate(accessory, history);
        vo.setRiskLevel(riskResult.getRiskLevel());
        vo.setRiskLabel(riskResult.getRiskLabel());
        vo.setRiskColor(riskResult.getRiskColor());
        vo.setRiskScore(riskResult.getRiskScore());

        String planStatus = determinePlanStatus(daysLeft, accessory.getWornStatus(), riskResult.getRiskLevel());
        vo.setPlanStatus(planStatus);
        vo.setPlanStatusLabel(PLAN_STATUS_LABELS.get(planStatus));
        vo.setPlanStatusColor(PLAN_STATUS_COLORS.get(planStatus));

        String actionType = determineActionType(planStatus, accessory.getWornStatus(), cyclePercent, accessory.getTypeCode());
        vo.setActionType(actionType);
        vo.setActionSuggestion(buildActionSuggestion(actionType, daysLeft, accessory.getWornStatus(), vo.getTypeName()));

        return vo;
    }

    private String determinePlanStatus(Integer daysLeft, String wornStatus, String riskLevel) {
        if ("broken".equals(wornStatus)) {
            return PLAN_STATUS_URGENT;
        }
        if ("severe".equals(wornStatus)) {
            return PLAN_STATUS_URGENT;
        }
        if (RiskLevelCalculator.RISK_CRITICAL.equals(riskLevel) || RiskLevelCalculator.RISK_EXTREME.equals(riskLevel)) {
            return PLAN_STATUS_URGENT;
        }
        if (daysLeft != null && daysLeft <= 0) {
            return PLAN_STATUS_URGENT;
        }
        if (RiskLevelCalculator.RISK_HIGH.equals(riskLevel)) {
            return PLAN_STATUS_ATTENTION;
        }
        if (daysLeft != null && daysLeft <= 30) {
            return PLAN_STATUS_ATTENTION;
        }
        if ("slight".equals(wornStatus)) {
            return PLAN_STATUS_ATTENTION;
        }
        return PLAN_STATUS_NORMAL;
    }

    private String determineActionType(String planStatus, String wornStatus, int cyclePercent, String typeCode) {
        if (PLAN_STATUS_URGENT.equals(planStatus)) {
            if ("broken".equals(wornStatus)) {
                return ACTION_REPLACE;
            }
            if ("severe".equals(wornStatus) || cyclePercent >= 100) {
                return ACTION_REPLACE;
            }
            return ACTION_CHECK;
        }
        if (PLAN_STATUS_ATTENTION.equals(planStatus)) {
            if ("cleaner".equals(typeCode) || "rosin".equals(typeCode)) {
                return ACTION_CLEAN;
            }
            return ACTION_CHECK;
        }
        return ACTION_NONE;
    }

    private String buildActionSuggestion(String actionType, Integer daysLeft, String wornStatus, String typeName) {
        switch (actionType) {
            case ACTION_REPLACE:
                if ("broken".equals(wornStatus)) {
                    return "已损坏，建议立即更换";
                }
                if (daysLeft != null && daysLeft < 0) {
                    return "已超期" + Math.abs(daysLeft) + "天，建议尽快更换" + typeName;
                }
                return "损耗严重，建议立即更换" + typeName;
            case ACTION_CHECK:
                if (daysLeft != null && daysLeft >= 0 && daysLeft <= 30) {
                    return "剩余" + daysLeft + "天到期，建议近期检查";
                }
                if ("slight".equals(wornStatus)) {
                    return "有轻微磨损，建议关注使用状态";
                }
                return "建议进行常规检查";
            case ACTION_CLEAN:
                return "建议进行清洁/养护操作";
            case ACTION_NONE:
            default:
                return "状态正常，按计划保养即可";
        }
    }
}
