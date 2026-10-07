package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.Repair;
import com.example.dorm.mapper.RepairMapper;
import com.example.dorm.service.RepairService;
import org.springframework.stereotype.Service;

/** Service implementation for Repair. */
@Service
public class RepairServiceImpl extends ServiceImpl<RepairMapper, Repair> implements RepairService {
}
