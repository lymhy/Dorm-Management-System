package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.SystemConfig;
import com.example.dorm.mapper.SystemConfigMapper;
import com.example.dorm.service.SystemConfigService;
import org.springframework.stereotype.Service;

/** Service implementation for SystemConfig. */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {
}
