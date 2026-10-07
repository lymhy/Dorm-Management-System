package com.example.dorm.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** Unified API response wrapper. */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Result<T> {
    private int code;
    private String message;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(String message) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.message = message;
        return r;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> Result<T> fail(String message) {
        Result<T> r = new Result<>();
        r.code = 401;
        r.message = message;
        return r;
    }
}
