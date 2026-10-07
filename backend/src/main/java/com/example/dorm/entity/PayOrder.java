package com.example.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 在线支付订单 pay_order */
@Data
@TableName("pay_order")
public class PayOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long feeId;
    private Long roomId;
    private Long employeeId;
    private BigDecimal amount;
    private String month;
    private String channel;
    /** PENDING 待支付 / SUCCESS 成功 / CLOSED 关闭 */
    private String status;
    private String subject;
    private String outTradeNo;
    private LocalDateTime paidTime;
    private LocalDateTime expireTime;
    private String callbackPayload;
    private String remark;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Integer deleted;
    private Long tenantId;
}