package com.instrument.controller;

import com.instrument.common.PageResult;
import com.instrument.common.Result;
import com.instrument.dto.AccessorySetDTO;
import com.instrument.dto.AccessorySetQueryDTO;
import com.instrument.entity.AccessorySet;
import com.instrument.service.AccessorySetService;
import com.instrument.vo.AccessorySetVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accessory-set")
@RequiredArgsConstructor
public class AccessorySetController {

    private final AccessorySetService accessorySetService;

    @GetMapping("/page")
    public Result<PageResult<AccessorySet>> page(AccessorySetQueryDTO query) {
        return Result.success(accessorySetService.page(query));
    }

    @GetMapping("/list")
    public Result<List<AccessorySet>> list(AccessorySetQueryDTO query) {
        return Result.success(accessorySetService.list(query));
    }

    @GetMapping("/{id}")
    public Result<AccessorySetVO> getById(@PathVariable Long id) {
        return Result.success(accessorySetService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody AccessorySetDTO dto) {
        return accessorySetService.add(dto) ? Result.success() : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody AccessorySetDTO dto) {
        return accessorySetService.update(dto) ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping
    public Result<Void> remove(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        return accessorySetService.remove(ids) ? Result.success() : Result.error("删除失败");
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return Result.error(400, "状态不能为空");
        }
        return accessorySetService.updateStatus(id, status) ? Result.success() : Result.error("更新失败");
    }
}
