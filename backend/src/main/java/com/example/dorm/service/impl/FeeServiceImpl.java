package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.Fee;
import com.example.dorm.mapper.FeeMapper;
import com.example.dorm.service.FeeService;
import org.springframework.stereotype.Service;

/** Service implementation for Fee. */
@Service
public class FeeServiceImpl extends ServiceImpl<FeeMapper, Fee> implements FeeService {
}
