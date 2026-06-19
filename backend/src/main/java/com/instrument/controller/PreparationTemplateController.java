package com.instrument.controller;

import com.instrument.common.PageResult;
import com.instrument.common.Result;
import com.instrument.dto.PreparationTemplateDTO;
import com.instrument.dto.PreparationTemplateQueryDTO;
import com.instrument.entity.PreparationTemplate;
import com.instrument.service.PreparationTemplateService;
import com.instrument.vo.PreparationTemplateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/preparation-template")
@RequiredArgsConstructor
public class PreparationTemplateController {

    private final PreparationTemplateService templateService;

    @GetMapping("/page")
    public Result<PageResult<PreparationTemplate>> page(PreparationTemplateQueryDTO query) {
        return Result.success(templateService.page(query));
    }

    @GetMapping("/list")
    public Result<List<PreparationTemplate>> list(PreparationTemplateQueryDTO query) {
        return Result.success(templateService.list(query));
    }

    @GetMapping("/{id}")
    public Result<PreparationTemplateVO> getById(@PathVariable Long id) {
        return Result.success(templateService.getById(id));
    }

    @GetMapping("/by-type/{typeCode}")
    public Result<PreparationTemplateVO> getByTypeCode(@PathVariable String typeCode) {
        return Result.success(templateService.getByTypeCode(typeCode));
    }

    @PostMapping
    public Result<PreparationTemplateVO> add(@Valid @RequestBody PreparationTemplateDTO dto) {
        PreparationTemplateVO result = templateService.add(dto);
        return result != null ? Result.success(result) : Result.error("新增失败");
    }

    @PutMapping
    public Result<PreparationTemplateVO> update(@Valid @RequestBody PreparationTemplateDTO dto) {
        PreparationTemplateVO result = templateService.update(dto);
        return result != null ? Result.success(result) : Result.error("更新失败");
    }

    @DeleteMapping
    public Result<Void> remove(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        return templateService.remove(ids) ? Result.success() : Result.error("删除失败");
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer enabled = body.get("enabled");
        if (enabled == null) {
            return Result.error("状态参数不能为空");
        }
        return templateService.updateStatus(id, enabled) ? Result.success() : Result.error("更新状态失败");
    }
}
