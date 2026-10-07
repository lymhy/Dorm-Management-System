package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.Announcement;
import com.example.dorm.service.AnnouncementService;
import com.example.dorm.util.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD REST API for Announcement. */
@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    @Resource
    private AnnouncementService service;

    @GetMapping("/page")
    public Result<Page<Announcement>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        Page<Announcement> p = new Page<>(current, size);
        LambdaQueryWrapper<Announcement> q = new LambdaQueryWrapper<>();
        q.orderByDesc(Announcement::getCreateTime);
        service.page(p, q);
        return Result.success(p);
    }

    @GetMapping("/latest")
    public Result<List<Announcement>> latest() {
        LambdaQueryWrapper<Announcement> q = new LambdaQueryWrapper<>();
        q.orderByDesc(Announcement::getCreateTime).last("LIMIT 5");
        return Result.success(service.list(q));
    }

    @GetMapping("/{id}")
    public Result<Announcement> get(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody Announcement entity) {
        return Result.success(service.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Announcement entity) {
        return Result.success(service.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(service.removeById(id));
    }
}
