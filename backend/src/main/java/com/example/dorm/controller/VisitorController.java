package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.Employee;
import com.example.dorm.entity.SysUser;
import com.example.dorm.entity.Visitor;
import com.example.dorm.service.EmployeeService;
import com.example.dorm.service.SysUserService;
import com.example.dorm.service.VisitorService;
import com.example.dorm.util.AuthUtil;
import com.example.dorm.util.Result;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/** CRUD REST API for Visitor. */
@RestController
@RequestMapping("/api/visitor")
public class VisitorController {

    @Resource
    private VisitorService service;
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

    @GetMapping("/page")
    public Result<Page<Visitor>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size) {
        return Result.success(service.page(new Page<>(current, size)));
    }

    // 当前员工的访客登记记录（必须在 /{id} 之前）
    @GetMapping("/my")
    public Result<Page<Visitor>> my(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        Long empId = getCurrentEmpId(auth);
        if (empId == null) return Result.fail("未登录");
        Page<Visitor> p = new Page<>(current, size);
        LambdaQueryWrapper<Visitor> q = new LambdaQueryWrapper<Visitor>()
            .eq(Visitor::getVisitorEmpId, empId).orderByDesc(Visitor::getId);
        service.page(p, q);
        return Result.success(p);
    }

    @GetMapping("/list")
    public Result<List<Visitor>> list() {
        return Result.success(service.list());
    }

    @GetMapping("/{id}")
    public Result<Visitor> get(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody Visitor entity) {
        return Result.success(service.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Visitor entity) {
        return Result.success(service.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(service.removeById(id));
    }
}
