package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.Employee;
import com.example.dorm.mapper.EmployeeMapper;
import com.example.dorm.service.EmployeeService;
import org.springframework.stereotype.Service;

/** Service implementation for Employee. */
@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {
}
