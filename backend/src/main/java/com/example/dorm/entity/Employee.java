package com.example.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** Entity mapped to table `employee`. */
@Data
@TableName("employee")
public class Employee {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String empNo;
    private String name;
    private Integer gender;
    private String dept;
    private String phone;
    private String idCard;
    private java.time.LocalDate entryDate;
    private Integer status;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Integer deleted;
    private Long tenantId;
}
