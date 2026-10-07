package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.dorm.entity.SystemConfig;
import com.example.dorm.service.SystemConfigService;
import com.example.dorm.util.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** REST API for System Config. */
@RestController
@RequestMapping("/api/system-config")
public class SystemConfigController {

    @Resource
    private SystemConfigService service;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        List<SystemConfig> configs = service.list();
        List<Map<String, Object>> result = configs.stream().map(cfg -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", cfg.getCfgKey());
            m.put("value", cfg.getCfgValue());
            m.put("name", cfg.getDescription());
            m.put("unit", unitOf(cfg.getCfgKey()));
            return m;
        }).toList();
        return Result.success(result);
    }

    private String unitOf(String key) {
        if (key == null) return null;
        return switch (key) {
            case "water_price" -> "元/吨";
            case "elec_price" -> "元/度";
            case "default_deposit" -> "元";
            default -> null;
        };
    }

    @GetMapping
    public Result<String> get(@RequestParam String key) {
        LambdaQueryWrapper<SystemConfig> q = new LambdaQueryWrapper<>();
        q.eq(SystemConfig::getCfgKey, key);
        SystemConfig cfg = service.getOne(q);
        return Result.success(cfg != null ? cfg.getCfgValue() : "");
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Map<String, String> body) {
        String key = body.get("key");
        String value = body.get("value");
        LambdaQueryWrapper<SystemConfig> q = new LambdaQueryWrapper<>();
        q.eq(SystemConfig::getCfgKey, key);
        SystemConfig existing = service.getOne(q);
        if (existing != null) {
            return Result.success(service.update(new LambdaUpdateWrapper<SystemConfig>()
                .eq(SystemConfig::getCfgKey, key)
                .set(SystemConfig::getCfgValue, value)));
        } else {
            SystemConfig newCfg = new SystemConfig();
            newCfg.setCfgKey(key);
            newCfg.setCfgValue(value);
            return Result.success(service.save(newCfg));
        }
    }
}
