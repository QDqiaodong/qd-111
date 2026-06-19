package com.instrument.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.common.PageResult;
import com.instrument.dto.PreparationTemplateDTO;
import com.instrument.dto.PreparationTemplateItemDTO;
import com.instrument.dto.PreparationTemplateQueryDTO;
import com.instrument.entity.Accessory;
import com.instrument.entity.PreparationTemplate;
import com.instrument.entity.PreparationTemplateItem;
import com.instrument.mapper.AccessoryMapper;
import com.instrument.mapper.PreparationTemplateItemMapper;
import com.instrument.mapper.PreparationTemplateMapper;
import com.instrument.service.PreparationTemplateService;
import com.instrument.vo.PreparationTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
public class PreparationTemplateServiceImpl implements PreparationTemplateService {

    private final PreparationTemplateMapper templateMapper;
    private final PreparationTemplateItemMapper itemMapper;
    private final AccessoryMapper accessoryMapper;

    @Override
    public PageResult<PreparationTemplate> page(PreparationTemplateQueryDTO query) {
        LambdaQueryWrapper<PreparationTemplate> wrapper = buildWrapper(query);
        wrapper.orderByDesc(PreparationTemplate::getCreateTime);
        IPage<PreparationTemplate> page = templateMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public List<PreparationTemplate> list(PreparationTemplateQueryDTO query) {
        LambdaQueryWrapper<PreparationTemplate> wrapper = buildWrapper(query);
        wrapper.orderByDesc(PreparationTemplate::getCreateTime);
        return templateMapper.selectList(wrapper);
    }

    @Override
    public PreparationTemplateVO getById(Long id) {
        PreparationTemplate template = templateMapper.selectById(id);
        if (template == null) {
            return null;
        }
        return buildVO(template);
    }

    @Override
    public PreparationTemplateVO getByTypeCode(String typeCode) {
        if (!StringUtils.hasText(typeCode)) {
            return null;
        }
        LambdaQueryWrapper<PreparationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PreparationTemplate::getTypeCode, typeCode);
        wrapper.eq(PreparationTemplate::getEnabled, 1);
        wrapper.orderByDesc(PreparationTemplate::getCreateTime);
        List<PreparationTemplate> templates = templateMapper.selectList(wrapper);
        if (templates == null || templates.isEmpty()) {
            return null;
        }
        return buildVO(templates.get(0));
    }

    @Override
    @Transactional
    public PreparationTemplateVO add(PreparationTemplateDTO dto) {
        PreparationTemplate template = new PreparationTemplate();
        BeanUtils.copyProperties(dto, template);
        template.setItemCount(dto.getItems().size());
        if (template.getEnabled() == null) {
            template.setEnabled(1);
        }

        boolean result = templateMapper.insert(template) > 0;
        if (!result) {
            return null;
        }

        saveItems(template.getId(), dto.getItems());

        return getById(template.getId());
    }

    @Override
    @Transactional
    public PreparationTemplateVO update(PreparationTemplateDTO dto) {
        PreparationTemplate existing = templateMapper.selectById(dto.getId());
        if (existing == null) {
            throw new IllegalArgumentException("模板不存在");
        }

        PreparationTemplate template = new PreparationTemplate();
        BeanUtils.copyProperties(dto, template);
        template.setItemCount(dto.getItems().size());

        boolean result = templateMapper.updateById(template) > 0;
        if (!result) {
            return null;
        }

        LambdaQueryWrapper<PreparationTemplateItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(PreparationTemplateItem::getTemplateId, dto.getId());
        itemMapper.delete(deleteWrapper);

        saveItems(dto.getId(), dto.getItems());

        return getById(dto.getId());
    }

    @Override
    @Transactional
    public boolean remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            LambdaQueryWrapper<PreparationTemplateItem> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(PreparationTemplateItem::getTemplateId, id);
            itemMapper.delete(deleteWrapper);
        }
        return templateMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public boolean updateStatus(Long id, Integer enabled) {
        PreparationTemplate template = new PreparationTemplate();
        template.setId(id);
        template.setEnabled(enabled);
        return templateMapper.updateById(template) > 0;
    }

    private void saveItems(Long templateId, List<PreparationTemplateItemDTO> itemDTOs) {
        if (itemDTOs == null || itemDTOs.isEmpty()) {
            return;
        }
        int sort = 1;
        for (PreparationTemplateItemDTO itemDTO : itemDTOs) {
            PreparationTemplateItem item = new PreparationTemplateItem();
            BeanUtils.copyProperties(itemDTO, item);
            item.setTemplateId(templateId);
            if (item.getRequired() == null) {
                item.setRequired(1);
            }
            if (item.getSortOrder() == null) {
                item.setSortOrder(sort++);
            }
            itemMapper.insert(item);
        }
    }

    private PreparationTemplateVO buildVO(PreparationTemplate template) {
        PreparationTemplateVO vo = new PreparationTemplateVO();
        BeanUtils.copyProperties(template, vo);

        LambdaQueryWrapper<PreparationTemplateItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(PreparationTemplateItem::getTemplateId, template.getId());
        itemWrapper.orderByAsc(PreparationTemplateItem::getSortOrder);
        List<PreparationTemplateItem> items = itemMapper.selectList(itemWrapper);
        vo.setItems(items);

        return vo;
    }

    private LambdaQueryWrapper<PreparationTemplate> buildWrapper(PreparationTemplateQueryDTO query) {
        LambdaQueryWrapper<PreparationTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getTypeCode())) {
            wrapper.eq(PreparationTemplate::getTypeCode, query.getTypeCode());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(PreparationTemplate::getName, query.getKeyword())
                    .or().like(PreparationTemplate::getDescription, query.getKeyword()));
        }
        if (query.getEnabled() != null) {
            wrapper.eq(PreparationTemplate::getEnabled, query.getEnabled());
        }
        return wrapper;
    }
}
