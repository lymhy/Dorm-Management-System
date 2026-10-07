package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.Room;
import com.example.dorm.service.RoomService;
import com.example.dorm.util.Result;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/** CRUD REST API for Room. */
@RestController
@RequestMapping("/api/room")
public class RoomController {

    @Resource
    private RoomService service;

    @GetMapping("/page")
    public Result<Page<Room>> page(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size,
                                   @RequestParam(required = false) Long buildingId,
                                   @RequestParam(required = false) Integer floor,
                                   @RequestParam(required = false) Integer status) {
        Page<Room> p = new Page<>(current, size);
        LambdaQueryWrapper<Room> q = new LambdaQueryWrapper<>();
        if (buildingId != null) q.eq(Room::getBuildingId, buildingId);
        if (floor != null && floor > 0) q.eq(Room::getFloor, floor);
        if (status != null) q.eq(Room::getStatus, status);
        q.orderByDesc(Room::getId);
        service.page(p, q);
        return Result.success(p);
    }

    @GetMapping("/list")
    public Result<List<Room>> list() {
        return Result.success(service.list());
    }

    @GetMapping("/{id}")
    public Result<Room> get(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody Room entity) {
        return Result.success(service.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Room entity) {
        return Result.success(service.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(service.removeById(id));
    }
}
