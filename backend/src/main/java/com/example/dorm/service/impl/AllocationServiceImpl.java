package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.Allocation;
import com.example.dorm.mapper.AllocationMapper;
import com.example.dorm.service.AllocationService;
import org.springframework.stereotype.Service;

/** Service implementation for Allocation. */
@Service
public class AllocationServiceImpl extends ServiceImpl<AllocationMapper, Allocation> implements AllocationService {
}
