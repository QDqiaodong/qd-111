package com.instrument.service;

import com.instrument.vo.DictVO;

import java.util.List;

public interface DictService {

    List<DictVO> accessoryTypes();

    List<DictVO> instruments();

    List<DictVO> wornStatuses();

    List<DictVO> replacementCycles();

    String getAccessoryTypeLabel(String code);

    String getInstrumentLabel(String code);

    String getWornStatusLabel(String code);

    Integer getStandardCycle(String accessoryType);
}
