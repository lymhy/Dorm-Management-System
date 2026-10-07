package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.MoveOut;
import com.example.dorm.mapper.MoveOutMapper;
import com.example.dorm.service.MoveOutService;
import org.springframework.stereotype.Service;

/** Service implementation for MoveOut. */
@Service
public class MoveOutServiceImpl extends ServiceImpl<MoveOutMapper, MoveOut> implements MoveOutService {
}
