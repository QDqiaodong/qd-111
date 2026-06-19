package com.instrument.controller;

import com.instrument.common.Result;
import com.instrument.service.MaintenancePlanService;
import com.instrument.vo.InstrumentMaintenancePlanVO;
import com.instrument.vo.MaintenancePlanItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/maintenance-plan")
@RequiredArgsConstructor
public class MaintenancePlanController {

    private final MaintenancePlanService maintenancePlanService;

    @GetMapping("/all")
    public Result<List<InstrumentMaintenancePlanVO>> allPlans() {
        return Result.success(maintenancePlanService.generatePlans());
    }

    @GetMapping("/instrument/{instrument}")
    public Result<InstrumentMaintenancePlanVO> planByInstrument(@PathVariable String instrument) {
        return Result.success(maintenancePlanService.generatePlanByInstrument(instrument));
    }

    @GetMapping("/items/{instrument}")
    public Result<List<MaintenancePlanItemVO>> planItems(@PathVariable String instrument) {
        return Result.success(maintenancePlanService.listPlanItems(instrument));
    }
}
