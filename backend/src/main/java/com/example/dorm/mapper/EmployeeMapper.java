package com.example.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dorm.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

/** MyBatis-Plus mapper for Employee. */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
}
