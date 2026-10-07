package com.example.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** Entity mapped to table `repair`. */
@Data
@TableName("repair")
public class Repair {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long employeeId;
    private String title;
    private String description;
    private Integer status;
    private String handler;
    private String handleRemark;
    private java.time.LocalDateTime reportTime;
    private java.time.LocalDateTime finishTime;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Integer deleted;
    private Long tenantId;
}
