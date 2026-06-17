package com.instrument.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.common.PageResult;
import com.instrument.dto.CycleRuleQueryDTO;
import com.instrument.dto.StandardCycleRuleDTO;
import com.instrument.entity.StandardCycleRule;
import com.instrument.mapper.StandardCycleRuleMapper;
import com.instrument.service.DictService;
import com.instrument.service.StandardCycleRuleService;
import com.instrument.vo.CycleRuleMatchVO;
import com.instrument.vo.StandardCycleRuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandardCycleRuleServiceImpl implements StandardCycleRuleService {

    private final StandardCycleRuleMapper ruleMapper;
    private final DictService dictService;

    @Override
    public PageResult<StandardCycleRule> page(CycleRuleQueryDTO query) {
        LambdaQueryWrapper<StandardCycleRule> wrapper = buildWrapper(query);
        wrapper.orderByDesc(StandardCycleRule::getPriority)
                .orderByDesc(StandardCycleRule::getCreateTime);
        IPage<StandardCycleRule> page = ruleMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    @Cacheable(value = "cycleRule", key = "'list' + #query.hashCode()", unless = "#result == null")
    public List<StandardCycleRule> list(CycleRuleQueryDTO query) {
        LambdaQueryWrapper<StandardCycleRule> wrapper = buildWrapper(query);
        wrapper.orderByDesc(StandardCycleRule::getPriority)
                .orderByDesc(StandardCycleRule::getCreateTime);
        return ruleMapper.selectList(wrapper);
    }

    @Override
    @Cacheable(value = "cycleRule", key = "#id", unless = "#result == null")
    public StandardCycleRule getById(Long id) {
        return ruleMapper.selectById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "cycleRule", allEntries = true)
    public boolean add(StandardCycleRuleDTO dto) {
        validateStandardCycle(dto.getStandardCycle());
        StandardCycleRule entity = new StandardCycleRule();
        BeanUtils.copyProperties(dto, entity);
        fillDictFields(entity);
        if (entity.getPriority() == null) {
            entity.setPriority(0);
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        return ruleMapper.insert(entity) > 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = "cycleRule", allEntries = true)
    public boolean update(StandardCycleRuleDTO dto) {
        validateStandardCycle(dto.getStandardCycle());
        StandardCycleRule entity = new StandardCycleRule();
        BeanUtils.copyProperties(dto, entity);
        fillDictFields(entity);
        return ruleMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = "cycleRule", allEntries = true)
    public boolean remove(List<Long> ids) {
        return ruleMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public CycleRuleMatchVO matchRule(String typeCode, String instrument, String specification) {
        return matchRule(typeCode, instrument, specification, null);
    }

    @Override
    public CycleRuleMatchVO matchRule(String typeCode, String instrument, String specification, Integer manualCycle) {
        CycleRuleMatchVO vo = new CycleRuleMatchVO();
        List<StandardCycleRule> candidates = findCandidateRules(typeCode, instrument, specification);

        if (candidates.isEmpty()) {
            Integer fallbackCycle = dictService.getStandardCycle(typeCode);
            vo.setMatchedCycle(fallbackCycle);
            vo.setMatchedCycleLabel(buildCycleLabel(fallbackCycle));
            vo.setTypeName(dictService.getAccessoryTypeLabel(typeCode));
            vo.setSuggestion("未找到匹配的规则，使用默认周期 " + fallbackCycle + " 天");
            return vo;
        }

        StandardCycleRule bestMatch = candidates.get(0);
        Integer matchedCycle = bestMatch.getStandardCycle();

        if (manualCycle != null && manualCycle > 0 && !manualCycle.equals(matchedCycle)) {
            vo.setFromManualOverride(true);
            vo.setMatchedCycle(manualCycle);
            vo.setMatchedCycleLabel(buildCycleLabel(manualCycle));

            double ratio = (double) manualCycle / matchedCycle;
            if (ratio < 0.5) {
                vo.setSuggestion("人工设置周期比标准周期短很多（标准：" + matchedCycle + "天），可能增加使用成本");
            } else if (ratio > 1.5) {
                vo.setSuggestion("人工设置周期比标准周期长很多（标准：" + matchedCycle + "天），可能影响使用效果");
            } else {
                vo.setSuggestion("已使用人工设置周期（标准：" + matchedCycle + "天）");
            }
        } else {
            vo.setMatchedCycle(matchedCycle);
            vo.setMatchedCycleLabel(buildCycleLabel(matchedCycle));
        }

        vo.setTypeName(bestMatch.getTypeName());
        vo.setInstrumentName(bestMatch.getInstrumentName());
        vo.setSpecDescription(bestMatch.getSpecDescription());
        vo.setRemark(bestMatch.getRemark());

        List<StandardCycleRuleVO> candidateVOs = candidates.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        vo.setCandidateRules(candidateVOs);

        if (vo.getSuggestion() == null) {
            vo.setSuggestion("已匹配到标准规则，周期 " + matchedCycle + " 天");
        }

        return vo;
    }

    @Override
    public Integer getMatchedCycle(String typeCode, String instrument, String specification) {
        List<StandardCycleRule> candidates = findCandidateRules(typeCode, instrument, specification);
        if (candidates.isEmpty()) {
            return dictService.getStandardCycle(typeCode);
        }
        return candidates.get(0).getStandardCycle();
    }

    @Override
    @Cacheable(value = "cycleRule", key = "'candidates:' + #typeCode + ':' + #instrument + ':' + #specification", unless = "#result == null || #result.isEmpty()")
    public List<StandardCycleRule> findCandidateRules(String typeCode, String instrument, String specification) {
        if (!StringUtils.hasText(typeCode)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<StandardCycleRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StandardCycleRule::getTypeCode, typeCode);
        wrapper.eq(StandardCycleRule::getEnabled, 1);

        wrapper.and(w -> w
                .eq(StandardCycleRule::getInstrument, instrument)
                .or().isNull(StandardCycleRule::getInstrument));

        List<StandardCycleRule> rules = ruleMapper.selectList(wrapper);

        if (rules.isEmpty()) {
            return new ArrayList<>();
        }

        List<ScoredRule> scoredRules = new ArrayList<>();
        for (StandardCycleRule rule : rules) {
            int score = 0;

            if (rule.getInstrument() != null && rule.getInstrument().equals(instrument)) {
                score += 100;
            } else if (rule.getInstrument() == null) {
                score += 10;
            }

            if (StringUtils.hasText(specification) && StringUtils.hasText(rule.getSpecPattern())) {
                if (specification.contains(rule.getSpecPattern())) {
                    score += 50;
                } else if (fuzzyMatch(specification, rule.getSpecPattern())) {
                    score += 30;
                }
            } else if (rule.getSpecPattern() == null) {
                score += 5;
            }

            score += rule.getPriority() != null ? rule.getPriority() : 0;

            if (score > 0) {
                scoredRules.add(new ScoredRule(rule, score));
            }
        }

        scoredRules.sort(Comparator.comparingInt(ScoredRule::getScore).reversed());

        return scoredRules.stream()
                .map(ScoredRule::getRule)
                .collect(Collectors.toList());
    }

    private boolean fuzzyMatch(String specification, String pattern) {
        if (!StringUtils.hasText(specification) || !StringUtils.hasText(pattern)) {
            return false;
        }

        String specLower = specification.toLowerCase();
        String patternLower = pattern.toLowerCase();

        String[] patternTokens = patternLower.split("[\\s,._-]+");
        for (String token : patternTokens) {
            if (token.length() >= 2 && specLower.contains(token)) {
                return true;
            }
        }

        return false;
    }

    private void fillDictFields(StandardCycleRule entity) {
        if (StringUtils.hasText(entity.getTypeCode())) {
            entity.setTypeName(dictService.getAccessoryTypeLabel(entity.getTypeCode()));
        }
        if (StringUtils.hasText(entity.getInstrument())) {
            entity.setInstrumentName(dictService.getInstrumentLabel(entity.getInstrument()));
        } else {
            entity.setInstrumentName("通用");
        }
    }

    private void validateStandardCycle(Integer cycle) {
        if (cycle == null || cycle <= 0) {
            throw new IllegalArgumentException("标准更换周期必须大于0，当前值: " + cycle);
        }
    }

    private LambdaQueryWrapper<StandardCycleRule> buildWrapper(CycleRuleQueryDTO query) {
        LambdaQueryWrapper<StandardCycleRule> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getTypeCode())) {
            wrapper.eq(StandardCycleRule::getTypeCode, query.getTypeCode());
        }
        if (StringUtils.hasText(query.getInstrument())) {
            wrapper.and(w -> w
                    .eq(StandardCycleRule::getInstrument, query.getInstrument())
                    .or().isNull(StandardCycleRule::getInstrument));
        }
        if (query.getEnabled() != null) {
            wrapper.eq(StandardCycleRule::getEnabled, query.getEnabled());
        }
        if (StringUtils.hasText(query.getSpecification())) {
            wrapper.and(w -> w
                    .like(StandardCycleRule::getSpecPattern, query.getSpecification())
                    .or().isNull(StandardCycleRule::getSpecPattern));
        }
        return wrapper;
    }

    private StandardCycleRuleVO convertToVO(StandardCycleRule rule) {
        StandardCycleRuleVO vo = new StandardCycleRuleVO();
        BeanUtils.copyProperties(rule, vo);
        vo.setStandardCycleLabel(buildCycleLabel(rule.getStandardCycle()));
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

    private static class ScoredRule {
        private final StandardCycleRule rule;
        private final int score;

        public ScoredRule(StandardCycleRule rule, int score) {
            this.rule = rule;
            this.score = score;
        }

        public StandardCycleRule getRule() {
            return rule;
        }

        public int getScore() {
            return score;
        }
    }
}
