package com.instrument.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.common.PageResult;
import com.instrument.dto.PreparationChecklistGenerateDTO;
import com.instrument.dto.PreparationChecklistItemCompleteDTO;
import com.instrument.dto.PreparationChecklistQueryDTO;
import com.instrument.dto.PreparationTemplateQueryDTO;
import com.instrument.entity.Accessory;
import com.instrument.entity.PreparationChecklist;
import com.instrument.entity.PreparationChecklistItem;
import com.instrument.entity.PreparationTemplate;
import com.instrument.entity.PreparationTemplateItem;
import com.instrument.mapper.AccessoryMapper;
import com.instrument.mapper.PreparationChecklistItemMapper;
import com.instrument.mapper.PreparationChecklistMapper;
import com.instrument.mapper.PreparationTemplateItemMapper;
import com.instrument.service.PreparationChecklistService;
import com.instrument.service.PreparationTemplateService;
import com.instrument.vo.PreparationChecklistCategoryVO;
import com.instrument.vo.PreparationChecklistVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreparationChecklistServiceImpl implements PreparationChecklistService {

    private final PreparationChecklistMapper checklistMapper;
    private final PreparationChecklistItemMapper checklistItemMapper;
    private final PreparationTemplateService templateService;
    private final PreparationTemplateItemMapper templateItemMapper;
    private final AccessoryMapper accessoryMapper;

    @Override
    public PageResult<PreparationChecklist> page(PreparationChecklistQueryDTO query) {
        LambdaQueryWrapper<PreparationChecklist> wrapper = buildWrapper(query);
        wrapper.orderByDesc(PreparationChecklist::getCreateTime);
        IPage<PreparationChecklist> page = checklistMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public List<PreparationChecklist> list(PreparationChecklistQueryDTO query) {
        LambdaQueryWrapper<PreparationChecklist> wrapper = buildWrapper(query);
        wrapper.orderByDesc(PreparationChecklist::getCreateTime);
        return checklistMapper.selectList(wrapper);
    }

    @Override
    public PreparationChecklistVO getById(Long id) {
        PreparationChecklist checklist = checklistMapper.selectById(id);
        if (checklist == null) {
            return null;
        }
        return buildVO(checklist);
    }

    @Override
    public List<PreparationChecklistCategoryVO> getChecklistWithCategories(Long id) {
        PreparationChecklist checklist = checklistMapper.selectById(id);
        if (checklist == null) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<PreparationChecklistItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PreparationChecklistItem::getChecklistId, id);
        itemWrapper.orderByAsc(PreparationChecklistItem::getSortOrder);
        List<PreparationChecklistItem> items = checklistItemMapper.selectList(itemWrapper);

        Map<String, List<PreparationChecklistItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(
                        PreparationChecklistItem::getCategory,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<PreparationChecklistCategoryVO> result = new ArrayList<>();
        for (Map.Entry<String, List<PreparationChecklistItem>> entry : grouped.entrySet()) {
            PreparationChecklistCategoryVO categoryVO = new PreparationChecklistCategoryVO();
            categoryVO.setCategory(entry.getKey());
            categoryVO.setCategoryName(entry.getValue().get(0).getCategoryName());
            categoryVO.setTotalCount(entry.getValue().size());
            categoryVO.setCompletedCount((int) entry.getValue().stream()
                    .filter(item -> item.getCompleted() != null && item.getCompleted() == 1)
                    .count());
            categoryVO.setItems(entry.getValue());
            result.add(categoryVO);
        }

        return result;
    }

    @Override
    @Transactional
    public PreparationChecklistVO generate(PreparationChecklistGenerateDTO dto) {
        Accessory accessory = accessoryMapper.selectById(dto.getAccessoryId());
        if (accessory == null) {
            throw new IllegalArgumentException("配件不存在");
        }

        PreparationTemplateVO templateVO = templateService.getByTypeCode(accessory.getTypeCode());
        if (templateVO == null) {
            throw new IllegalArgumentException("该配件类型暂无准备清单模板");
        }

        PreparationChecklist checklist = new PreparationChecklist();
        checklist.setTemplateId(templateVO.getId());
        checklist.setTemplateName(templateVO.getName());
        checklist.setTypeCode(accessory.getTypeCode());
        checklist.setTypeName(accessory.getTypeName());
        checklist.setAccessoryId(accessory.getId());
        checklist.setAccessoryName(accessory.getName());
        checklist.setReplacementRecordId(dto.getReplacementRecordId());
        checklist.setOperator(dto.getOperator());
        checklist.setRemark(dto.getRemark());
        checklist.setStatus("pending");
        checklist.setTotalCount(templateVO.getItems().size());
        checklist.setCompletedCount(0);
        checklist.setRequiredCompletedCount(0);

        int requiredTotal = (int) templateVO.getItems().stream()
                .filter(item -> item.getRequired() != null && item.getRequired() == 1)
                .count();
        checklist.setRequiredTotalCount(requiredTotal);

        checklistMapper.insert(checklist);

        for (PreparationTemplateItem templateItem : templateVO.getItems()) {
            PreparationChecklistItem item = new PreparationChecklistItem();
            item.setChecklistId(checklist.getId());
            item.setTemplateItemId(templateItem.getId());
            item.setCategory(templateItem.getCategory());
            item.setCategoryName(templateItem.getCategoryName());
            item.setName(templateItem.getName());
            item.setDescription(templateItem.getDescription());
            item.setRequired(templateItem.getRequired());
            item.setSortOrder(templateItem.getSortOrder());
            item.setCompleted(0);
            checklistItemMapper.insert(item);
        }

        log.info("生成准备清单成功，checklistId={}, accessoryId={}, templateId={}",
                checklist.getId(), accessory.getId(), templateVO.getId());

        return getById(checklist.getId());
    }

    @Override
    @Transactional
    public PreparationChecklistVO startChecklist(Long id, String operator) {
        PreparationChecklist checklist = checklistMapper.selectById(id);
        if (checklist == null) {
            throw new IllegalArgumentException("清单不存在");
        }
        if (!"pending".equals(checklist.getStatus())) {
            throw new IllegalArgumentException("清单状态不允许开始");
        }

        checklist.setStatus("in_progress");
        checklist.setStartTime(LocalDateTime.now());
        if (StringUtils.hasText(operator)) {
            checklist.setOperator(operator);
        }
        checklistMapper.updateById(checklist);

        return getById(id);
    }

    @Override
    @Transactional
    public PreparationChecklistVO completeItem(PreparationChecklistItemCompleteDTO dto) {
        PreparationChecklistItem item = checklistItemMapper.selectById(dto.getItemId());
        if (item == null) {
            throw new IllegalArgumentException("清单项不存在");
        }

        item.setCompleted(dto.getCompleted());
        if (dto.getCompleted() != null && dto.getCompleted() == 1) {
            item.setCompletedTime(LocalDateTime.now());
            item.setCompletedBy(dto.getCompletedBy());
        } else {
            item.setCompletedTime(null);
            item.setCompletedBy(null);
        }
        item.setCompletionNote(dto.getCompletionNote());
        checklistItemMapper.updateById(item);

        updateChecklistStats(item.getChecklistId());

        return getById(item.getChecklistId());
    }

    @Override
    @Transactional
    public PreparationChecklistVO completeChecklist(Long id, String operator) {
        PreparationChecklist checklist = checklistMapper.selectById(id);
        if (checklist == null) {
            throw new IllegalArgumentException("清单不存在");
        }

        if (checklist.getRequiredCompletedCount() < checklist.getRequiredTotalCount()) {
            throw new IllegalArgumentException("还有必做项未完成，无法结束清单");
        }

        checklist.setStatus("completed");
        checklist.setFinishTime(LocalDateTime.now());
        if (StringUtils.hasText(operator)) {
            checklist.setOperator(operator);
        }
        checklistMapper.updateById(checklist);

        log.info("准备清单完成，checklistId={}", id);

        return getById(id);
    }

    @Override
    @Transactional
    public boolean remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            LambdaQueryWrapper<PreparationChecklistItem> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(PreparationChecklistItem::getChecklistId, id);
            checklistItemMapper.delete(deleteWrapper);
        }
        return checklistMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public boolean linkReplacementRecord(Long checklistId, Long replacementRecordId) {
        PreparationChecklist checklist = checklistMapper.selectById(checklistId);
        if (checklist == null) {
            return false;
        }
        checklist.setReplacementRecordId(replacementRecordId);
        return checklistMapper.updateById(checklist) > 0;
    }

    @Override
    public List<PreparationChecklistVO> getByReplacementRecordId(Long replacementRecordId) {
        if (replacementRecordId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<PreparationChecklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PreparationChecklist::getReplacementRecordId, replacementRecordId);
        wrapper.orderByDesc(PreparationChecklist::getCreateTime);
        List<PreparationChecklist> checklists = checklistMapper.selectList(wrapper);
        return checklists.stream().map(this::buildVO).collect(Collectors.toList());
    }

    @Override
    public List<PreparationChecklistVO> getByAccessoryId(Long accessoryId) {
        if (accessoryId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<PreparationChecklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PreparationChecklist::getAccessoryId, accessoryId);
        wrapper.orderByDesc(PreparationChecklist::getCreateTime);
        List<PreparationChecklist> checklists = checklistMapper.selectList(wrapper);
        return checklists.stream().map(this::buildVO).collect(Collectors.toList());
    }

    private void updateChecklistStats(Long checklistId) {
        LambdaQueryWrapper<PreparationChecklistItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PreparationChecklistItem::getChecklistId, checklistId);
        List<PreparationChecklistItem> items = checklistItemMapper.selectList(itemWrapper);

        int completedCount = (int) items.stream()
                .filter(item -> item.getCompleted() != null && item.getCompleted() == 1)
                .count();
        int requiredCompletedCount = (int) items.stream()
                .filter(item -> item.getRequired() != null && item.getRequired() == 1
                        && item.getCompleted() != null && item.getCompleted() == 1)
                .count();

        PreparationChecklist checklist = new PreparationChecklist();
        checklist.setId(checklistId);
        checklist.setCompletedCount(completedCount);
        checklist.setRequiredCompletedCount(requiredCompletedCount);
        checklistMapper.updateById(checklist);
    }

    private PreparationChecklistVO buildVO(PreparationChecklist checklist) {
        PreparationChecklistVO vo = new PreparationChecklistVO();
        BeanUtils.copyProperties(checklist, vo);

        vo.setStatusName(getStatusName(checklist.getStatus()));

        LambdaQueryWrapper<PreparationChecklistItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PreparationChecklistItem::getChecklistId, checklist.getId());
        itemWrapper.orderByAsc(PreparationChecklistItem::getSortOrder);
        List<PreparationChecklistItem> items = checklistItemMapper.selectList(itemWrapper);
        vo.setItems(items);

        return vo;
    }

    private String getStatusName(String status) {
        if (status == null) return "";
        switch (status) {
            case "pending": return "待开始";
            case "in_progress": return "进行中";
            case "completed": return "已完成";
            default: return status;
        }
    }

    private LambdaQueryWrapper<PreparationChecklist> buildWrapper(PreparationChecklistQueryDTO query) {
        LambdaQueryWrapper<PreparationChecklist> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getTypeCode())) {
            wrapper.eq(PreparationChecklist::getTypeCode, query.getTypeCode());
        }
        if (query.getAccessoryId() != null) {
            wrapper.eq(PreparationChecklist::getAccessoryId, query.getAccessoryId());
        }
        if (query.getReplacementRecordId() != null) {
            wrapper.eq(PreparationChecklist::getReplacementRecordId, query.getReplacementRecordId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(PreparationChecklist::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(PreparationChecklist::getAccessoryName, query.getKeyword())
                    .or().like(PreparationChecklist::getTemplateName, query.getKeyword()));
        }
        return wrapper;
    }
}
