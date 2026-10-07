package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.*;
import com.example.dorm.service.*;
import com.example.dorm.util.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** REST API for Move Out Application. */
@RestController
@RequestMapping("/api/move-out")
public class MoveOutController {

    @Resource
    private MoveOutService moveOutService;
    @Resource
    private EmployeeService employeeService;
    @Resource
    private RoomService roomService;
    @Resource
    private BuildingService buildingService;

    @GetMapping("/page")
    public Result<Page<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        Page<MoveOut> p = new Page<>(current, size);
        LambdaQueryWrapper<MoveOut> q = new LambdaQueryWrapper<>();
        if (status != null) q.eq(MoveOut::getStatus, status);
        q.orderByDesc(MoveOut::getCreateTime);
        moveOutService.page(p, q);

        Page<Map<String, Object>> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MoveOut mo : p.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", mo.getId());
            row.put("employeeId", mo.getEmployeeId());
            row.put("roomId", mo.getRoomId());
            row.put("moveOutDate", mo.getCheckOutDate()); // @JsonProperty 已在 MoveOut entity 处理
            row.put("depositRefund", mo.getDepositRefund());
            row.put("status", mo.getStatus());
            row.put("reply", mo.getReply());
            row.put("createTime", mo.getCreateTime());

            // 联表：员工姓名
            if (mo.getEmployeeId() != null) {
                Employee emp = employeeService.getById(mo.getEmployeeId());
                row.put("employeeName", emp != null ? emp.getName() : "");
            }
            // 联表：房间 → 楼栋+房间号
            if (mo.getRoomId() != null) {
                Room room = roomService.getById(mo.getRoomId());
                if (room != null) {
                    Building b = buildingService.getById(room.getBuildingId());
                    String buildingName = b != null ? b.getName() : "";
                    row.put("room", Map.of(
                        "buildingName", buildingName,
                        "roomNo", room.getRoomNo() != null ? room.getRoomNo() : ""
                    ));
                }
            }
            rows.add(row);
        }
        result.setRecords(rows);
        return Result.success(result);
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody MoveOut entity) {
        return Result.success(moveOutService.save(entity));
    }

    @PutMapping("/{id}/approve")
    public Result<Boolean> approve(@PathVariable Long id) {
        MoveOut r = moveOutService.getById(id);
        r.setStatus(1);
        r.setReplyTime(LocalDateTime.now());
        return Result.success(moveOutService.updateById(r));
    }

    @PutMapping("/{id}/reject")
    public Result<Boolean> reject(@PathVariable Long id, @RequestBody MoveOut body) {
        MoveOut r = moveOutService.getById(id);
        r.setStatus(2);
        r.setReply(body.getReply());
        r.setReplyTime(LocalDateTime.now());
        return Result.success(moveOutService.updateById(r));
    }
}
