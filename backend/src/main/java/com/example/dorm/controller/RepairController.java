package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.Employee;
import com.example.dorm.entity.Repair;
import com.example.dorm.entity.SysUser;
import com.example.dorm.service.EmployeeService;
import com.example.dorm.service.RepairService;
import com.example.dorm.service.SysUserService;
import com.example.dorm.util.AuthUtil;
import com.example.dorm.util.NotificationHelper;
import com.example.dorm.util.Result;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/** CRUD REST API for Repair. */
@RestController
@RequestMapping("/api/repair")
public class RepairController {

    @Resource
    private RepairService service;
    @Resource
    private EmployeeService employeeService;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private AuthUtil authUtil;
    @Resource
    private NotificationHelper notificationHelper;

    private SysUser currentUser(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String username = authUtil.parseUsername(auth.substring(7));
        return sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    private Long getCurrentEmpId(String auth) {
        SysUser user = currentUser(auth);
        if (user == null) return null;
        Employee emp = employeeService.getOne(new LambdaQueryWrapper<Employee>().eq(Employee::getName, user.getRealName()));
        return emp != null ? emp.getId() : null;
    }

    @GetMapping("/page")
    public Result<Page<Repair>> page(@RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "10") long size) {
        return Result.success(service.page(new Page<>(current, size)));
    }

    // 当前员工的报修记录（必须在 /{id} 之前）
    @GetMapping("/my")
    public Result<Page<Repair>> my(@RequestParam(defaultValue = "1") long current,
                                    @RequestParam(defaultValue = "10") long size,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        Long empId = getCurrentEmpId(auth);
        if (empId == null) return Result.fail("未登录");
        Page<Repair> p = new Page<>(current, size);
        LambdaQueryWrapper<Repair> q = new LambdaQueryWrapper<Repair>()
            .eq(Repair::getEmployeeId, empId).orderByDesc(Repair::getId);
        service.page(p, q);
        return Result.success(p);
    }

    @GetMapping("/list")
    public Result<List<Repair>> list() {
        return Result.success(service.list());
    }

    @GetMapping("/{id}")
    public Result<Repair> get(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody Repair entity,
                               @RequestHeader(value = "Authorization", required = false) String auth) {
        SysUser user = currentUser(auth);
        if (user != null && !"admin".equals(user.getRole())) {
            // 员工提交的报修：报修人取登录身份，状态固定为待处理，处理人与处理备注由管理员分配时填写
            entity.setEmployeeId(getCurrentEmpId(auth));
            entity.setStatus(0);
            entity.setHandler(null);
            entity.setHandleRemark(null);
        }
        if (entity.getReportTime() == null) {
            entity.setReportTime(LocalDateTime.now());
        }
        return Result.success(service.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Repair entity) {
        Repair old = entity.getId() != null ? service.getById(entity.getId()) : null;
        boolean ok = service.updateById(entity);
        if (ok && entity.getStatus() != null) {
            Long empId = entity.getEmployeeId() != null ? entity.getEmployeeId()
                    : (old != null ? old.getEmployeeId() : null);
            Integer oldStatus = old != null ? old.getStatus() : null;
            String title = old != null ? old.getTitle() : entity.getTitle();
            String t = title == null ? "" : title;
            if (empId != null && entity.getStatus() == 1 && (oldStatus == null || oldStatus != 1)) {
                notificationHelper.send(empId, "repair_accept", "报修已接单",
                        "您的报修「" + t + "」已接单，维修师傅正在处理，请留意进度。", entity.getId());
            } else if (empId != null && entity.getStatus() == 2 && (oldStatus == null || oldStatus != 2)) {
                notificationHelper.send(empId, "repair_finish", "维修已完成",
                        "您的报修「" + t + "」已处理完成，感谢您的反馈。", entity.getId());
            }
        }
        return Result.success(ok);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(service.removeById(id));
    }
}
