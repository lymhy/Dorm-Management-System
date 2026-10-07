package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.Allocation;
import com.example.dorm.service.AllocationService;
import com.example.dorm.util.Result;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/** CRUD REST API for Allocation. */
@RestController
@RequestMapping("/api/allocation")
public class AllocationController {

    @Resource
    private AllocationService service;

    @GetMapping("/page")
    public Result<Page<Allocation>> page(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size) {
        return Result.success(service.page(new Page<>(current, size)));
    }

    @GetMapping("/list")
    public Result<List<Allocation>> list() {
        return Result.success(service.list());
    }

    @GetMapping("/{id}")
    public Result<Allocation> get(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody Allocation entity) {
        return Result.success(service.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Allocation entity) {
        return Result.success(service.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(service.removeById(id));
    }
}
