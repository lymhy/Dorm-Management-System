package com.example.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("move_out")
public class MoveOut {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private Long roomId;
    @JsonProperty("moveOutDate")
    private LocalDate checkOutDate;
    private BigDecimal depositRefund;
    private Integer status;      // 0待审批 1通过 2拒绝
    private String reply;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime replyTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
    private Long tenantId;
}
