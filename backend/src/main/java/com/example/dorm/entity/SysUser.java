package com.example.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

/** Entity mapped to table `sys_user`. */
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    @TableField("real_name")
    private String realName;
    private String role;
    private String phone;
    private Integer status;
    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Integer deleted;
    private Long tenantId;

    // 显式 getter/setter，绕过 Lombok 在 JDK25 下对 realName 字段的失效问题
    public String getRealName() { return realName; }
    @JsonProperty("realName")
    public void setRealName(String realName) { this.realName = realName; }
}
