package com.example.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 缴费流水 fee_payment */
@Data
@TableName("fee_payment")
public class FeePayment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long feeId;
    private Long roomId;
    private Long employeeId;
    private BigDecimal amount;
    private String month;
    private LocalDateTime payTime;
    private String payMethod;
    private String operator;
    private String remark;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Integer deleted;
    private Long tenantId;
}
