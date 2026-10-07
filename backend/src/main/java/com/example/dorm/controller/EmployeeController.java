package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.Allocation;
import com.example.dorm.entity.Building;
import com.example.dorm.entity.Employee;
import com.example.dorm.entity.Room;
import com.example.dorm.entity.SysUser;
import com.example.dorm.service.AllocationService;
import com.example.dorm.service.BuildingService;
import com.example.dorm.service.EmployeeService;
import com.example.dorm.service.RoomService;
import com.example.dorm.service.SysUserService;
import com.example.dorm.util.AuthUtil;
import com.example.dorm.util.Result;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** CRUD REST API for Employee. */
@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    @Resource
    private EmployeeService service;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private AllocationService allocationService;
    @Resource
    private RoomService roomService;
    @Resource
    private BuildingService buildingService;
    @Resource
    private AuthUtil authUtil;

    @GetMapping("/page")
    public Result<Page<Employee>> page(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) String dept,
                                   @RequestParam(required = false) Integer gender) {
        Page<Employee> p = new Page<>(current, size);
        LambdaQueryWrapper<Employee> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            // 用 and(...) 包裹 OR 组，避免与后面的 AND 条件优先级错乱
            q.and(w -> w.like(Employee::getName, k)
                    .or().like(Employee::getEmpNo, k)
                    .or().like(Employee::getDept, k)
                    .or().like(Employee::getPhone, k));
        }
        if (status != null) q.eq(Employee::getStatus, status);
        if (dept != null && !dept.isBlank()) q.eq(Employee::getDept, dept.trim());
        if (gender != null) q.eq(Employee::getGender, gender);
        q.orderByDesc(Employee::getId);
        service.page(p, q);
        return Result.success(p);
    }

    /** 部门列表（供筛选下拉使用，必须在 /{id} 之前） */
    @GetMapping("/depts")
    public Result<List<String>> depts() {
        List<String> ds = service.list().stream()
                .map(Employee::getDept)
                .filter(d -> d != null && !d.isBlank())
                .distinct()
                .sorted()
                .toList();
        return Result.success(ds);
    }

    @GetMapping("/list")
    public Result<List<Employee>> list() {
        return Result.success(service.list());
    }

    @GetMapping("/{id}")
    public Result<Employee> get(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody Employee entity) {
        return Result.success(service.save(entity));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Employee entity) {
        return Result.success(service.updateById(entity));
    }

    // 获取当前登录员工自己的信息（必须在 /{id} 之前）
    @GetMapping("/me")
    public Result<Employee> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return Result.fail("未登录");
        String token = auth.substring(7);
        String username = authUtil.parseUsername(token);
        // sys_user.real_name = employee.name（固定映射）
        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) return Result.fail("用户不存在");
        String realName = user.getRealName();
        Employee emp = service.getOne(new LambdaQueryWrapper<Employee>().eq(Employee::getName, realName));
        return Result.success(emp);
    }

    // 获取当前员工的入住信息（含房间详情）（必须在 /{id} 之前）
    @GetMapping("/my-allocation")
    public Result<Map<String, Object>> myAllocation(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return Result.fail("未登录");
        String token = auth.substring(7);
        String username = authUtil.parseUsername(token);
        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) return Result.fail("用户不存在");
        String realName = user.getRealName();
        Employee emp = service.getOne(new LambdaQueryWrapper<Employee>().eq(Employee::getName, realName));
        Map<String, Object> result = new HashMap<>();
        result.put("employee", emp);
        if (emp == null) return Result.success(result);

        Allocation allocation = allocationService.getOne(new LambdaQueryWrapper<Allocation>()
            .eq(Allocation::getEmployeeId, emp.getId()).eq(Allocation::getStatus, 1));
        result.put("allocation", allocation);
        if (allocation != null) {
            Room room = roomService.getById(allocation.getRoomId());
            result.put("room", room);
            if (room != null) {
                Building building = buildingService.getById(room.getBuildingId());
                result.put("building", building);
            }
        }
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(service.removeById(id));
    }
}
