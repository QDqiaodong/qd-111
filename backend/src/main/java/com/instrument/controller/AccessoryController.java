package com.instrument.controller;

import com.instrument.common.PageResult;
import com.instrument.common.Result;
import com.instrument.dto.AccessoryDTO;
import com.instrument.dto.AccessoryQueryDTO;
import com.instrument.entity.Accessory;
import com.instrument.service.AccessoryService;
import com.instrument.vo.AccessoryLifecycleVO;
import com.instrument.vo.CalendarDayVO;
import com.instrument.vo.CalendarMonthVO;
import com.instrument.vo.CycleReferenceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accessory")
@RequiredArgsConstructor
public class AccessoryController {

    private final AccessoryService accessoryService;

    @GetMapping("/page")
    public Result<PageResult<Accessory>> page(AccessoryQueryDTO query) {
        return Result.success(accessoryService.page(query));
    }

    @GetMapping("/list")
    public Result<List<Accessory>> list(AccessoryQueryDTO query) {
        return Result.success(accessoryService.list(query));
    }

    @GetMapping("/cycle-reference")
    public Result<CycleReferenceVO> getCycleReference(
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String instrument,
            @RequestParam(required = false) Integer currentCycle) {
        return Result.success(accessoryService.getCycleReference(typeCode, instrument, currentCycle));
    }

    @GetMapping("/{id}")
    public Result<Accessory> getById(@PathVariable Long id) {
        return Result.success(accessoryService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody AccessoryDTO dto) {
        return accessoryService.add(dto) ? Result.success() : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody AccessoryDTO dto) {
        return accessoryService.update(dto) ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping
    public Result<Void> remove(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        return accessoryService.remove(ids) ? Result.success() : Result.error("删除失败");
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return Result.error(400, "状态不能为空");
        }
        return accessoryService.updateStatus(id, status) ? Result.success() : Result.error("更新失败");
    }

    @PatchMapping("/batch-status")
    public Result<Void> batchUpdateStatus(@RequestBody Map<String, Object> body) {
        Object idsObj = body.get("ids");
        String status = (String) body.get("status");
        if (idsObj == null || status == null || status.isBlank()) {
            return Result.error(400, "参数不完整：ids 和 status 不能为空");
        }
        if (!(idsObj instanceof List<?> rawIds)) {
            return Result.error(400, "参数格式错误：ids 必须是数组");
        }
        if (rawIds.isEmpty()) {
            return Result.error(400, "请选择要操作的数据");
        }
        List<Long> ids = new ArrayList<>(rawIds.size());
        for (int i = 0; i < rawIds.size(); i++) {
            Object idObj = rawIds.get(i);
            if (idObj == null) {
                return Result.error(400, "参数错误：第 " + (i + 1) + " 个 ID 不能为空");
            }
            try {
                Long id;
                if (idObj instanceof Number number) {
                    id = number.longValue();
                } else if (idObj instanceof String str) {
                    id = Long.parseLong(str.trim());
                } else {
                    return Result.error(400, "参数错误：第 " + (i + 1) + " 个 ID 类型不支持，仅支持数字或数字字符串");
                }
                if (id <= 0) {
                    return Result.error(400, "参数错误：第 " + (i + 1) + " 个 ID 必须大于 0");
                }
                ids.add(id);
            } catch (NumberFormatException e) {
                return Result.error(400, "参数错误：第 " + (i + 1) + " 个 ID 不是有效的数字格式");
            }
        }
        return accessoryService.batchUpdateStatus(ids, status) ? Result.success() : Result.error("更新失败");
    }

    @GetMapping("/{id}/lifecycle")
    public Result<AccessoryLifecycleVO> getLifecycle(@PathVariable Long id) {
        return Result.success(accessoryService.getLifecycle(id));
    }

    @GetMapping("/lifecycle")
    public Result<List<AccessoryLifecycleVO>> listLifecycle(AccessoryQueryDTO query) {
        return Result.success(accessoryService.listLifecycle(query));
    }

    @GetMapping("/calendar/month")
    public Result<CalendarMonthVO> getCalendarMonth(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return Result.success(accessoryService.getCalendarMonth(year, month));
    }

    @GetMapping("/calendar/day")
    public Result<CalendarDayVO> getCalendarDay(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(accessoryService.getCalendarDay(date));
    }
}
