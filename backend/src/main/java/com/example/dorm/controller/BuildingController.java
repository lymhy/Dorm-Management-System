package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.Building;
import com.example.dorm.service.BuildingService;
import com.example.dorm.util.Result;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/** CRUD REST API for Building. */
@RestController
@RequestMapping("/api/building")
public class BuildingController {

    @Resource
    private BuildingService service;

    @GetMapping("/page")
    public Result<Page<Building>> page(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size,
                                   @RequestParam(required = false) String keyword) {
        Page<Building> p = new Page<>(current, size);
        LambdaQueryWrapper<Building> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            keyword = keyword.trim();
            q.like(Building::getName, keyword)
              .or().like(Building::getCode, keyword)
              .or().like(Building::getAddress, keyword)
              .or().like(Building::getManager, keyword);
        }
        q.orderByDesc(Building::getId);
        service.page(p, q);
        return Result.success(p);
    }

    @GetMapping("/list")
    public Result<List<Building>> list() {
        return Result.success(service.list());
    }

    @GetMapping("/{id}")
    public Result<Building> get(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody Building entity) {
        return Result.success(service.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Building entity) {
        return Result.success(service.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(service.removeById(id));
    }
}
