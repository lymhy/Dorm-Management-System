package com.example.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dorm.entity.Allocation;
import com.example.dorm.entity.Building;
import com.example.dorm.entity.Employee;
import com.example.dorm.entity.Fee;
import com.example.dorm.entity.FeePayment;
import com.example.dorm.entity.Room;
import com.example.dorm.entity.SysUser;
import com.example.dorm.entity.SystemConfig;
import com.example.dorm.service.AllocationService;
import com.example.dorm.service.BuildingService;
import com.example.dorm.service.EmployeeService;
import com.example.dorm.service.FeePaymentService;
import com.example.dorm.service.FeeService;
import com.example.dorm.service.RoomService;
import com.example.dorm.service.SysUserService;
import com.example.dorm.service.SystemConfigService;
import com.example.dorm.util.AuthUtil;
import com.example.dorm.util.NotificationHelper;
import com.example.dorm.util.Result;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** CRUD REST API for Fee. */
@RestController
@RequestMapping("/api/fee")
public class FeeController {

    @Resource
    private FeeService service;
    @Resource
    private FeePaymentService feePaymentService;
    @Resource
    private AllocationService allocationService;
    @Resource
    private EmployeeService employeeService;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private RoomService roomService;
    @Resource
    private BuildingService buildingService;
    @Resource
    private AuthUtil authUtil;
    @Resource
    private NotificationHelper notificationHelper;
    @Resource
    private SystemConfigService systemConfigService;

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

    /** 分页 + 多条件筛选（房间号关键字 / 指定房间 / 月份 / 缴纳状态） */
    @GetMapping("/page")
    public Result<Page<Fee>> page(@RequestParam(defaultValue = "1") long current,
                                  @RequestParam(defaultValue = "10") long size,
                                  @RequestParam(required = false) Long roomId,
                                  @RequestParam(required = false) String month,
                                  @RequestParam(required = false) Integer paid,
                                  @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Fee> q = new LambdaQueryWrapper<>();
        if (roomId != null) q.eq(Fee::getRoomId, roomId);
        if (month != null && !month.isBlank()) q.eq(Fee::getMonth, month.trim());
        if (paid != null) q.eq(Fee::getPaid, paid);
        if (keyword != null && !keyword.isBlank()) {
            List<Long> ids = roomService.list(new LambdaQueryWrapper<Room>()
                            .like(Room::getRoomNo, keyword.trim()))
                    .stream().map(Room::getId).toList();
            if (ids.isEmpty()) return Result.success(new Page<>(current, size));
            q.in(Fee::getRoomId, ids);
        }
        q.orderByDesc(Fee::getId);
        return Result.success(service.page(new Page<>(current, size), q));
    }

    /** 汇总统计（按当前筛选条件），供导出/仪表盘使用 */
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam(required = false) String month) {
        LambdaQueryWrapper<Fee> q = new LambdaQueryWrapper<>();
        if (month != null && !month.isBlank()) q.eq(Fee::getMonth, month.trim());
        List<Fee> all = service.list(q);
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal unpaidAmount = BigDecimal.ZERO;
        int paidCount = 0;
        for (Fee f : all) {
            BigDecimal t = f.getTotal() == null ? BigDecimal.ZERO : f.getTotal();
            totalAmount = totalAmount.add(t);
            if (f.getPaid() != null && f.getPaid() == 1) {
                paidCount++;
                paidAmount = paidAmount.add(t);
            } else {
                unpaidAmount = unpaidAmount.add(t);
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("count", all.size());
        res.put("paidCount", paidCount);
        res.put("unpaidCount", all.size() - paidCount);
        res.put("totalAmount", totalAmount);
        res.put("paidAmount", paidAmount);
        res.put("unpaidAmount", unpaidAmount);
        return Result.success(res);
    }

    /** 按楼栋 / 楼层汇总 + 月度趋势（统计报表） */
    @GetMapping("/report")
    public Result<Map<String, Object>> report(@RequestParam(required = false) String month) {
        LambdaQueryWrapper<Fee> q = new LambdaQueryWrapper<>();
        if (month != null && !month.isBlank()) q.eq(Fee::getMonth, month.trim());
        List<Fee> fees = service.list(q);

        Map<Long, Room> rooms = new LinkedHashMap<>();
        for (Room r : roomService.list()) rooms.put(r.getId(), r);
        Map<Long, Building> buildings = new LinkedHashMap<>();
        for (Building b : buildingService.list()) buildings.put(b.getId(), b);

        Map<Long, Map<String, Object>> byBuilding = new LinkedHashMap<>();
        Map<String, Map<String, Object>> byFloor = new LinkedHashMap<>();
        Map<String, Map<String, Object>> trend = new TreeMap<>();

        for (Fee f : fees) {
            BigDecimal t = f.getTotal() == null ? BigDecimal.ZERO : f.getTotal();
            boolean paid = f.getPaid() != null && f.getPaid() == 1;
            Room r = f.getRoomId() == null ? null : rooms.get(f.getRoomId());
            Long bid = r == null ? null : r.getBuildingId();
            String bname;
            if (bid != null && buildings.get(bid) != null) bname = buildings.get(bid).getName();
            else if (bid != null) bname = bid + "号楼";
            else bname = "未知楼栋";

            Map<String, Object> b = byBuilding.computeIfAbsent(bid == null ? -1L : bid, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("buildingId", bid);
                m.put("buildingName", bname);
                init(m);
                return m;
            });
            accumulate(b, t, paid);

            Integer floor = r == null ? null : r.getFloor();
            String fkey = bname + "#" + (floor == null ? "未知" : floor);
            Map<String, Object> fm = byFloor.computeIfAbsent(fkey, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("buildingName", bname);
                m.put("floor", floor);
                init(m);
                return m;
            });
            accumulate(fm, t, paid);

            String mm = f.getMonth() == null ? "未知" : f.getMonth();
            Map<String, Object> tm = trend.computeIfAbsent(mm, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("month", mm);
                init(m);
                return m;
            });
            accumulate(tm, t, paid);
        }

        List<Map<String, Object>> floors = new ArrayList<>(byFloor.values());
        floors.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.get("buildingName")))
                .thenComparingInt(m -> m.get("floor") == null ? 0 : ((Integer) m.get("floor"))));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("byBuilding", new ArrayList<>(byBuilding.values()));
        res.put("byFloor", floors);
        res.put("trend", new ArrayList<>(trend.values()));
        return Result.success(res);
    }

    private void init(Map<String, Object> m) {
        m.put("count", 0);
        m.put("totalAmount", BigDecimal.ZERO);
        m.put("paidAmount", BigDecimal.ZERO);
        m.put("unpaidAmount", BigDecimal.ZERO);
        m.put("unpaidCount", 0);
    }

    private void accumulate(Map<String, Object> m, BigDecimal t, boolean paid) {
        m.put("count", ((Integer) m.get("count")) + 1);
        m.put("totalAmount", ((BigDecimal) m.get("totalAmount")).add(t));
        if (paid) {
            m.put("paidAmount", ((BigDecimal) m.get("paidAmount")).add(t));
        } else {
            m.put("unpaidAmount", ((BigDecimal) m.get("unpaidAmount")).add(t));
            m.put("unpaidCount", ((Integer) m.get("unpaidCount")) + 1);
        }
    }

    /** 一键催缴：对未缴账单所在房间的在住员工发送催缴通知 */
    @PostMapping("/urge")
    public Result<Map<String, Object>> urge(@RequestBody Map<String, Object> body) {
        List<Long> ids = toIds(body.get("ids"));
        String month = body.get("month") == null ? null : String.valueOf(body.get("month"));
        LambdaQueryWrapper<Fee> q = new LambdaQueryWrapper<>();
        if (!ids.isEmpty()) q.in(Fee::getId, ids);
        q.eq(Fee::getPaid, 0);
        if (month != null && !month.isBlank()) q.eq(Fee::getMonth, month.trim());
        List<Fee> fees = service.list(q);

        int notified = 0;
        for (Fee f : fees) {
            if (f.getRoomId() == null) continue;
            List<Allocation> allocs = allocationService.list(new LambdaQueryWrapper<Allocation>()
                    .eq(Allocation::getRoomId, f.getRoomId()).eq(Allocation::getStatus, 1));
            for (Allocation a : allocs) {
                notificationHelper.send(a.getEmployeeId(), "fee_urge", "水电费催缴通知",
                        "您所在房间 " + (f.getMonth() == null ? "本期" : f.getMonth())
                                + " 水电费 \u00a5" + f.getTotal() + " 尚未缴纳，请及时办理缴费。", f.getId());
                notified++;
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("bills", fees.size());
        res.put("notified", notified);
        return Result.success(res);
    }

    // 当前员工的账单（按入住房间过滤，必须在 /{id} 之前）
    @GetMapping("/my")
    public Result<Page<Fee>> my(@RequestParam(defaultValue = "1") long current,
                                @RequestParam(defaultValue = "10") long size,
                                @RequestHeader(value = "Authorization", required = false) String auth) {
        Long empId = getCurrentEmpId(auth);
        if (empId == null) return Result.fail("未登录");
        Allocation alloc = allocationService.getOne(new LambdaQueryWrapper<Allocation>()
            .eq(Allocation::getEmployeeId, empId).eq(Allocation::getStatus, 1));
        if (alloc == null) return Result.success(new Page<Fee>());
        Page<Fee> p = new Page<>(current, size);
        LambdaQueryWrapper<Fee> q = new LambdaQueryWrapper<Fee>()
            .eq(Fee::getRoomId, alloc.getRoomId()).orderByDesc(Fee::getId);
        service.page(p, q);
        return Result.success(p);
    }

    @GetMapping("/list")
    public Result<List<Fee>> list() {
        return Result.success(service.list(new LambdaQueryWrapper<Fee>().orderByDesc(Fee::getId)));
    }

    @GetMapping("/{id}")
    public Result<Fee> get(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    public Result<Boolean> add(@RequestBody Fee entity,
                               @RequestHeader(value = "Authorization", required = false) String auth) {
        fillTotal(entity);
        boolean ok = service.save(entity);
        if (ok) {
            checkFeeWarning(entity);
            if (entity.getPaid() != null && entity.getPaid() == 1) recordPayment(entity, currentUsername(auth));
        }
        return Result.success(ok);
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Fee entity,
                                  @RequestHeader(value = "Authorization", required = false) String auth) {
        fillTotal(entity);
        Fee old = entity.getId() == null ? null : service.getById(entity.getId());
        boolean ok = service.updateById(entity);
        if (ok) {
            checkFeeWarning(entity);
            Fee after = entity.getId() == null ? null : service.getById(entity.getId());
            boolean wasPaid = old != null && old.getPaid() != null && old.getPaid() == 1;
            if (after != null && after.getPaid() != null && after.getPaid() == 1 && !wasPaid) {
                recordPayment(after, currentUsername(auth));
            }
        }
        return Result.success(ok);
    }

    /** 单条标记已缴 / 未缴 */
    @PutMapping("/{id}/paid")
    public Result<Boolean> markPaid(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                    @RequestHeader(value = "Authorization", required = false) String auth) {
        Fee f = service.getById(id);
        if (f == null) return Result.error("账单不存在");
        int paid = body.get("paid") == null ? 1 : Integer.parseInt(String.valueOf(body.get("paid")));
        Fee upd = new Fee();
        upd.setId(id);
        upd.setPaid(paid);
        boolean ok = service.updateById(upd);
        if (ok && paid == 1 && (f.getPaid() == null || f.getPaid() != 1)) recordPayment(f, currentUsername(auth));
        return Result.success(ok);
    }

    /** 批量标记已缴 / 未缴 */
    @PutMapping("/batch-paid")
    public Result<Integer> batchPaid(@RequestBody Map<String, Object> body,
                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        List<Long> ids = toIds(body.get("ids"));
        if (ids.isEmpty()) return Result.error("请选择账单");
        int paid = body.get("paid") == null ? 1 : Integer.parseInt(String.valueOf(body.get("paid")));
        List<Fee> fees = service.listByIds(ids);
        Fee upd = new Fee();
        upd.setPaid(paid);
        service.update(upd, new LambdaQueryWrapper<Fee>().in(Fee::getId, ids));
        if (paid == 1) {
            String op = currentUsername(auth);
            for (Fee f : fees) {
                if (f.getPaid() == null || f.getPaid() != 1) recordPayment(f, op);
            }
        }
        return Result.success(ids.size());
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<Integer> batchDelete(@RequestBody Map<String, Object> body) {
        List<Long> ids = toIds(body.get("ids"));
        if (ids.isEmpty()) return Result.error("请选择账单");
        service.removeByIds(ids);
        return Result.success(ids.size());
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(service.removeById(id));
    }

    private List<Long> toIds(Object raw) {
        List<Long> ids = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                String s = String.valueOf(o).trim();
                if (!s.isEmpty()) ids.add(Long.valueOf(s));
            }
        }
        return ids;
    }

    /** 合计为空时按 水费+电费 自动补齐 */
    private void fillTotal(Fee fee) {
        if (fee == null) return;
        if (fee.getTotal() == null) {
            BigDecimal w = fee.getWaterFee() == null ? BigDecimal.ZERO : fee.getWaterFee();
            BigDecimal e = fee.getElecFee() == null ? BigDecimal.ZERO : fee.getElecFee();
            fee.setTotal(w.add(e));
        }
    }

    /** 记录一条缴费流水（关联房间在住员工） */
    private void recordPayment(Fee fee, String operator) {
        if (fee == null) return;
        FeePayment p = new FeePayment();
        p.setFeeId(fee.getId());
        p.setRoomId(fee.getRoomId());
        p.setAmount(fee.getTotal());
        p.setMonth(fee.getMonth());
        p.setPayTime(LocalDateTime.now());
        p.setPayMethod("线下缴纳");
        p.setOperator(operator == null ? "系统" : operator);
        p.setRemark("账单缴费");
        if (fee.getRoomId() != null) {
            List<Allocation> allocs = allocationService.list(new LambdaQueryWrapper<Allocation>()
                    .eq(Allocation::getRoomId, fee.getRoomId()).eq(Allocation::getStatus, 1));
            if (!allocs.isEmpty()) p.setEmployeeId(allocs.get(0).getEmployeeId());
        }
        feePaymentService.save(p);
    }

    /** 水电费超限预警：合计超过阈值时通知该房间所有在住员工。 */
    private void checkFeeWarning(Fee fee) {
        if (fee == null || fee.getRoomId() == null || fee.getTotal() == null) return;
        BigDecimal threshold = getThreshold();
        if (fee.getTotal().compareTo(threshold) <= 0) return;
        List<Allocation> allocs = allocationService.list(new LambdaQueryWrapper<Allocation>()
                .eq(Allocation::getRoomId, fee.getRoomId()).eq(Allocation::getStatus, 1));
        String month = fee.getMonth() == null ? "本期" : fee.getMonth();
        for (Allocation a : allocs) {
            notificationHelper.send(a.getEmployeeId(), "fee_warning", "水电费超限提醒",
                    "您所在房间 " + month + " 水电费合计 \u00a5" + fee.getTotal()
                            + "，已超过预警阈值 \u00a5" + threshold + "，请及时关注缴纳。", fee.getId());
        }
    }

    private BigDecimal getThreshold() {
        try {
            SystemConfig c = systemConfigService.getOne(new LambdaQueryWrapper<SystemConfig>()
                    .eq(SystemConfig::getCfgKey, "utility_threshold"));
            if (c != null && c.getCfgValue() != null && !c.getCfgValue().trim().isEmpty()) {
                return new BigDecimal(c.getCfgValue().trim());
            }
        } catch (Exception ignore) {
        }
        return new BigDecimal("200");
    }
}
