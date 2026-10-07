package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.Visitor;
import com.example.dorm.mapper.VisitorMapper;
import com.example.dorm.service.VisitorService;
import org.springframework.stereotype.Service;

/** Service implementation for Visitor. */
@Service
public class VisitorServiceImpl extends ServiceImpl<VisitorMapper, Visitor> implements VisitorService {
}
