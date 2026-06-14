package com.instrument.controller;

import com.instrument.common.Result;
import com.instrument.entity.AccessoryGroup;
import com.instrument.service.AccessoryGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class AccessoryGroupController {

    private final AccessoryGroupService groupService;

    @GetMapping("/tree")
    public Result<List<AccessoryGroup>> tree() {
        return Result.success(groupService.list());
    }

    @GetMapping("/list")
    public Result<List<AccessoryGroup>> list() {
        return Result.success(groupService.list());
    }

    @GetMapping("/{id}")
    public Result<AccessoryGroup> getById(@PathVariable Long id) {
        return Result.success(groupService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody AccessoryGroup group) {
        return groupService.add(group) ? Result.success() : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody AccessoryGroup group) {
        return groupService.update(group) ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        return groupService.remove(id) ? Result.success() : Result.error("删除失败");
    }
}
