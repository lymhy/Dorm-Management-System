package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.Building;
import com.example.dorm.mapper.BuildingMapper;
import com.example.dorm.service.BuildingService;
import org.springframework.stereotype.Service;

/** Service implementation for Building. */
@Service
public class BuildingServiceImpl extends ServiceImpl<BuildingMapper, Building> implements BuildingService {
}
