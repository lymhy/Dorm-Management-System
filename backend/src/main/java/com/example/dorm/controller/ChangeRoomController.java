package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.*;
import com.example.dorm.service.*;
import com.example.dorm.util.NotificationHelper;
import com.example.dorm.util.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** REST API for Change Room Application. */
@RestController
@RequestMapping("/api/change-room")
public class ChangeRoomController {

    @Resource
    private ChangeRoomService changeRoomService;
    @Resource
    private EmployeeService employeeService;
    @Resource
    private RoomService roomService;
    @Resource
    private BuildingService buildingService;
    @Resource
    private NotificationHelper notificationHelper;

    @GetMapping("/page")
    public Result<Page<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        Page<ChangeRoom> p = new Page<>(current, size);
        LambdaQueryWrapper<ChangeRoom> q = new LambdaQueryWrapper<>();
        if (status != null) q.eq(ChangeRoom::getStatus, status);
        q.orderByDesc(ChangeRoom::getCreateTime);
        changeRoomService.page(p, q);

        Page<Map<String, Object>> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ChangeRoom cr : p.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", cr.getId());
            row.put("employeeId", cr.getEmployeeId());
            row.put("currentRoomId", cr.getCurrentRoomId());
            row.put("targetType", cr.getTargetType());
            row.put("reason", cr.getReason());
            row.put("status", cr.getStatus());
            row.put("reply", cr.getReply());
            row.put("createTime", cr.getCreateTime());

            // 联表：员工姓名
            if (cr.getEmployeeId() != null) {
                Employee emp = employeeService.getById(cr.getEmployeeId());
                row.put("employeeName", emp != null ? emp.getName() : "");
            }
            // 联表：当前房间 → 楼栋+房间号
            if (cr.getCurrentRoomId() != null) {
                Room room = roomService.getById(cr.getCurrentRoomId());
                if (room != null) {
                    Building b = buildingService.getById(room.getBuildingId());
                    String buildingName = b != null ? b.getName() : "";
                    row.put("currentRoom", Map.of(
                        "buildingName", buildingName,
                        "roomNo", room.getRoomNo() != null ? room.getRoomNo() : ""
                    ));
                    row.put("targetRoomType", cr.getTargetType()); // 方便前端显示
                }
            }
            rows.add(row);
        }
        result.setRecords(rows);
        return Result.success(result);
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody ChangeRoom entity) {
        return Result.success(changeRoomService.save(entity));
    }

    @PutMapping("/{id}/approve")
    public Result<Boolean> approve(@PathVariable Long id) {
        ChangeRoom r = changeRoomService.getById(id);
        r.setStatus(1);
        r.setReplyTime(LocalDateTime.now());
        boolean ok = changeRoomService.updateById(r);
        if (ok) {
            notificationHelper.send(r.getEmployeeId(), "change_room_result", "调宿申请已通过",
                    "您提交的调宿申请已通过审批，请前往「入住信息」查看最新房间安排。", id);
        }
        return Result.success(ok);
    }

    @PutMapping("/{id}/reject")
    public Result<Boolean> reject(@PathVariable Long id, @RequestBody ChangeRoom body) {
        ChangeRoom r = changeRoomService.getById(id);
        r.setStatus(2);
        r.setReply(body.getReply());
        r.setReplyTime(LocalDateTime.now());
        boolean ok = changeRoomService.updateById(r);
        if (ok) {
            String reply = body.getReply() == null ? "" : body.getReply();
            notificationHelper.send(r.getEmployeeId(), "change_room_result", "调宿申请未通过",
                    "您提交的调宿申请未通过：" + reply, id);
        }
        return Result.success(ok);
    }
}
