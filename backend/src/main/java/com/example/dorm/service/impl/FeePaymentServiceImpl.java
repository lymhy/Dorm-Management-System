package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.FeePayment;
import com.example.dorm.mapper.FeePaymentMapper;
import com.example.dorm.service.FeePaymentService;
import org.springframework.stereotype.Service;

@Service
public class FeePaymentServiceImpl extends ServiceImpl<FeePaymentMapper, FeePayment> implements FeePaymentService {
}
