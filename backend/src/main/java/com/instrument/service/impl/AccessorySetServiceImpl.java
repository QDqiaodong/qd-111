package com.instrument.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.instrument.common.PageResult;
import com.instrument.dto.AccessorySetDTO;
import com.instrument.dto.AccessorySetItemDTO;
import com.instrument.dto.AccessorySetQueryDTO;
import com.instrument.entity.Accessory;
import com.instrument.entity.AccessorySet;
import com.instrument.entity.AccessorySetItem;
import com.instrument.mapper.AccessoryMapper;
import com.instrument.mapper.AccessorySetItemMapper;
import com.instrument.mapper.AccessorySetMapper;
import com.instrument.service.AccessorySetService;
import com.instrument.service.DictService;
import com.instrument.vo.AccessorySetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessorySetServiceImpl implements AccessorySetService {

    private final AccessorySetMapper setMapper;
    private final AccessorySetItemMapper setItemMapper;
    private final AccessoryMapper accessoryMapper;
    private final DictService dictService;

    private static final Set<String> VALID_STATUSES = new HashSet<>(Arrays.asList("enabled", "disabled"));

    @Override
    public PageResult<AccessorySet> page(AccessorySetQueryDTO query) {
        LambdaQueryWrapper<AccessorySet> wrapper = buildWrapper(query);
        wrapper.orderByDesc(AccessorySet::getCreateTime);
        IPage<AccessorySet> page = setMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public List<AccessorySet> list(AccessorySetQueryDTO query) {
        LambdaQueryWrapper<AccessorySet> wrapper = buildWrapper(query);
        wrapper.orderByDesc(AccessorySet::getCreateTime);
        return setMapper.selectList(wrapper);
    }

    @Override
    public AccessorySetVO getById(Long id) {
        AccessorySet set = setMapper.selectById(id);
        if (set == null) {
            return null;
        }
        AccessorySetVO vo = new AccessorySetVO();
        BeanUtils.copyProperties(set, vo);

        List<AccessorySetItem> items = setItemMapper.selectList(
                new LambdaQueryWrapper<AccessorySetItem>()
                        .eq(AccessorySetItem::getSetId, id)
                        .orderByAsc(AccessorySetItem::getSortOrder)
                        .orderByAsc(AccessorySetItem::getId));
        vo.setItems(items);
        vo.setItemCount(items.size());
        return vo;
    }

    @Override
    @Transactional
    public boolean add(AccessorySetDTO dto) {
        validateStatus(dto.getStatus());
        AccessorySet entity = new AccessorySet();
        BeanUtils.copyProperties(dto, entity, "items");
        fillInstrumentName(entity);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("enabled");
        }
        entity.setItemCount(dto.getItems().size());
        setMapper.insert(entity);
        saveItems(entity.getId(), dto.getItems());
        return true;
    }

    @Override
    @Transactional
    public boolean update(AccessorySetDTO dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("套装ID不能为空");
        }
        validateStatus(dto.getStatus());
        AccessorySet entity = new AccessorySet();
        BeanUtils.copyProperties(dto, entity, "items");
        fillInstrumentName(entity);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("enabled");
        }
        entity.setItemCount(dto.getItems().size());
        boolean result = setMapper.updateById(entity) > 0;

        setItemMapper.delete(new LambdaQueryWrapper<AccessorySetItem>()
                .eq(AccessorySetItem::getSetId, dto.getId()));
        saveItems(dto.getId(), dto.getItems());
        return result;
    }

    @Override
    @Transactional
    public boolean remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        int deletedItems = setItemMapper.delete(new LambdaQueryWrapper<AccessorySetItem>()
                .in(AccessorySetItem::getSetId, ids));
        log.info("删除套装前级联清理套装明细，套装数={}, 清理明细数={}", ids.size(), deletedItems);
        return setMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, String status) {
        validateStatus(status);
        AccessorySet entity = new AccessorySet();
        entity.setId(id);
        entity.setStatus(status);
        return setMapper.updateById(entity) > 0;
    }

    private void saveItems(Long setId, List<AccessorySetItemDTO> itemDTOs) {
        if (itemDTOs == null || itemDTOs.isEmpty()) {
            return;
        }
        List<Long> accessoryIds = itemDTOs.stream()
                .map(AccessorySetItemDTO::getAccessoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Accessory> accMap = new HashMap<>();
        if (!accessoryIds.isEmpty()) {
            List<Accessory> accessories = accessoryMapper.selectBatchIds(accessoryIds);
            accMap = accessories.stream()
                    .collect(Collectors.toMap(Accessory::getId, a -> a, (a, b) -> a));
        }

        for (int i = 0; i < itemDTOs.size(); i++) {
            AccessorySetItemDTO itemDTO = itemDTOs.get(i);
            Accessory accessory = accMap.get(itemDTO.getAccessoryId());
            if (accessory == null) {
                throw new IllegalArgumentException("配件不存在或已删除，配件ID: " + itemDTO.getAccessoryId());
            }
            AccessorySetItem item = new AccessorySetItem();
            item.setSetId(setId);
            item.setAccessoryId(accessory.getId());
            item.setAccessoryName(accessory.getName());
            item.setTypeCode(accessory.getTypeCode());
            item.setTypeName(accessory.getTypeName());
            item.setSpecification(accessory.getSpecification());
            item.setInstrument(accessory.getInstrument());
            item.setInstrumentName(accessory.getInstrumentName());
            item.setGroupId(accessory.getGroupId());
            item.setGroupName(accessory.getGroupName());
            item.setQuantity(itemDTO.getQuantity() != null && itemDTO.getQuantity() > 0
                    ? itemDTO.getQuantity() : 1);
            item.setSortOrder(itemDTO.getSortOrder() != null ? itemDTO.getSortOrder() : i + 1);
            item.setRemark(itemDTO.getRemark());
            setItemMapper.insert(item);
        }
    }

    private void fillInstrumentName(AccessorySet entity) {
        if (StringUtils.hasText(entity.getInstrument())) {
            entity.setInstrumentName(dictService.getInstrumentLabel(entity.getInstrument()));
        }
    }

    private void validateStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return;
        }
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("无效的套装状态: " + status + "，合法值为: " + VALID_STATUSES);
        }
    }

    private LambdaQueryWrapper<AccessorySet> buildWrapper(AccessorySetQueryDTO query) {
        LambdaQueryWrapper<AccessorySet> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(AccessorySet::getName, query.getKeyword())
                    .or().like(AccessorySet::getDescription, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getInstrument())) {
            wrapper.eq(AccessorySet::getInstrument, query.getInstrument());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(AccessorySet::getStatus, query.getStatus());
        }
        return wrapper;
    }
}
