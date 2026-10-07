package com.example.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** Entity mapped to table `fee`. */
@Data
@TableName("fee")
public class Fee {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String month;
    private java.math.BigDecimal waterUsage;
    private java.math.BigDecimal elecUsage;
    private java.math.BigDecimal waterFee;
    private java.math.BigDecimal elecFee;
    private java.math.BigDecimal total;
    private Integer paid;
    private java.time.LocalDate dueDate;
    private String remark;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Integer deleted;
    private Long tenantId;
}
