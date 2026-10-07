package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.Employee;
import com.example.dorm.entity.Notification;
import com.example.dorm.entity.SysUser;
import com.example.dorm.service.EmployeeService;
import com.example.dorm.service.NotificationService;
import com.example.dorm.service.SysUserService;
import com.example.dorm.util.AuthUtil;
import com.example.dorm.util.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST API for Notification (消息通知). */
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Resource
    private NotificationService service;
    @Resource
    private EmployeeService employeeService;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private AuthUtil authUtil;

    private Long getCurrentEmpId(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String username = authUtil.parseUsername(auth.substring(7));
        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) return null;
        Employee emp = employeeService.getOne(new LambdaQueryWrapper<Employee>().eq(Employee::getName, user.getRealName()));
        return emp != null ? emp.getId() : null;
    }

    /** 当前员工的通知分页（isRead 可选：0未读 1已读） */
    @GetMapping("/my")
    public Result<Page<Notification>> my(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) Integer isRead,
                                         @RequestHeader(value = "Authorization", required = false) String auth) {
        Long empId = getCurrentEmpId(auth);
        if (empId == null) return Result.success(new Page<>(current, size));
        Page<Notification> p = new Page<>(current, size);
        LambdaQueryWrapper<Notification> q = new LambdaQueryWrapper<Notification>()
            .eq(Notification::getEmpId, empId);
        if (isRead != null) q.eq(Notification::getIsRead, isRead);
        q.orderByDesc(Notification::getId);
        service.page(p, q);
        return Result.success(p);
    }

    /** 未读数量（用于角标） */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestHeader(value = "Authorization", required = false) String auth) {
        Long empId = getCurrentEmpId(auth);
        if (empId == null) return Result.success(0L);
        long count = service.count(new LambdaQueryWrapper<Notification>()
            .eq(Notification::getEmpId, empId).eq(Notification::getIsRead, 0));
        return Result.success(count);
    }

    /** 全部标记为已读 */
    @PutMapping("/read-all")
    public Result<Boolean> readAll(@RequestHeader(value = "Authorization", required = false) String auth) {
        Long empId = getCurrentEmpId(auth);
        if (empId == null) return Result.success(true);
        service.update(new LambdaUpdateWrapper<Notification>()
            .eq(Notification::getEmpId, empId).eq(Notification::getIsRead, 0)
            .set(Notification::getIsRead, 1));
        return Result.success(true);
    }

    /** 单条标记为已读 */
    @PutMapping("/{id}/read")
    public Result<Boolean> read(@PathVariable Long id) {
        Notification n = service.getById(id);
        if (n == null) return Result.error("通知不存在");
        n.setIsRead(1);
        return Result.success(service.updateById(n));
    }

    /** 单条标记为未读 */
    @PutMapping("/{id}/unread")
    public Result<Boolean> unread(@PathVariable Long id) {
        Notification n = service.getById(id);
        if (n == null) return Result.error("通知不存在");
        n.setIsRead(0);
        return Result.success(service.updateById(n));
    }

    @GetMapping("/list")
    public Result<List<Notification>> list() {
        return Result.success(service.list());
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(service.removeById(id));
    }
}
