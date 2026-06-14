package com.instrument.controller;

import com.instrument.common.Result;
import com.instrument.service.DictService;
import com.instrument.vo.DictVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping("/accessory-types")
    public Result<List<DictVO>> accessoryTypes() {
        return Result.success(dictService.accessoryTypes());
    }

    @GetMapping("/instruments")
    public Result<List<DictVO>> instruments() {
        return Result.success(dictService.instruments());
    }

    @GetMapping("/worn-statuses")
    public Result<List<DictVO>> wornStatuses() {
        return Result.success(dictService.wornStatuses());
    }

    @GetMapping("/replacement-cycles")
    public Result<List<DictVO>> replacementCycles() {
        return Result.success(dictService.replacementCycles());
    }
}
