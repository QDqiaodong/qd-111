package com.instrument.controller;

import com.instrument.common.PageResult;
import com.instrument.common.Result;
import com.instrument.dto.ReplacementDTO;
import com.instrument.dto.ReplacementQueryDTO;
import com.instrument.entity.ReplacementRecord;
import com.instrument.service.ReplacementRecordService;
import com.instrument.vo.ReplacementResultVO;
import com.instrument.vo.ReplacementTimelineVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/replacement")
@RequiredArgsConstructor
public class ReplacementRecordController {

    private final ReplacementRecordService recordService;

    @GetMapping("/page")
    public Result<PageResult<ReplacementRecord>> page(ReplacementQueryDTO query) {
        return Result.success(recordService.page(query));
    }

    @GetMapping("/list")
    public Result<List<ReplacementRecord>> list(ReplacementQueryDTO query) {
        return Result.success(recordService.list(query));
    }

    @GetMapping("/timeline")
    public Result<List<ReplacementTimelineVO>> timeline(ReplacementQueryDTO query) {
        return Result.success(recordService.timeline(query));
    }

    @GetMapping("/history/{accessoryId}")
    public Result<List<ReplacementRecord>> history(@PathVariable Long accessoryId) {
        return Result.success(recordService.history(accessoryId));
    }

    @GetMapping("/{id}")
    public Result<ReplacementRecord> getById(@PathVariable Long id) {
        return Result.success(recordService.getById(id));
    }

    @PostMapping
    public Result<ReplacementResultVO> add(@Valid @RequestBody ReplacementDTO dto) {
        ReplacementResultVO result = recordService.add(dto);
        return result != null ? Result.success(result) : Result.error("新增失败");
    }

    @PutMapping
    public Result<ReplacementResultVO> update(@Valid @RequestBody ReplacementDTO dto) {
        ReplacementResultVO result = recordService.update(dto);
        return result != null ? Result.success(result) : Result.error("更新失败");
    }

    @DeleteMapping
    public Result<Void> remove(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        return recordService.remove(ids) ? Result.success() : Result.error("删除失败");
    }

    @PostMapping("/recalculate/accessory/{accessoryId}")
    public Result<Void> recalculateByAccessory(
            @PathVariable Long accessoryId,
            @RequestParam(required = false, defaultValue = "true") boolean withStandardCycle) {
        if (withStandardCycle) {
            recordService.recalculateByAccessoryWithStandardCycle(accessoryId);
        } else {
            recordService.recalculateByAccessory(accessoryId);
        }
        return Result.success();
    }

    @PostMapping("/recalculate/accessories")
    public Result<Void> recalculateByAccessoryIds(@RequestBody Map<String, Object> body) {
        Object idsObj = body.get("accessoryIds");
        if (!(idsObj instanceof List<?> rawIds)) {
            return Result.error(400, "参数错误：accessoryIds 必须是数组");
        }
        List<Long> accessoryIds = new ArrayList<>(rawIds.size());
        for (Object idObj : rawIds) {
            if (idObj instanceof Number number) {
                accessoryIds.add(number.longValue());
            } else if (idObj instanceof String str) {
                accessoryIds.add(Long.parseLong(str.trim()));
            }
        }
        Object withCycleObj = body.get("withStandardCycle");
        boolean withStandardCycle = withCycleObj == null || Boolean.TRUE.equals(withCycleObj);
        if (withStandardCycle) {
            for (Long id : accessoryIds) {
                recordService.recalculateByAccessoryWithStandardCycle(id);
            }
        } else {
            recordService.recalculateByAccessoryIds(accessoryIds);
        }
        return Result.success();
    }

    @PostMapping("/recalculate/condition")
    public Result<Void> recalculateByCondition(@RequestBody(required = false) Map<String, String> body) {
        String typeCode = body != null ? body.get("typeCode") : null;
        String instrument = body != null ? body.get("instrument") : null;
        recordService.recalculateByCondition(typeCode, instrument);
        return Result.success();
    }

    @PostMapping("/recalculate/all")
    public Result<Void> recalculateAll() {
        recordService.recalculateByCondition(null, null);
        return Result.success();
    }
}
