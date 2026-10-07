package com.example.dorm.dto;

import lombok.Data;

/** Login response payload. */
@Data
public class LoginVO {
    private String token;
    private String username;
    private String realName;
    private String role;
}
