package com.instrument.controller;

import com.instrument.common.PageResult;
import com.instrument.common.Result;
import com.instrument.dto.ReplacementDTO;
import com.instrument.dto.ReplacementQueryDTO;
import com.instrument.entity.ReplacementRecord;
import com.instrument.service.ReplacementRecordService;
import com.instrument.vo.ReplacementTimelineVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public Result<Void> add(@Valid @RequestBody ReplacementDTO dto) {
        return recordService.add(dto) ? Result.success() : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody ReplacementDTO dto) {
        return recordService.update(dto) ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping
    public Result<Void> remove(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        return recordService.remove(ids) ? Result.success() : Result.error("删除失败");
    }
}
