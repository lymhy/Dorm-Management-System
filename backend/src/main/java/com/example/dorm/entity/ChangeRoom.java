package com.example.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("change_room")
public class ChangeRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private Long currentRoomId;
    private Integer targetType;  // 目标房型
    private String reason;
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
