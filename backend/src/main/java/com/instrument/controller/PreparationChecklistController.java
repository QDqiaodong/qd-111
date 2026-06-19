package com.instrument.controller;

import com.instrument.common.PageResult;
import com.instrument.common.Result;
import com.instrument.dto.PreparationChecklistGenerateDTO;
import com.instrument.dto.PreparationChecklistItemCompleteDTO;
import com.instrument.dto.PreparationChecklistQueryDTO;
import com.instrument.entity.PreparationChecklist;
import com.instrument.service.PreparationChecklistService;
import com.instrument.vo.PreparationChecklistCategoryVO;
import com.instrument.vo.PreparationChecklistVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/preparation-checklist")
@RequiredArgsConstructor
public class PreparationChecklistController {

    private final PreparationChecklistService checklistService;

    @GetMapping("/page")
    public Result<PageResult<PreparationChecklist>> page(PreparationChecklistQueryDTO query) {
        return Result.success(checklistService.page(query));
    }

    @GetMapping("/list")
    public Result<List<PreparationChecklist>> list(PreparationChecklistQueryDTO query) {
        return Result.success(checklistService.list(query));
    }

    @GetMapping("/{id}")
    public Result<PreparationChecklistVO> getById(@PathVariable Long id) {
        return Result.success(checklistService.getById(id));
    }

    @GetMapping("/{id}/categories")
    public Result<List<PreparationChecklistCategoryVO>> getChecklistWithCategories(@PathVariable Long id) {
        return Result.success(checklistService.getChecklistWithCategories(id));
    }

    @PostMapping("/generate")
    public Result<PreparationChecklistVO> generate(@Valid @RequestBody PreparationChecklistGenerateDTO dto) {
        PreparationChecklistVO result = checklistService.generate(dto);
        return result != null ? Result.success(result) : Result.error("生成清单失败");
    }

    @PostMapping("/{id}/start")
    public Result<PreparationChecklistVO> startChecklist(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String operator = body != null ? body.get("operator") : null;
        PreparationChecklistVO result = checklistService.startChecklist(id, operator);
        return result != null ? Result.success(result) : Result.error("开始清单失败");
    }

    @PostMapping("/complete-item")
    public Result<PreparationChecklistVO> completeItem(@Valid @RequestBody PreparationChecklistItemCompleteDTO dto) {
        PreparationChecklistVO result = checklistService.completeItem(dto);
        return result != null ? Result.success(result) : Result.error("更新项状态失败");
    }

    @PostMapping("/{id}/complete")
    public Result<PreparationChecklistVO> completeChecklist(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String operator = body != null ? body.get("operator") : null;
        PreparationChecklistVO result = checklistService.completeChecklist(id, operator);
        return result != null ? Result.success(result) : Result.error("完成清单失败");
    }

    @DeleteMapping
    public Result<Void> remove(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        return checklistService.remove(ids) ? Result.success() : Result.error("删除失败");
    }

    @PostMapping("/{checklistId}/link-replacement/{replacementRecordId}")
    public Result<Void> linkReplacementRecord(
            @PathVariable Long checklistId,
            @PathVariable Long replacementRecordId) {
        return checklistService.linkReplacementRecord(checklistId, replacementRecordId)
                ? Result.success() : Result.error("关联更换记录失败");
    }

    @GetMapping("/by-replacement/{replacementRecordId}")
    public Result<List<PreparationChecklistVO>> getByReplacementRecordId(@PathVariable Long replacementRecordId) {
        return Result.success(checklistService.getByReplacementRecordId(replacementRecordId));
    }

    @GetMapping("/by-accessory/{accessoryId}")
    public Result<List<PreparationChecklistVO>> getByAccessoryId(@PathVariable Long accessoryId) {
        return Result.success(checklistService.getByAccessoryId(accessoryId));
    }
}
