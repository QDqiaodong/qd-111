package com.instrument.util;

import com.instrument.entity.Accessory;
import com.instrument.entity.ReplacementRecord;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RiskLevelCalculator {

    public static final String RISK_LOW = "low";
    public static final String RISK_MEDIUM = "medium";
    public static final String RISK_HIGH = "high";
    public static final String RISK_CRITICAL = "critical";
    public static final String RISK_EXTREME = "extreme";

    private static final Map<String, String> RISK_LABELS = Map.of(
            RISK_LOW, "低风险",
            RISK_MEDIUM, "中风险",
            RISK_HIGH, "高风险",
            RISK_CRITICAL, "严重风险",
            RISK_EXTREME, "极端风险"
    );

    private static final Map<String, String> RISK_COLORS = Map.of(
            RISK_LOW, "#67c23a",
            RISK_MEDIUM, "#409eff",
            RISK_HIGH, "#e6a23c",
            RISK_CRITICAL, "#f56c6c",
            RISK_EXTREME, "#9c27b0"
    );

    private static final Map<String, Integer> WORN_STATUS_SCORES = Map.of(
            "good", 0,
            "slight", 20,
            "severe", 70,
            "broken", 100
    );

    public static class RiskResult {
        private String riskLevel;
        private String riskLabel;
        private String riskColor;
        private int riskScore;
        private Integer usageDays;
        private Integer daysLeft;
        private Integer cyclePercent;
        private LocalDate baselineDate;

        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getRiskLabel() { return riskLabel; }
        public void setRiskLabel(String riskLabel) { this.riskLabel = riskLabel; }
        public String getRiskColor() { return riskColor; }
        public void setRiskColor(String riskColor) { this.riskColor = riskColor; }
        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
        public Integer getUsageDays() { return usageDays; }
        public void setUsageDays(Integer usageDays) { this.usageDays = usageDays; }
        public Integer getDaysLeft() { return daysLeft; }
        public void setDaysLeft(Integer daysLeft) { this.daysLeft = daysLeft; }
        public Integer getCyclePercent() { return cyclePercent; }
        public void setCyclePercent(Integer cyclePercent) { this.cyclePercent = cyclePercent; }
        public LocalDate getBaselineDate() { return baselineDate; }
        public void setBaselineDate(LocalDate baselineDate) { this.baselineDate = baselineDate; }
    }

    public static RiskResult calculate(Accessory accessory, List<ReplacementRecord> replacementHistory) {
        RiskResult result = new RiskResult();
        LocalDate today = LocalDate.now();

        LocalDate baselineDate = null;
        if (replacementHistory != null && !replacementHistory.isEmpty()) {
            baselineDate = replacementHistory.stream()
                    .map(ReplacementRecord::getReplaceDate)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);
        }
        if (baselineDate == null) {
            baselineDate = accessory.getPurchaseDate();
        }
        result.setBaselineDate(baselineDate);

        int usageDays = 0;
        if (baselineDate != null) {
            usageDays = (int) Math.max(ChronoUnit.DAYS.between(baselineDate, today), 0);
        }
        result.setUsageDays(usageDays);

        Integer standardCycle = accessory.getStandardCycle();
        Integer daysLeft = null;
        Integer cyclePercent = 0;

        if (standardCycle != null && standardCycle > 0) {
            daysLeft = standardCycle - usageDays;
            result.setDaysLeft(daysLeft);
            cyclePercent = Math.min(Math.round((float) usageDays / standardCycle * 100), 100);
            result.setCyclePercent(cyclePercent);
        }

        int cycleScore = calculateCycleScore(cyclePercent, daysLeft, standardCycle);
        int wornScore = WORN_STATUS_SCORES.getOrDefault(accessory.getWornStatus(), 0);
        int expiredPenalty = calculateExpiredPenalty(daysLeft, standardCycle);
        int combinedScore = calculateCombinedScore(cycleScore, wornScore, expiredPenalty, accessory.getWornStatus());

        result.setRiskScore(Math.min(Math.max(combinedScore, 0), 100));
        String level = determineRiskLevel(result.getRiskScore(), accessory.getWornStatus(), daysLeft);
        result.setRiskLevel(level);
        result.setRiskLabel(RISK_LABELS.getOrDefault(level, "未知"));
        result.setRiskColor(RISK_COLORS.getOrDefault(level, "#c0c4cc"));

        return result;
    }

    private static int calculateCycleScore(int cyclePercent, Integer daysLeft, Integer standardCycle) {
        if (standardCycle == null || standardCycle <= 0) {
            return 0;
        }
        int score;
        if (cyclePercent >= 150) {
            score = 95;
        } else if (cyclePercent >= 120) {
            score = 85;
        } else if (cyclePercent >= 100) {
            score = 75;
        } else if (cyclePercent >= 90) {
            score = 60;
        } else if (cyclePercent >= 80) {
            score = 45;
        } else if (cyclePercent >= 50) {
            score = 25;
        } else if (cyclePercent >= 30) {
            score = 10;
        } else {
            score = 0;
        }
        return score;
    }

    private static int calculateExpiredPenalty(Integer daysLeft, Integer standardCycle) {
        if (daysLeft == null || standardCycle == null || standardCycle <= 0) {
            return 0;
        }
        if (daysLeft >= 0) {
            return 0;
        }
        int overdueDays = Math.abs(daysLeft);
        double overdueRatio = (double) overdueDays / standardCycle;
        if (overdueRatio >= 1.0) {
            return 25;
        } else if (overdueRatio >= 0.5) {
            return 18;
        } else if (overdueRatio >= 0.2) {
            return 12;
        } else {
            return 6;
        }
    }

    private static int calculateCombinedScore(int cycleScore, int wornScore, int expiredPenalty, String wornStatus) {
        double cycleWeight = 0.45;
        double wornWeight = 0.40;
        double penaltyWeight = 0.15;

        if ("broken".equals(wornStatus)) {
            wornWeight = 0.60;
            cycleWeight = 0.25;
        }

        double combined = cycleScore * cycleWeight + wornScore * wornWeight + expiredPenalty * penaltyWeight;

        if ("broken".equals(wornStatus)) {
            combined = Math.max(combined, 90);
        } else if ("severe".equals(wornStatus)) {
            combined = Math.max(combined, 65);
        }

        return (int) Math.round(combined);
    }

    private static String determineRiskLevel(int score, String wornStatus, Integer daysLeft) {
        if ("broken".equals(wornStatus)) {
            return RISK_EXTREME;
        }
        if ("severe".equals(wornStatus) && score >= 80) {
            return RISK_CRITICAL;
        }
        if (daysLeft != null && daysLeft < 0) {
            int overdue = Math.abs(daysLeft);
            if (overdue >= 180) {
                return RISK_CRITICAL;
            } else if (overdue >= 60) {
                return score >= 80 ? RISK_CRITICAL : RISK_HIGH;
            }
        }
        if (score >= 90) {
            return RISK_EXTREME;
        } else if (score >= 75) {
            return RISK_CRITICAL;
        } else if (score >= 55) {
            return RISK_HIGH;
        } else if (score >= 30) {
            return RISK_MEDIUM;
        } else {
            return RISK_LOW;
        }
    }

    public static String getRiskLabel(String level) {
        return RISK_LABELS.getOrDefault(level, "未知");
    }

    public static String getRiskColor(String level) {
        return RISK_COLORS.getOrDefault(level, "#c0c4cc");
    }

    public static List<String> getAllRiskLevels() {
        return Arrays.asList(RISK_LOW, RISK_MEDIUM, RISK_HIGH, RISK_CRITICAL, RISK_EXTREME);
    }

    public static Map<String, String> getAllRiskLabels() {
        return new LinkedHashMap<>(RISK_LABELS);
    }

    public static Map<String, String> getAllRiskColors() {
        return new LinkedHashMap<>(RISK_COLORS);
    }
}
