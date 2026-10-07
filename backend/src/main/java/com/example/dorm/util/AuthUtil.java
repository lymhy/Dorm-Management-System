package com.example.dorm.util;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/** Auth helper backed by Sa-Token; tokens are stateless JWTs. */
@Component
public class AuthUtil {

    /** Log in the user and return the issued token. */
    public String generateToken(String username) {
        StpUtil.login(username);
        return StpUtil.getTokenValue();
    }

    /** Resolve the username carried by the given token, or null when the token is invalid. */
    public String parseUsername(String token) {
        Object loginId = StpUtil.getLoginIdByToken(token);
        return loginId == null ? null : String.valueOf(loginId);
    }
}