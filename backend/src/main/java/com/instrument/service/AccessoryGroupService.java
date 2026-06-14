package com.instrument.service;

import com.instrument.entity.AccessoryGroup;

import java.util.List;

public interface AccessoryGroupService {

    List<AccessoryGroup> list();

    AccessoryGroup getById(Long id);

    boolean add(AccessoryGroup group);

    boolean update(AccessoryGroup group);

    boolean remove(Long id);
}
