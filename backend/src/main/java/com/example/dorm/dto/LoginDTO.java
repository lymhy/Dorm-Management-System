package com.example.dorm.dto;

import lombok.Data;

/** Login request payload. */
@Data
public class LoginDTO {
    private String username;
    private String password;
}
