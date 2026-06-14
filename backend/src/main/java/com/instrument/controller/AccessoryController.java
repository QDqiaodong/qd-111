package com.instrument.controller;

import com.instrument.common.PageResult;
import com.instrument.common.Result;
import com.instrument.dto.AccessoryDTO;
import com.instrument.dto.AccessoryQueryDTO;
import com.instrument.entity.Accessory;
import com.instrument.service.AccessoryService;
import com.instrument.vo.AccessoryLifecycleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
        if (status == null) {
            return Result.error("状态不能为空");
        }
        return accessoryService.updateStatus(id, status) ? Result.success() : Result.error("更新失败");
    }

    @PatchMapping("/batch-status")
    public Result<Void> batchUpdateStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.get("ids");
        String status = (String) body.get("status");
        if (ids == null || ids.isEmpty() || status == null) {
            return Result.error("参数不完整");
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
}
