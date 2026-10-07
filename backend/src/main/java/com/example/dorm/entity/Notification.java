package com.example.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** Entity mapped to table `notification`. */
@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 接收员工ID (employee.id) */
    private Long empId;
    /** 类型: repair_accept / repair_finish / fee_warning / change_room_result */
    private String type;
    private String title;
    private String content;
    /** 关联业务ID */
    private Long bizId;
    /** 0未读 1已读 */
    private Integer isRead;
    private LocalDateTime createTime;
    private Integer deleted;
    private Long tenantId;
}
