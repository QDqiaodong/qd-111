package com.instrument.service;

import com.instrument.vo.AccessoryCompatibilityVO;

public interface AccessoryCompatibilityService {

    AccessoryCompatibilityVO checkCompatibility(String typeCode, String instrument, String specification);

    AccessoryCompatibilityVO checkCompatibility(String typeCode, String instrument, String specification, String name);
}
