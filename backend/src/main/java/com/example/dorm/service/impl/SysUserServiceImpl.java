package com.example.dorm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dorm.entity.SysUser;
import com.example.dorm.mapper.SysUserMapper;
import com.example.dorm.service.SysUserService;
import org.springframework.stereotype.Service;

/** Service implementation for SysUser. */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
}
