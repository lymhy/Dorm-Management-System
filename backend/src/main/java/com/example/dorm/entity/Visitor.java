package com.example.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** Entity mapped to table `visitor`. */
@Data
@TableName("visitor")
public class Visitor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String phone;
    private Long visitorEmpId;
    private Long roomId;
    private String reason;
    private java.time.LocalDateTime timeIn;
    private java.time.LocalDateTime timeOut;
    private String remark;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Integer deleted;
    private Long tenantId;
}
