package com.instrument.controller;

import com.instrument.common.PageResult;
import com.instrument.common.Result;
import com.instrument.dto.WornStatusDictDTO;
import com.instrument.entity.WornStatusDict;
import com.instrument.service.WornStatusDictService;
import com.instrument.vo.WornStatusUsageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/worn-status-dict")
@RequiredArgsConstructor
public class WornStatusDictController {

    private final WornStatusDictService wornStatusDictService;

    @GetMapping("/page")
    public Result<PageResult<WornStatusDict>> page(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(wornStatusDictService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/list")
    public Result<List<WornStatusDict>> list() {
        return Result.success(wornStatusDictService.listAll());
    }

    @GetMapping("/list-enabled")
    public Result<List<WornStatusDict>> listEnabled() {
        return Result.success(wornStatusDictService.listEnabled());
    }

    @GetMapping("/{id}")
    public Result<WornStatusDict> getById(@PathVariable Long id) {
        return Result.success(wornStatusDictService.getById(id));
    }

    @GetMapping("/code/{code}")
    public Result<WornStatusDict> getByCode(@PathVariable String code) {
        return Result.success(wornStatusDictService.getByCode(code));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody WornStatusDictDTO dto) {
        return wornStatusDictService.add(dto) ? Result.success() : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody WornStatusDictDTO dto) {
        return wornStatusDictService.update(dto) ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping
    public Result<Void> remove(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        return wornStatusDictService.remove(ids) ? Result.success() : Result.error("删除失败");
    }

    @GetMapping("/usage/{id}")
    public Result<WornStatusUsageVO> getUsageInfo(@PathVariable Long id) {
        return Result.success(wornStatusDictService.getUsageInfo(id));
    }

    @PatchMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer enabled = body.get("enabled");
        if (enabled == null) {
            return Result.error("状态值不能为空");
        }
        return wornStatusDictService.toggleStatus(id, enabled) ? Result.success() : Result.error("状态切换失败");
    }
}
