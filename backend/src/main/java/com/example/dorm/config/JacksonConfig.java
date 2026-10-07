package com.example.dorm.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * LocalDateTime 兼容前端常见的多种时间字符串格式。
 *
 * <p>application.yml 里的 spring.jackson.date-format 只对 java.util.Date 生效，
 * 对 java.time.LocalDateTime 无效，导致前端发来的 "yyyy-MM-dd HH:mm:ss" 无法解析。
 * 这里统一放宽：ISO（2026-10-07T16:54:14）、空格分隔（2026-10-07 16:54:14）、
 * 斜杠分隔（2026/10/7 16:54:14）均可接收；空白字符串按 null 处理。
 */
@Configuration
public class JacksonConfig {

    private static final List<DateTimeFormatter> FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss")
    );

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> builder.deserializerByType(LocalDateTime.class, new LenientLocalDateTimeDeserializer());
    }

    static class LenientLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String raw = p.getValueAsString();
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String text = raw.trim();
            for (DateTimeFormatter format : FORMATS) {
                try {
                    return LocalDateTime.parse(text, format);
                } catch (DateTimeParseException ignored) {
                    // 尝试下一种格式
                }
            }
            throw InvalidFormatException.from(p, "无法解析日期时间: " + raw, raw, LocalDateTime.class);
        }
    }
}