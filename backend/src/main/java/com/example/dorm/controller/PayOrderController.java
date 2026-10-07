package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.Allocation;
import com.example.dorm.entity.Employee;
import com.example.dorm.entity.Fee;
import com.example.dorm.entity.PayOrder;
import com.example.dorm.entity.Room;
import com.example.dorm.entity.SysUser;
import com.example.dorm.service.AllocationService;
import com.example.dorm.service.EmployeeService;
import com.example.dorm.service.FeeService;
import com.example.dorm.service.PayOrderService;
import com.example.dorm.service.RoomService;
import com.example.dorm.service.SysUserService;
import com.example.dorm.util.AuthUtil;
import com.example.dorm.util.Result;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线支付接口。
 * 员工: 对本人房间的未缴账单下单 -> (模拟)确认支付 -> 查看订单
 * 管理员: 查看全部订单流水
 */
@RestController
@RequestMapping("/api/pay")
public class PayOrderController {

    @Resource
    private PayOrderService payOrderService;
    @Resource
    private FeeService feeService;
    @Resource
    private AllocationService allocationService;
    @Resource
    private EmployeeService employeeService;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private RoomService roomService;
    @Resource
    private AuthUtil authUtil;

    /** 当前登录用户对应的员工ID（与 FeeController 同规则：按 realName 匹配） */
    private Long getCurrentEmpId(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String username;
        try {
            username = authUtil.parseUsername(auth.substring(7));
        } catch (Exception e) {
            return null;
        }
        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) return null;
        Employee emp = employeeService.getOne(new LambdaQueryWrapper<Employee>().eq(Employee::getName, user.getRealName()));
        return emp != null ? emp.getId() : null;
    }

    private String currentUsername(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        try {
            return authUtil.parseUsername(auth.substring(7));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAdmin(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return false;
        try {
            SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, authUtil.parseUsername(auth.substring(7))));
            return user != null && "admin".equals(user.getRole());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 创建支付订单。员工只能为本人房间的账单下单；管理员可代任意账单下单。
     * 返回: 订单信息 + 收银参数（payParams，模拟通道为收银台地址）
     */
    @PostMapping("/order")
    public Result<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body,
                                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        Long feeId = parseLong(body.get("feeId"));
        if (feeId == null) return Result.error("缺少账单ID");
        String channel = body.get("channel") == null ? "mock" : String.valueOf(body.get("channel"));

        Fee fee = feeService.getById(feeId);
        if (fee == null) return Result.error("账单不存在");
        if (fee.getPaid() != null && fee.getPaid() == 1) return Result.error("该账单已缴纳，无需重复支付");

        Long empId;
        String operator = currentUsername(auth);
        if (isAdmin(auth)) {
            // 管理员代付：优先取房间在住员工
            empId = null;
            if (fee.getRoomId() != null) {
                Allocation alloc = allocationService.getOne(new LambdaQueryWrapper<Allocation>()
                        .eq(Allocation::getRoomId, fee.getRoomId()).eq(Allocation::getStatus, 1));
                if (alloc != null) empId = alloc.getEmployeeId();
            }
        } else {
            empId = getCurrentEmpId(auth);
            if (empId == null) return Result.error("未找到员工信息，无法在线缴费");
            // 越权校验：只能支付本人入住房间的账单
            Allocation alloc = allocationService.getOne(new LambdaQueryWrapper<Allocation>()
                    .eq(Allocation::getEmployeeId, empId).eq(Allocation::getStatus, 1));
            if (alloc == null || !alloc.getRoomId().equals(fee.getRoomId())) {
                return Result.error("只能支付本人房间的账单");
            }
        }

        try {
            PayOrder order = payOrderService.createOrder(feeId, empId, channel, operator);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("orderNo", order.getOrderNo());
            res.put("amount", order.getAmount());
            res.put("month", order.getMonth());
            res.put("status", order.getStatus());
            res.put("expireTime", order.getExpireTime());
            res.put("payParams", order.getRemark());
            return Result.success(res);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 确认支付（模拟渠道回调）。真实渠道接入后，此逻辑由异步回调接口承担，
     * 并需校验渠道签名；当前由收银台页在用户确认后调用。
     */
    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirm(@RequestBody Map<String, Object> body) {
        String orderNo = body.get("orderNo") == null ? null : String.valueOf(body.get("orderNo")).trim();
        if (orderNo == null || orderNo.isBlank()) return Result.error("缺少订单号");
        String outTradeNo = body.get("outTradeNo") == null ? "MOCK" + System.currentTimeMillis()
                : String.valueOf(body.get("outTradeNo"));
        try {
            PayOrder order = payOrderService.confirmPaid(orderNo, outTradeNo, String.valueOf(body));
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("orderNo", order.getOrderNo());
            res.put("status", order.getStatus());
            res.put("paidTime", order.getPaidTime());
            res.put("amount", order.getAmount());
            return Result.success(res);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 关闭/取消订单 */
    @PostMapping("/close")
    public Result<Boolean> close(@RequestBody Map<String, Object> body) {
        String orderNo = body.get("orderNo") == null ? null : String.valueOf(body.get("orderNo")).trim();
        if (orderNo == null || orderNo.isBlank()) return Result.error("缺少订单号");
        String reason = body.get("reason") == null ? "用户关闭" : String.valueOf(body.get("reason"));
        return Result.success(payOrderService.closeOrder(orderNo, reason));
    }

    /** 订单详情（含关联账单，用于收银台展示） */
    @GetMapping("/order/{orderNo}")
    public Result<Map<String, Object>> orderDetail(@PathVariable String orderNo) {
        PayOrder order = payOrderService.getByOrderNo(orderNo);
        if (order == null) return Result.error("订单不存在");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("orderNo", order.getOrderNo());
        res.put("feeId", order.getFeeId());
        res.put("amount", order.getAmount());
        res.put("month", order.getMonth());
        res.put("status", order.getStatus());
        res.put("channel", order.getChannel());
        res.put("subject", order.getSubject());
        res.put("expireTime", order.getExpireTime());
        res.put("paidTime", order.getPaidTime());
        if (order.getFeeId() != null) {
            Fee fee = feeService.getById(order.getFeeId());
            if (fee != null) {
                res.put("feePaid", fee.getPaid());
                res.put("total", fee.getTotal());
            }
        }
        return Result.success(res);
    }

    /** 员工的订单列表（最近 50 条） */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> my(@RequestHeader(value = "Authorization", required = false) String auth) {
        Long empId = getCurrentEmpId(auth);
        if (empId == null) return Result.error("未找到员工信息");
        List<PayOrder> orders = payOrderService.myOrders(empId, 50);
        return Result.success(orders.stream().map(this::toRow).toList());
    }

    /** 管理员：全部订单分页 */
    @GetMapping("/page")
    public Result<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String channel,
                                                  @RequestHeader(value = "Authorization", required = false) String auth) {
        if (!isAdmin(auth)) return Result.error("无权限");
        LambdaQueryWrapper<PayOrder> q = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) q.eq(PayOrder::getStatus, status.trim());
        if (channel != null && !channel.isBlank()) q.eq(PayOrder::getChannel, channel.trim());
        q.orderByDesc(PayOrder::getId);
        Page<PayOrder> p = payOrderService.page(new Page<>(current, size), q);
        Page<Map<String, Object>> result = new Page<>(current, size, p.getTotal());
        result.setRecords(p.getRecords().stream().map(this::toRow).toList());
        return Result.success(result);
    }

    private Map<String, Object> toRow(PayOrder o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("orderNo", o.getOrderNo());
        m.put("feeId", o.getFeeId());
        m.put("roomId", o.getRoomId());
        m.put("employeeId", o.getEmployeeId());
        m.put("amount", o.getAmount());
        m.put("month", o.getMonth());
        m.put("channel", o.getChannel());
        m.put("status", o.getStatus());
        m.put("subject", o.getSubject());
        m.put("paidTime", o.getPaidTime());
        m.put("expireTime", o.getExpireTime());
        m.put("createTime", o.getCreateTime());
        if (o.getRoomId() != null) {
            Room r = roomService.getById(o.getRoomId());
            if (r != null) m.put("roomNo", r.getRoomNo());
        }
        return m;
    }

    private Long parseLong(Object raw) {
        if (raw == null) return null;
        try {
            return Long.valueOf(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}