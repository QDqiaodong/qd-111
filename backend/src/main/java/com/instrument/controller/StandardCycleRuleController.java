package com.instrument.controller;

import com.instrument.common.PageResult;
import com.instrument.common.Result;
import com.instrument.dto.CycleRuleQueryDTO;
import com.instrument.dto.StandardCycleRuleDTO;
import com.instrument.entity.StandardCycleRule;
import com.instrument.service.StandardCycleRuleService;
import com.instrument.vo.CycleRuleMatchVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cycle-rule")
@RequiredArgsConstructor
public class StandardCycleRuleController {

    private final StandardCycleRuleService ruleService;

    @GetMapping("/page")
    public Result<PageResult<StandardCycleRule>> page(CycleRuleQueryDTO query) {
        return Result.success(ruleService.page(query));
    }

    @GetMapping("/list")
    public Result<List<StandardCycleRule>> list(CycleRuleQueryDTO query) {
        return Result.success(ruleService.list(query));
    }

    @GetMapping("/{id}")
    public Result<StandardCycleRule> getById(@PathVariable Long id) {
        return Result.success(ruleService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody StandardCycleRuleDTO dto) {
        return ruleService.add(dto) ? Result.success() : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody StandardCycleRuleDTO dto) {
        return ruleService.update(dto) ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping
    public Result<Void> remove(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的数据");
        }
        return ruleService.remove(ids) ? Result.success() : Result.error("删除失败");
    }

    @GetMapping("/match")
    public Result<CycleRuleMatchVO> match(
            @RequestParam String typeCode,
            @RequestParam(required = false) String instrument,
            @RequestParam(required = false) String specification,
            @RequestParam(required = false) Integer manualCycle) {
        return Result.success(ruleService.matchRule(typeCode, instrument, specification, manualCycle));
    }

    @GetMapping("/candidates")
    public Result<List<StandardCycleRule>> getCandidates(
            @RequestParam String typeCode,
            @RequestParam(required = false) String instrument,
            @RequestParam(required = false) String specification) {
        return Result.success(ruleService.findCandidateRules(typeCode, instrument, specification));
    }
}
