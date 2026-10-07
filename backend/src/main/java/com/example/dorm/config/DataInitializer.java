package com.example.dorm.config;

import com.example.dorm.entity.SysUser;
import com.example.dorm.mapper.SysUserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/** Seed an admin account on first startup. */
@Component
public class DataInitializer implements CommandLineRunner {

    @Resource
    private SysUserMapper userMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        Long count = userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>());
        if (count == null || count == 0) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(encoder.encode("admin123"));
            admin.setRealName("管理员");
            admin.setRole("admin");
            admin.setPhone("13800000000");
            admin.setStatus(1);
            userMapper.insert(admin);
        }
    }
}
