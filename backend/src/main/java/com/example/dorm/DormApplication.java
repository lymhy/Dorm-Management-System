package com.example.dorm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Employee Dormitory Management System - Spring Boot entry point. */
@SpringBootApplication
@org.mybatis.spring.annotation.MapperScan("com.example.dorm.mapper")
public class DormApplication {
    public static void main(String[] args) {
        SpringApplication.run(DormApplication.class, args);
    }
}
