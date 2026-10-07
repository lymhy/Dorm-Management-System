package com.example.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** Entity mapped to table `allocation`. */
@Data
@TableName("allocation")
public class Allocation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private Long roomId;
    private String bedNo;
    private java.math.BigDecimal deposit;
    private java.time.LocalDate checkInDate;
    private java.time.LocalDate checkOutDate;
    private Integer status;
    private String remark;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Integer deleted;
    private Long tenantId;
}
