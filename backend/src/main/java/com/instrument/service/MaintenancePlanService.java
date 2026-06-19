package com.instrument.service;

import com.instrument.vo.InstrumentMaintenancePlanVO;
import com.instrument.vo.MaintenancePlanItemVO;

import java.util.List;

public interface MaintenancePlanService {

    List<InstrumentMaintenancePlanVO> generatePlans();

    InstrumentMaintenancePlanVO generatePlanByInstrument(String instrument);

    List<MaintenancePlanItemVO> listPlanItems(String instrument);
}
