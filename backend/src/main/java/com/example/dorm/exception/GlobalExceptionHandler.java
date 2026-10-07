package com.example.dorm.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.example.dorm.util.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Global exception handler returning unified error JSON. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        return Result.error(401, "未登录或登录已过期");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handle(Exception e) {
        e.printStackTrace();
        return Result.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }
}
