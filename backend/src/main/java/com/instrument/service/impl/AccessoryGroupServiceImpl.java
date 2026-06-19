package com.instrument.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.instrument.entity.Accessory;
import com.instrument.entity.AccessoryGroup;
import com.instrument.entity.ReplacementRecord;
import com.instrument.mapper.AccessoryGroupMapper;
import com.instrument.mapper.AccessoryMapper;
import com.instrument.mapper.ReplacementRecordMapper;
import com.instrument.service.AccessoryGroupService;
import com.instrument.vo.GroupCapacityStatsVO;
import com.instrument.vo.GroupHealthScoreVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccessoryGroupServiceImpl implements AccessoryGroupService {

    private final AccessoryGroupMapper groupMapper;
    private final AccessoryMapper accessoryMapper;
    private final ReplacementRecordMapper recordMapper;

    private static final int SEVERE_DEDUCTION = 5;
    private static final int SEVERE_MAX = 30;
    private static final int BROKEN_DEDUCTION = 10;
    private static final int BROKEN_MAX = 40;
    private static final int OVERDUE_DEDUCTION = 8;
    private static final int OVERDUE_MAX = 30;
    private static final int RECENT_THRESHOLD = 2;
    private static final int RECENT_DEDUCTION = 3;
    private static final int RECENT_MAX = 15;
    private static final int RECENT_DAYS = 30;

    @Override
    public List<AccessoryGroup> list() {
        List<AccessoryGroup> groups = groupMapper.selectList(null);
        groups.sort(Comparator
                .comparing(AccessoryGroup::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AccessoryGroup::getId));
        return groups;
    }

    @Override
    public AccessoryGroup getById(Long id) {
        return groupMapper.selectById(id);
    }

    @Override
    @Transactional
    public AccessoryGroup add(AccessoryGroup group) {
        if (group.getSortOrder() == null) {
            group.setSortOrder(0);
        }
        int rows = groupMapper.insert(group);
        if (rows > 0) {
            normalizeSortOrders();
            return groupMapper.selectById(group.getId());
        }
        return null;
    }

    @Override
    @Transactional
    public boolean update(AccessoryGroup group) {
        boolean ok = groupMapper.updateById(group) > 0;
        if (ok) {
            normalizeSortOrders();
        }
        return ok;
    }

    @Override
    @Transactional
    public boolean remove(Long id) {
        long accessoryCount = accessoryMapper.selectCount(
                new LambdaQueryWrapper<Accessory>().eq(Accessory::getGroupId, id)
        );
        if (accessoryCount > 0) {
            throw new IllegalArgumentException("该分组下仍有 " + accessoryCount + " 个配件，请先移除或变更配件的分组后再删除");
        }
        boolean ok = groupMapper.deleteById(id) > 0;
        if (ok) {
            normalizeSortOrders();
        }
        return ok;
    }

    private void normalizeSortOrders() {
        List<AccessoryGroup> groups = groupMapper.selectList(null);
        groups.sort(Comparator
                .comparing(AccessoryGroup::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AccessoryGroup::getId));
        int order = 1;
        List<AccessoryGroup> changed = new ArrayList<>();
        for (AccessoryGroup group : groups) {
            Integer current = group.getSortOrder();
            if (current == null || !current.equals(order)) {
                group.setSortOrder(order);
                changed.add(group);
            }
            order++;
        }
        for (AccessoryGroup group : changed) {
            groupMapper.updateById(group);
        }
    }

    @Override
    public GroupHealthScoreVO healthScore(Long groupId) {
        AccessoryGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            return null;
        }
        return calculateHealthScore(group);
    }

    @Override
    public List<GroupHealthScoreVO> healthScores() {
        List<AccessoryGroup> groups = list();
        return groups.stream().map(this::calculateHealthScore).collect(Collectors.toList());
    }

    @Override
    public List<GroupCapacityStatsVO> capacityStats() {
        List<AccessoryGroup> groups = list();
        return groups.stream().map(this::calculateCapacityStats).collect(Collectors.toList());
    }

    private GroupCapacityStatsVO calculateCapacityStats(AccessoryGroup group) {
        Long groupId = group.getId();
        LocalDate today = LocalDate.now();

        List<Accessory> accessories = accessoryMapper.selectList(
                new LambdaQueryWrapper<Accessory>().eq(Accessory::getGroupId, groupId)
        );

        int totalCount = accessories.size();
        int severeCount = (int) accessories.stream()
                .filter(a -> "severe".equals(a.getWornStatus())).count();
        int brokenCount = (int) accessories.stream()
                .filter(a -> "broken".equals(a.getWornStatus())).count();

        int maxUnreplacedDays = 0;
        LocalDate lastReplacementDate = null;

        List<Long> accessoryIds = accessories.stream()
                .map(Accessory::getId).collect(Collectors.toList());

        if (!accessoryIds.isEmpty()) {
            List<ReplacementRecord> allRecords = recordMapper.selectList(
                    new LambdaQueryWrapper<ReplacementRecord>()
                            .in(ReplacementRecord::getAccessoryId, accessoryIds)
                            .orderByDesc(ReplacementRecord::getReplaceDate)
            );

            Map<Long, LocalDate> lastReplaceMap = new HashMap<>();
            for (ReplacementRecord record : allRecords) {
                if (!lastReplaceMap.containsKey(record.getAccessoryId())) {
                    lastReplaceMap.put(record.getAccessoryId(), record.getReplaceDate());
                }
            }

            for (Accessory acc : accessories) {
                LocalDate lastDate = lastReplaceMap.getOrDefault(acc.getId(), acc.getPurchaseDate());
                if (lastDate != null) {
                    long days = ChronoUnit.DAYS.between(lastDate, today);
                    if (days > maxUnreplacedDays) {
                        maxUnreplacedDays = (int) days;
                    }
                }
            }

            if (!allRecords.isEmpty()) {
                lastReplacementDate = allRecords.get(0).getReplaceDate();
            } else {
                for (Accessory acc : accessories) {
                    if (acc.getPurchaseDate() != null) {
                        if (lastReplacementDate == null || acc.getPurchaseDate().isAfter(lastReplacementDate)) {
                            lastReplacementDate = acc.getPurchaseDate();
                        }
                    }
                }
            }
        }

        GroupCapacityStatsVO vo = new GroupCapacityStatsVO();
        vo.setGroupId(groupId);
        vo.setGroupName(group.getName());
        vo.setSortOrder(group.getSortOrder());
        vo.setTotalCount(totalCount);
        vo.setSevereCount(severeCount);
        vo.setBrokenCount(brokenCount);
        vo.setMaxUnreplacedDays(maxUnreplacedDays);
        vo.setLastReplacementDate(lastReplacementDate);

        return vo;
    }

    private GroupHealthScoreVO calculateHealthScore(AccessoryGroup group) {
        Long groupId = group.getId();

        List<Accessory> accessories = accessoryMapper.selectList(
                new LambdaQueryWrapper<Accessory>().eq(Accessory::getGroupId, groupId)
        );

        int totalCount = accessories.size();
        int severeCount = (int) accessories.stream().filter(a -> "severe".equals(a.getWornStatus())).count();
        int brokenCount = (int) accessories.stream().filter(a -> "broken".equals(a.getWornStatus())).count();

        LocalDate today = LocalDate.now();
        int overdueCount = 0;
        for (Accessory acc : accessories) {
            if (acc.getPurchaseDate() == null || acc.getStandardCycle() == null || acc.getStandardCycle() <= 0) {
                continue;
            }
            List<ReplacementRecord> records = recordMapper.selectList(
                    new LambdaQueryWrapper<ReplacementRecord>()
                            .eq(ReplacementRecord::getAccessoryId, acc.getId())
                            .orderByDesc(ReplacementRecord::getReplaceDate)
                            .last("LIMIT 1")
            );
            LocalDate lastDate = records.isEmpty() ? acc.getPurchaseDate() : records.get(0).getReplaceDate();
            if (lastDate == null) continue;
            long usageDays = ChronoUnit.DAYS.between(lastDate, today);
            if (usageDays > acc.getStandardCycle()) {
                overdueCount++;
            }
        }

        LocalDate recentStart = today.minusDays(RECENT_DAYS);
        List<Long> accessoryIds = accessories.stream().map(Accessory::getId).collect(Collectors.toList());
        int recentReplacementCount = 0;
        if (!accessoryIds.isEmpty()) {
            recentReplacementCount = recordMapper.selectCount(
                    new LambdaQueryWrapper<ReplacementRecord>()
                            .in(ReplacementRecord::getAccessoryId, accessoryIds)
                            .ge(ReplacementRecord::getReplaceDate, recentStart)
            ).intValue();
        }

        int severeDeduction = Math.min(severeCount * SEVERE_DEDUCTION, SEVERE_MAX);
        int brokenDeduction = Math.min(brokenCount * BROKEN_DEDUCTION, BROKEN_MAX);
        int overdueDeduction = Math.min(overdueCount * OVERDUE_DEDUCTION, OVERDUE_MAX);
        int recentDeduction = recentReplacementCount > RECENT_THRESHOLD
                ? Math.min((recentReplacementCount - RECENT_THRESHOLD) * RECENT_DEDUCTION, RECENT_MAX)
                : 0;

        int totalDeduction = severeDeduction + brokenDeduction + overdueDeduction + recentDeduction;
        int score = Math.max(0, 100 - totalDeduction);

        String level;
        String color;
        if (score >= 80) {
            level = "健康";
            color = "#67c23a";
        } else if (score >= 60) {
            level = "一般";
            color = "#e6a23c";
        } else if (score >= 40) {
            level = "较差";
            color = "#f56c6c";
        } else {
            level = "危险";
            color = "#c45656";
        }

        GroupHealthScoreVO vo = new GroupHealthScoreVO();
        vo.setGroupId(groupId);
        vo.setGroupName(group.getName());
        vo.setScore(score);
        vo.setLevel(level);
        vo.setColor(color);
        vo.setTotalCount(totalCount);
        vo.setSevereCount(severeCount);
        vo.setBrokenCount(brokenCount);
        vo.setOverdueCount(overdueCount);
        vo.setRecentReplacementCount(recentReplacementCount);

        List<GroupHealthScoreVO.ScoreDetailItem> details = new ArrayList<>();
        details.add(buildDetail("severe", "严重损耗", severeCount, severeDeduction, SEVERE_MAX));
        details.add(buildDetail("broken", "已损坏/断裂", brokenCount, brokenDeduction, BROKEN_MAX));
        details.add(buildDetail("overdue", "超期未更换", overdueCount, overdueDeduction, OVERDUE_MAX));
        details.add(buildDetail("recent_replacement", "近期更换频繁", recentReplacementCount, recentDeduction, RECENT_MAX));
        vo.setDetails(details);

        return vo;
    }

    private GroupHealthScoreVO.ScoreDetailItem buildDetail(String factor, String label, int count, int deduction, int maxDeduction) {
        GroupHealthScoreVO.ScoreDetailItem item = new GroupHealthScoreVO.ScoreDetailItem();
        item.setFactor(factor);
        item.setLabel(label);
        item.setCount(count);
        item.setDeduction(deduction);
        item.setMaxDeduction(maxDeduction);
        return item;
    }
}
