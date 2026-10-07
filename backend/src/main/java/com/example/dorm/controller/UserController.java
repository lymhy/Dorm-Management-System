package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.SysUser;
import com.example.dorm.service.SysUserService;
import com.example.dorm.util.Result;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 用户账号管理：增删改查、批量导入、重置密码、启用/禁用。 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final String DEFAULT_PASSWORD = "123456";

    @Resource
    private SysUserService service;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 分页查询（keyword 匹配用户名/姓名/手机号，role/status 可筛选） */
    @GetMapping("/page")
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String role,
                                      @RequestParam(required = false) Integer status) {
        Page<SysUser> p = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            q.and(w -> w.like(SysUser::getUsername, k)
                    .or().like(SysUser::getRealName, k)
                    .or().like(SysUser::getPhone, k));
        }
        if (role != null && !role.isBlank()) q.eq(SysUser::getRole, role.trim());
        if (status != null) q.eq(SysUser::getStatus, status);
        q.orderByDesc(SysUser::getId);
        service.page(p, q);
        if (p.getRecords() != null) p.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(p);
    }

    @GetMapping("/{id}")
    public Result<SysUser> get(@PathVariable Long id) {
        SysUser u = service.getById(id);
        if (u != null) u.setPassword(null);
        return Result.success(u);
    }

    /** 新增账号（密码为空时用默认密码 123456） */
    @PostMapping
    public Result<Boolean> add(@RequestBody SysUser entity) {
        if (entity.getUsername() == null || entity.getUsername().isBlank())
            return Result.error("用户名不能为空");
        String uname = entity.getUsername().trim();
        long exists = service.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, uname));
        if (exists > 0) return Result.error("用户名已存在");
        entity.setUsername(uname);
        if (entity.getPassword() == null || entity.getPassword().isBlank()) entity.setPassword(DEFAULT_PASSWORD);
        entity.setPassword(encoder.encode(entity.getPassword()));
        if (entity.getRole() == null || entity.getRole().isBlank()) entity.setRole("employee");
        if (entity.getStatus() == null) entity.setStatus(1);
        return Result.success(service.save(entity));
    }

    /** 编辑账号（密码留空表示不修改） */
    @PutMapping
    public Result<Boolean> update(@RequestBody SysUser entity) {
        if (entity.getId() == null) return Result.error("缺少ID");
        SysUser old = service.getById(entity.getId());
        if (old == null) return Result.error("用户不存在");
        if (entity.getUsername() != null && !entity.getUsername().isBlank()
                && !entity.getUsername().trim().equals(old.getUsername())) {
            long exists = service.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, entity.getUsername().trim()));
            if (exists > 0) return Result.error("用户名已存在");
            entity.setUsername(entity.getUsername().trim());
        } else {
            entity.setUsername(null);
        }
        if (entity.getPassword() == null || entity.getPassword().isBlank()) {
            entity.setPassword(null);
        } else {
            entity.setPassword(encoder.encode(entity.getPassword()));
        }
        return Result.success(service.updateById(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        SysUser u = service.getById(id);
        if (u != null && "admin".equalsIgnoreCase(u.getUsername())) return Result.error("内置管理员账号不可删除");
        return Result.success(service.removeById(id));
    }

    /** 重置密码（不传则重置为默认 123456） */
    @PutMapping("/{id}/reset-password")
    public Result<Boolean> resetPassword(@PathVariable Long id,
                                         @RequestBody(required = false) Map<String, String> body) {
        SysUser u = service.getById(id);
        if (u == null) return Result.error("用户不存在");
        String pwd = (body != null && body.get("password") != null && !body.get("password").isBlank())
                ? body.get("password") : DEFAULT_PASSWORD;
        SysUser upd = new SysUser();
        upd.setId(id);
        upd.setPassword(encoder.encode(pwd));
        return Result.success(service.updateById(upd));
    }

    /** 启用 / 禁用账号 */
    @PutMapping("/{id}/status")
    public Result<Boolean> setStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        SysUser u = service.getById(id);
        if (u == null) return Result.error("用户不存在");
        Integer status = body == null ? null : body.get("status");
        if (status == null) return Result.error("缺少状态值");
        if ("admin".equalsIgnoreCase(u.getUsername()) && status == 0) return Result.error("内置管理员账号不可禁用");
        SysUser upd = new SysUser();
        upd.setId(id);
        upd.setStatus(status);
        return Result.success(service.updateById(upd));
    }

    /** 批量导入（前端解析 Excel 后提交 JSON 数组） */
    @PostMapping("/import")
    public Result<Map<String, Object>> importUsers(@RequestBody List<SysUser> rows) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        if (rows == null) rows = Collections.emptyList();
        for (int i = 0; i < rows.size(); i++) {
            SysUser r = rows.get(i);
            int line = i + 2; // 表头占第1行
            try {
                if (r.getUsername() == null || r.getUsername().isBlank()) {
                    errors.add("第" + line + "行：用户名为空");
                    continue;
                }
                String uname = r.getUsername().trim();
                long exists = service.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, uname));
                if (exists > 0) {
                    errors.add("第" + line + "行：用户名「" + uname + "」已存在");
                    continue;
                }
                SysUser u = new SysUser();
                u.setUsername(uname);
                u.setRealName(r.getRealName() == null ? "" : r.getRealName().trim());
                u.setRole((r.getRole() == null || r.getRole().isBlank()) ? "employee" : r.getRole().trim());
                u.setPhone(r.getPhone() == null ? null : r.getPhone().trim());
                u.setStatus(r.getStatus() == null ? 1 : r.getStatus());
                String pwd = (r.getPassword() == null || r.getPassword().isBlank()) ? DEFAULT_PASSWORD : r.getPassword();
                u.setPassword(encoder.encode(pwd));
                service.save(u);
                success++;
            } catch (Exception e) {
                errors.add("第" + line + "行：" + e.getMessage());
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total", rows.size());
        res.put("success", success);
        res.put("fail", rows.size() - success);
        res.put("errors", errors);
        return Result.success(res);
    }
}
