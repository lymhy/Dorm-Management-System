package com.example.dorm.controller;

import com.example.dorm.dto.LoginDTO;
import com.example.dorm.dto.LoginVO;
import com.example.dorm.entity.SysUser;
import com.example.dorm.mapper.SysUserMapper;
import com.example.dorm.service.SysUserService;
import com.example.dorm.util.AuthUtil;
import com.example.dorm.util.Result;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/** Authentication: login/logout/info. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserMapper userMapper;
    private final AuthUtil authUtil;
    private final SysUserService sysUserService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(SysUserMapper userMapper, AuthUtil authUtil, SysUserService sysUserService) {
        this.userMapper = userMapper;
        this.authUtil = authUtil;
        this.sysUserService = sysUserService;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.error(401, "username or password incorrect");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error(403, "account disabled");
        }
        String token = authUtil.generateToken(user.getUsername());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        return Result.success(vo);
    }

    @GetMapping("/info")
    public Result<LoginVO> info(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return Result.fail("未登录");
        String username = authUtil.parseUsername(auth.substring(7));
        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));
        if (user == null) return Result.fail("用户不存在");
        LoginVO vo = new LoginVO();
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        return Result.success(vo);
    }

    @PutMapping("/password")
    public Result<Boolean> changePassword(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, String> body) {
        if (auth == null || !auth.startsWith("Bearer ")) return Result.fail("未登录");
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (oldPwd == null || newPwd == null) return Result.fail("参数不完整");
        String token = auth.substring(7);
        String username = authUtil.parseUsername(token);
        SysUser user = sysUserService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));
        if (user == null) return Result.fail("用户不存在");
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) return Result.fail("旧密码错误");
        user.setPassword(passwordEncoder.encode(newPwd));
        return Result.success(sysUserService.updateById(user));
    }
}
