package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.Building;
import com.example.dorm.entity.Employee;
import com.example.dorm.entity.FeePayment;
import com.example.dorm.entity.Room;
import com.example.dorm.service.BuildingService;
import com.example.dorm.service.EmployeeService;
import com.example.dorm.service.FeePaymentService;
import com.example.dorm.service.RoomService;
import com.example.dorm.util.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 缴费流水（收据明细）。 */
@RestController
@RequestMapping("/api/fee-payment")
public class FeePaymentController {

    @Resource
    private FeePaymentService service;
    @Resource
    private RoomService roomService;
    @Resource
    private BuildingService buildingService;
    @Resource
    private EmployeeService employeeService;

    @GetMapping("/page")
    public Result<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long roomId,
                                                  @RequestParam(required = false) String month,
                                                  @RequestParam(required = false) String payMethod) {
        LambdaQueryWrapper<FeePayment> q = new LambdaQueryWrapper<>();
        if (roomId != null) q.eq(FeePayment::getRoomId, roomId);
        if (month != null && !month.isBlank()) q.eq(FeePayment::getMonth, month.trim());
        if (payMethod != null && !payMethod.isBlank()) q.eq(FeePayment::getPayMethod, payMethod.trim());
        if (keyword != null && !keyword.isBlank()) {
            List<Long> ids = roomService.list(new LambdaQueryWrapper<Room>()
                            .like(Room::getRoomNo, keyword.trim()))
                    .stream().map(Room::getId).toList();
            if (ids.isEmpty()) return Result.success(new Page<>(current, size));
            q.in(FeePayment::getRoomId, ids);
        }
        q.orderByDesc(FeePayment::getPayTime).orderByDesc(FeePayment::getId);
        Page<FeePayment> p = service.page(new Page<>(current, size), q);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeePayment pay : p.getRecords()) rows.add(toRow(pay));
        Page<Map<String, Object>> result = new Page<>(current, size, p.getTotal());
        result.setRecords(rows);
        return Result.success(result);
    }

    /** 某账单对应的缴费流水（用于生成收据） */
    @GetMapping("/by-fee/{feeId}")
    public Result<Map<String, Object>> byFee(@PathVariable Long feeId) {
        List<FeePayment> list = service.list(new LambdaQueryWrapper<FeePayment>()
                .eq(FeePayment::getFeeId, feeId).orderByDesc(FeePayment::getId));
        if (list.isEmpty()) return Result.success(null);
        return Result.success(toRow(list.get(0)));
    }

    /** 汇总：缴费笔数 + 缴费总额（可按月份） */
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam(required = false) String month) {
        LambdaQueryWrapper<FeePayment> q = new LambdaQueryWrapper<>();
        if (month != null && !month.isBlank()) q.eq(FeePayment::getMonth, month.trim());
        List<FeePayment> all = service.list(q);
        BigDecimal amount = BigDecimal.ZERO;
        for (FeePayment p : all) if (p.getAmount() != null) amount = amount.add(p.getAmount());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("count", all.size());
        res.put("amount", amount);
        return Result.success(res);
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeePayment p : service.list(new LambdaQueryWrapper<FeePayment>()
                .orderByDesc(FeePayment::getPayTime).orderByDesc(FeePayment::getId))) {
            rows.add(toRow(p));
        }
        return Result.success(rows);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        FeePayment p = service.getById(id);
        return Result.success(p == null ? null : toRow(p));
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody FeePayment entity) {
        if (entity.getPayTime() == null) entity.setPayTime(java.time.LocalDateTime.now());
        if (entity.getPayMethod() == null || entity.getPayMethod().isBlank()) entity.setPayMethod("线下缴纳");
        return Result.success(service.save(entity));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(service.removeById(id));
    }

    private Map<String, Object> toRow(FeePayment pay) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", pay.getId());
        m.put("feeId", pay.getFeeId());
        m.put("roomId", pay.getRoomId());
        m.put("employeeId", pay.getEmployeeId());
        m.put("amount", pay.getAmount());
        m.put("month", pay.getMonth());
        m.put("payTime", pay.getPayTime());
        m.put("payMethod", pay.getPayMethod());
        m.put("operator", pay.getOperator());
        m.put("remark", pay.getRemark());
        if (pay.getRoomId() != null) {
            Room r = roomService.getById(pay.getRoomId());
            if (r != null) {
                m.put("roomNo", r.getRoomNo());
                m.put("floor", r.getFloor());
                if (r.getBuildingId() != null) {
                    Building b = buildingService.getById(r.getBuildingId());
                    m.put("buildingId", r.getBuildingId());
                    m.put("buildingName", b == null ? null : b.getName());
                }
            }
        }
        if (pay.getEmployeeId() != null) {
            Employee e = employeeService.getById(pay.getEmployeeId());
            m.put("employeeName", e == null ? null : e.getName());
        }
        return m;
    }
}
